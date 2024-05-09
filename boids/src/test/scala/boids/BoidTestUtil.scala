package boids

import munit.Assertions.*
import cs214.*
import scala.annotation.targetName

val DELTA = 0.005f

def assertEqualsVector2(obtained: Vector2, expected: Vector2)(using munit.Location) =
  assertEqualsFloat(obtained.x, expected.x, DELTA)
  assertEqualsFloat(obtained.y, expected.y, DELTA)

def assertEqualsBoid(obtained: Boid, expected: Boid)(using munit.Location) =
  assert(
    equalsBoid(obtained, expected),
    f"expected boid $expected, obtained $obtained"
  )

def equalsFloat(f1: Float, f2: Float): Boolean =
  val exactlyTheSame = java.lang.Float.compare(f1, f2) == 0
  val almostTheSame = Math.abs(f1 - f2) <= DELTA
  exactlyTheSame || almostTheSame

def equalsBoid(b1: Boid, b2: Boid): Boolean =
  equalsFloat(b1.position.x, b2.position.x) &&
    equalsFloat(b1.position.y, b2.position.y) &&
    equalsFloat(b1.velocity.x, b2.velocity.x) &&
    equalsFloat(b1.velocity.y, b2.velocity.y)

def boidSequenceToSeqBoid(bs: BoidSequence): Seq[Boid] =
  Seq.unfold(bs)(s => Option.unless(s.isEmpty)((s.head, s.tail)))

def seqBoidToBoidSequence(sb: Boid*): BoidSequence =
  seqBoidToBoidSequence(sb)

@targetName("seqBoidToBoidSequenceStar")
def seqBoidToBoidSequence(sb: Seq[Boid]) =
  sb.foldRight[BoidSequence](BoidNil())((b, bs) => BoidCons(b, bs))

def assertEqualsSeqBoid(obtained: Seq[Boid], expected: Seq[Boid], physics: Physics)(using munit.Location) =
  for boid <- obtained do
    assert(
      physics.minimumSpeed - DELTA <= boid.velocity.norm
        && physics.maximumSpeed + DELTA >= boid.velocity.norm,
      f"boid sequence contains a boid with velocity out of bounds: ${boid.velocity}"
    )
    assert(
      boid.position.x.isFinite && boid.position.y.isFinite
        && boid.velocity.x.isFinite && boid.velocity.y.isFinite,
      f"boid sequence contains a boid with nonfinite parameters: $boid"
    )
  assert(
    obtained.forall(bo => expected.exists(be => equalsBoid(bo, be))) &&
      expected.forall(be => obtained.exists(bo => equalsBoid(bo, be))) &&
      obtained.length == expected.length,
    f"obtained boid sequence $obtained does not match expected $expected"
  )

def assertEqualsBoidSequence(obtained: BoidSequence, expected: BoidSequence, physics: Physics)(using munit.Location) =
  assertEqualsSeqBoid(boidSequenceToSeqBoid(obtained), boidSequenceToSeqBoid(expected), physics)

def readTestCase(name: String)(using munit.Location) =
  val relPath = os.rel / "src" / "test" / "json" / (name + ".json")
  val testCaseRaw =
    try
      os.read(os.pwd / relPath)
    catch
      case _: java.nio.file.NoSuchFileException =>
        os.read(os.pwd / "labs" / "boids" / relPath)
  ujson.read(testCaseRaw)

def jsonToBoidSequence(boids: ujson.Value): BoidSequence =
  seqBoidToBoidSequence(
    boids.arr.map(b => Boid(Vector2(b("x").num, b("y").num), Vector2(b("vx").num, b("vy").num))).toSeq
  )

def jsonToPhysics(physics: ujson.Value): Physics =
  Physics(
    minimumSpeed = physics("minimumSpeed").num.toFloat,
    maximumSpeed = physics("maximumSpeed").num.toFloat,
    perceptionRadius = physics("perceptionRadius").num.toFloat,
    avoidanceRadius = physics("avoidanceRadius").num.toFloat,
    avoidanceWeight = physics("avoidanceWeight").num.toFloat,
    cohesionWeight = physics("cohesionWeight").num.toFloat,
    alignmentWeight = physics("alignmentWeight").num.toFloat,
    containmentWeight = physics("containmentWeight").num.toFloat
  )

def runTestCase(name: String)(using munit.Location) =
  val testCase = readTestCase(name)
  val initBoids = jsonToBoidSequence(testCase("initialBoids"))
  val physics = jsonToPhysics(testCase("physics"))
  testCase("reference")
    .arr
    .map(jsonToBoidSequence)
    .toSeq
    .drop(1)
    .foldLeft(initBoids) { (world, expected) =>
      val next = tickWorld(world, physics)
      assertEqualsBoidSequence(next, expected, physics)
      next
    }

def seqBoidToJson(boids: Seq[Boid]) =
  boids.map(b =>
    ujson.Obj(
      "x" -> b.position.x,
      "y" -> b.position.y,
      "vx" -> b.velocity.x,
      "vy" -> b.velocity.y
    )
  )

