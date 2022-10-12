ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.10"

val zioVersion = "2.+"
val tapirVersion = "1.1.2"

lazy val root = (project in file("."))
  .settings(
    name := "snowplow-task",
    libraryDependencies ++= Seq(
      "io.circe" %% "circe-json-schema" % "0.2.0" exclude("com.github.everit-org.json-schema", "org.everit.json.schema"),
      "com.github.erosb" % "everit-json-schema" % "1.14.1",
      "com.softwaremill.sttp.client3" %% "zio" % "3.8.2",
      "com.softwaremill.sttp.client3" %% "circe" % "3.8.2",
      "com.softwaremill.sttp.tapir" %% "tapir-zio" % tapirVersion,
      "com.softwaremill.sttp.tapir" %% "tapir-json-circe" % tapirVersion,
      "com.softwaremill.sttp.tapir" %% "tapir-sttp-client" % tapirVersion,
      "com.softwaremill.sttp.tapir" %% "tapir-zio-http-server" % tapirVersion,
      "com.softwaremill.sttp.tapir" %% "tapir-sttp-stub-server" % tapirVersion,
      "com.softwaremill.sttp.tapir" %% "tapir-swagger-ui-bundle" % tapirVersion,
      "com.softwaremill.sttp.tapir" %% "tapir-openapi-circe-yaml" % "1.0.0-M9",

      "dev.zio" %% "zio-test" % zioVersion % Test,
      "dev.zio" %% "zio-test-sbt" % zioVersion % Test,
      "dev.zio" %% "zio-test-magnolia" % zioVersion % Test,
    ),
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")
  )
