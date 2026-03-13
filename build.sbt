val scala3Version = "3.3.7"

lazy val root = project
  .in(file("."))
  .settings(
    name := "Tasty 2 MorphIR",
    version := "0.1.0-SNAPSHOT",

    scalaVersion := scala3Version,

    libraryDependencies ++= Seq(
      "org.scala-lang" %% "scala3-tasty-inspector" % scalaVersion.value,
      "com.typesafe.scala-logging" %% "scala-logging" % "3.9.5",
      "org.morphir" %% "morphir-ir" % "0.21.0",
      // Runtime
      "org.slf4j" % "slf4j-simple" % "2.0.17" % Runtime,
      // Test
      "org.scalameta" %% "munit" % "1.2.4" % Test
    )
  )
