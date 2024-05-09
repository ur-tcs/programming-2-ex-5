name := "boids"
scalaVersion := "3.3.1"
libraryDependencies += "org.scala-lang" %% "toolkit-test" % "0.1.7" % Test
libraryDependencies += "org.scala-lang" %% "toolkit" % "0.1.7"
scalacOptions ++= Seq("-source:future", "-deprecation", "-language:fewerBraces", "-Xfatal-warnings")
run / fork := true
Global / cancelable := true

// Library dependencies used for http
val http4sVersion = "1.0.0-M39"
val circeVersion = "0.14.1"
lazy val httpLibraryDependencies = Seq(
  "org.http4s"    %% "http4s-ember-server" % http4sVersion,
  "org.http4s"    %% "http4s-dsl"          % http4sVersion,
  "org.http4s"    %% "http4s-circe"        % http4sVersion,
  "io.circe"      %% "circe-generic"       % circeVersion,
  "org.slf4j" % "slf4j-nop" % "2.0.0"
)
libraryDependencies ++= httpLibraryDependencies