implicit val physicsWriter: upickle.default.ReadWriter[Physics] = upickle.default.macroRW

def generateTestCase(name: String, initialBoids: Seq[Boid], physics: Physics, steps: Int = 50) =
  val path = os.pwd / "labs" / "boids" / "src" / "test" / "json" / (name + ".json")
  val reference = Seq.iterate(seqBoidToBoidSequence(initialBoids), steps)(boids => tickWorld(boids, physics))
  val testCase = ujson.Obj(
    "initialBoids" -> seqBoidToJson(initialBoids),
    "physics" -> upickle.default.writeJs(physics),
    "reference" -> reference.map(boidSequenceToSeqBoid).map(seqBoidToJson)
  )
  os.write.over(path, ujson.write(testCase, 4, true))

def generateTestCases() =
  val noForces = Physics(
    minimumSpeed = 2f,
    maximumSpeed = 4f,
    perceptionRadius = 80f,
    avoidanceRadius = 22f,
    // all weights are zero
    avoidanceWeight = 0f,
    cohesionWeight = 0f,
    alignmentWeight = 0f,
    containmentWeight = 0f
  )
  generateTestCase(
    "00_singleBoidNoForces",
    Seq(Boid(Vector2(501, 305), Vector2(2, 3))),
    noForces
  )
  generateTestCase(
    "01_threeBoidsNoForces",
    Seq(
      Boid(Vector2(501, 305), Vector2(2, 3)),
      Boid(Vector2(510, 305), Vector2(-2, 1)),
      Boid(Vector2(600, 399), Vector2(-2, -2))
    ),
    noForces
  )

  val onlyAvoidance = Physics(
    // set min and max speed so that they won't affect the tests
    minimumSpeed = 0f,
    maximumSpeed = 100f,
    perceptionRadius = 80f,
    // only the avoidance parameters are relevant here
    avoidanceRadius = 22f,
    avoidanceWeight = 10f,
    cohesionWeight = 0f,
    alignmentWeight = 0f,
    containmentWeight = 0f
  )
  generateTestCase(
    "10_singleBoidAvoidance",
    Seq(Boid(Vector2(501, 305), Vector2(2, 3))),
    onlyAvoidance
  )
  generateTestCase(
    "11_twoBoidsAvoidanceX",
    Seq(Boid(Vector2(534, 305), Vector2(-2, 0)), Boid(Vector2(510, 305), Vector2(2, 0))),
    onlyAvoidance
  )
  generateTestCase(
    "12_twoBoidsAvoidanceXY",
    Seq(Boid(Vector2(534, 305), Vector2(-2, 1)), Boid(Vector2(510, 305), Vector2(2, 1))),
    onlyAvoidance
  )
  generateTestCase(
    "13_twoBoidsAvoidanceFar",
    Seq(Boid(Vector2(534, 305), Vector2(-2, 1)), Boid(Vector2(510, 205), Vector2(2, -1))),
    onlyAvoidance
  )
  generateTestCase(
    "14_mixedAvoidance",
    Seq(
      Boid(Vector2(534, 305), Vector2(-2, 1)),
      Boid(Vector2(510, 205), Vector2(2, 0)),
      Boid(Vector2(534, 315), Vector2(-2, 1)),
      Boid(Vector2(510, 190), Vector2(2, 0))
    ),
    onlyAvoidance
  )
  generateTestCase(
    "15_avoidanceSamePosition",
    Seq(
      Boid(Vector2(534, 305), Vector2(-2, 1)),
      Boid(Vector2(510, 205), Vector2(2, 0)),
      Boid(Vector2(534, 315), Vector2(-2, 1)),
      Boid(Vector2(510, 190), Vector2(2, 0)),
      Boid(Vector2(510, 190), Vector2(-2, 0))
    ),
    onlyAvoidance
  )

  val onlyCohesion = Physics(
    // set min and max speed so that they won't affect the tests
    minimumSpeed = 0f,
    maximumSpeed = 6f,
    // avoidance and cohesion parameters are relevant
    perceptionRadius = 80f,
    avoidanceRadius = 22f,
    avoidanceWeight = 0f,
    cohesionWeight = 0.1f,
    // these two are still zero
    alignmentWeight = 0f,
    containmentWeight = 0f
  )
  generateTestCase(
    "20_twoBoidsRestCohesion",
    Seq(Boid(Vector2(540, 350), Vector2(0, 0)), Boid(Vector2(510, 310), Vector2(0, 0))),
    onlyCohesion
  )
  generateTestCase(
    "21_twoBoidsCohesionDance",
    Seq(Boid(Vector2(540, 350), Vector2(0, 0)), Boid(Vector2(510, 310), Vector2(0, 1))),
    onlyCohesion
  )
  generateTestCase(
    "22_twoBoidsCohesionFar",
    Seq(Boid(Vector2(540, 150), Vector2(0, -1)), Boid(Vector2(510, 310), Vector2(0, 1))),
    onlyCohesion
  )
  // 23_chaoticCohesion built by hand

  val avoidanceAndCohesion = Physics(
    // set min and max speed so that they won't affect the tests
    minimumSpeed = 0f,
    maximumSpeed = 100f,
    // avoidance and cohesion parameters are relevant
    perceptionRadius = 80f,
    avoidanceRadius = 22f,
    avoidanceWeight = 10f,
    cohesionWeight = 0.1f,
    // these two are still zero
    alignmentWeight = 0f,
    containmentWeight = 0f
  )
  generateTestCase(
    "30_avoidanceCohesion",
    Seq(Boid(Vector2(540, 350), Vector2(0, 0)), Boid(Vector2(510, 310), Vector2(0, 0))),
    avoidanceAndCohesion,
    steps = 4
  )
  generateTestCase(
    "31_avoidanceCohesionLonger",
    Seq(Boid(Vector2(540, 350), Vector2(0, 0)), Boid(Vector2(510, 310), Vector2(0, 0))),
    avoidanceAndCohesion
  )

  val threeBodyProblem = Physics(
    minimumSpeed = 0f,
    maximumSpeed = 5f,
    perceptionRadius = 200f,
    avoidanceRadius = 35f,
    avoidanceWeight = 8f,
    cohesionWeight = 0.001f,
    alignmentWeight = 0f,
    containmentWeight = 0f
  )
  generateTestCase(
    "32_threeBodyProblem",
    Seq(
      Boid(Vector2(534, 305), Vector2(-2, 1)),
      Boid(Vector2(510, 205), Vector2(2, 1)),
      Boid(Vector2(410, 255), Vector2(2, -1))
    ),
    threeBodyProblem,
    steps = 300
  )

  val onlyAlignment = Physics(
    minimumSpeed = 0f,
    maximumSpeed = 5f,
    perceptionRadius = 200f,
    avoidanceRadius = 35f,
    avoidanceWeight = 0f,
    cohesionWeight = 0f,
    alignmentWeight = 0.04f,
    containmentWeight = 0f
  )
  generateTestCase(
    "40_onlyAlignment",
    Seq(
      Boid(Vector2(534, 305), Vector2(-1, 1)),
      Boid(Vector2(510, 205), Vector2(2, 1)),
      Boid(Vector2(410, 255), Vector2(2, -1))
    ),
    onlyAlignment
  )
  generateTestCase(
    "41_alignmentFar",
    Seq(
      Boid(Vector2(612, 305), Vector2(2, 1)),
      Boid(Vector2(411, 205), Vector2(1, 2)),
      Boid(Vector2(210, 255), Vector2(0, 1.5))
    ),
    onlyAlignment
  )

  val onlyContainment = Physics(
    minimumSpeed = 0f,
    maximumSpeed = 5f,
    perceptionRadius = 200f,
    avoidanceRadius = 35f,
    avoidanceWeight = 0f,
    cohesionWeight = 0f,
    alignmentWeight = 0f,
    containmentWeight = 0.5f
  )
  generateTestCase(
    "50_containmentTop",
    Seq(Boid(Vector2(500, 20), Vector2(1, -1))),
    onlyContainment
  )
  generateTestCase(
    "51_containmentBottom",
    Seq(Boid(Vector2(500, 680), Vector2(1, 1))),
    onlyContainment
  )
  generateTestCase(
    "52_containmentLeft",
    Seq(Boid(Vector2(20, 300), Vector2(-1, -1))),
    onlyContainment
  )
  generateTestCase(
    "53_containmentRight",
    Seq(Boid(Vector2(980, 300), Vector2(1, -1))),
    onlyContainment
  )
  generateTestCase(
    "54_containmentCumulative",
    Seq(Boid(Vector2(30, 20), Vector2(-1, -1))),
    onlyContainment
  )

  val allTogether = Physics(
    minimumSpeed = 2f,
    maximumSpeed = 5f,
    perceptionRadius = 80f,
    avoidanceRadius = 15f,
    avoidanceWeight = 1f,
    cohesionWeight = 0.001f,
    alignmentWeight = 0.027f,
    containmentWeight = 0.5f
  )
  generateTestCase(
    "60_allTogether",
    Seq(
      Boid(Vector2(534, 305), Vector2(-2, 1)),
      Boid(Vector2(510, 205), Vector2(2, 1)),
      Boid(Vector2(410, 255), Vector2(2, -1)),
      Boid(Vector2(574, 75), Vector2(2, 1)),
      Boid(Vector2(120, 125), Vector2(2, -1)),
      Boid(Vector2(489, 275), Vector2(2, 1)),
      Boid(Vector2(574, 85), Vector2(-2, -1)),
      Boid(Vector2(120, 135), Vector2(2, 1)),
      Boid(Vector2(472, 275), Vector2(2, -1))
    ),
    allTogether,
    steps = 200
  )
