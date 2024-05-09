package ui

import cats.*
import cats.effect.*
import io.circe.generic.auto.*
import io.circe.syntax.*
import io.circe.{HCursor, Encoder, Decoder, ACursor, Json}
import io.circe.Decoder.Result
import org.http4s.*
import org.http4s.dsl.*
import org.http4s.headers.*
import org.http4s.implicits.*
import org.http4s.circe.*
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.middleware.{ErrorAction, ErrorHandling}
import com.comcast.ip4s.*
import boids.*
import scala.annotation.tailrec
import org.http4s.dsl.impl.QueryParamDecoderMatcher
import cats.data.OptionT
import p2.{Vector2, BoidSequence, BoidCons, BoidNil}

case class BoidData(x: Float, y: Float, vx: Float, vy: Float)

object Server extends IOApp:

  val dsl = Http4sDsl[IO]
  import dsl.*

  val DEFAULT_BOIDS_COUNT = 500
  val WEB_SRC_PATH = "src/main/www/boids."

  val default = Physics(
    minimumSpeed = 1f,
    maximumSpeed = 8f,
    perceptionRadius = 80f,
    avoidanceRadius = 15f,
    avoidanceWeight = 1f,
    cohesionWeight = 0.001f,
    alignmentWeight = 0.027f,
    containmentWeight = 0.5f
  )
  var world: World = World(default)
  var boids: BoidSequence = p2.BoidNil()

  def initializeRandom(boidsCount: Int) = synchronized {
    world = World(default)
    boids = World.createRandom(boidsCount, default)
  }

  def initializeWith(physics: Physics, initialBoids: BoidSequence) = synchronized {
    world = World(physics)
    boids = initialBoids
  }

  def update() = synchronized {
    boids = world.tick(boids)
  }

  def readStatic(ext: String) =
    val source = scala.io.Source.fromFile(WEB_SRC_PATH + ext)
    val contents =
      try source.mkString
      finally source.close()
    contents

  def flockRoutes: HttpRoutes[IO] =

    object BoidsCountQueryParamDecoderMatcher
        extends OptionalValidatingQueryParamDecoderMatcher[Int]("boidsCount")
    object TestCaseNameQueryParamDecoderMatcher
        extends QueryParamDecoderMatcher[String]("name")

    HttpRoutes.of[IO] {
      case GET -> Root =>
        Ok(readStatic("html"), `Content-Type`.apply(MediaType("text", "html")))
      case GET -> Root / "boids.html" =>
        Ok(readStatic("html"), `Content-Type`.apply(MediaType("text", "html")))
      case GET -> Root / "boids.css" =>
        Ok(readStatic("css"), `Content-Type`.apply(MediaType("text", "css")))
      case GET -> Root / "boids.js" =>
        Ok(readStatic("js"), `Content-Type`.apply(MediaType("text", "javascript")))
      case GET -> Root / "initializeRandom" :?
          BoidsCountQueryParamDecoderMatcher(maybeBoidsCount) =>
        val boidsCount = maybeBoidsCount match
          case None => DEFAULT_BOIDS_COUNT
          case Some(validatedBoidsCount) => validatedBoidsCount.fold(
              _ => DEFAULT_BOIDS_COUNT,
              boidsCount => boidsCount
            )
        initializeRandom(boidsCount)
        Ok(Config(world.physics, boids).asJson)
      case GET -> Root / "get" =>
        update()
        Ok(boids.asJson)
      case GET -> Root / "testCase" :? TestCaseNameQueryParamDecoderMatcher(testCase) =>
        Ok(
          os.read(os.pwd / "src" / "test" / "json" / (testCase + ".json")),
          `Content-Type`(MediaType("text", "json"))
        )
      case GET -> Root / "testCases" =>
        Ok(
          os.list(os.pwd / "src" / "test" / "json").map(_.baseName).asJson,
          `Content-Type`(MediaType("text", "json"))
        )
      case req @ POST -> Root / "initializeWith" =>
        req.as[Config].flatMap(c =>
          initializeWith(c.physics, c.initialBoids)
          Ok(c.asJson)
        )
    }

  val withErrorLogging = ErrorHandling.Recover.total(
    ErrorAction.log(
      flockRoutes.orNotFound,
      messageFailureLogAction =
        // ???,
        (t, _) =>
          IO.println(t) >> IO.raiseError(t),
      serviceErrorLogAction =
        // ???,
        (t, _) =>
          IO.println(t) >> IO.raiseError(t),
    )
  )

  override def run(args: List[String]): IO[ExitCode] =
    val server = EmberServerBuilder
      .default[IO]
      .withHost(ipv4"0.0.0.0")
      .withPort(port"8080")
      .withHttpApp(withErrorLogging)
      .withShutdownTimeout(concurrent.duration.Duration(200, "ms"))
      .build
      .use(_ => IO.never)
      .as(ExitCode.Success)
    server

case class Config(physics: Physics, initialBoids: BoidSequence)

implicit val decoder: Decoder[Config] = new Decoder[Config]:
  override def apply(c: HCursor): Result[Config] =
    c.downField("physics").as[Physics].flatMap { physics =>
      val bdrs =
        for
          boids <- c.downField("initialBoids").values.toList
          boid <- boids
        yield boid.as[BoidData]
      bdrs.foldLeft[Result[BoidSequence]](Right(BoidNil())) { (seqr, bdr) =>
        for
          seq <- seqr
          bd <- bdr
        yield BoidCons(Boid(Vector2(bd.x, bd.y), Vector2(bd.vx, bd.vy)), seq)
      }.map(boids => Config(physics, boids))
    }
implicit val configDecoder: EntityDecoder[IO, Config] = jsonOf[IO, Config]

implicit val encoder: Encoder[BoidSequence] = new Encoder[BoidSequence]:
  @tailrec
  def toBoidDataList(boids: BoidSequence, acc: List[BoidData] = Nil): List[BoidData] =
    boids match
      case BoidNil() => acc
      case BoidCons(b, tl) =>
        val data = BoidData(b.position.x, b.position.y, b.velocity.x, b.velocity.y)
        toBoidDataList(tl, data :: acc)

  override def apply(boids: BoidSequence): Json =
    toBoidDataList(boids).asJson

implicit val boidsEncoder: EntityEncoder[IO, BoidSequence] = jsonEncoderOf[BoidSequence]
