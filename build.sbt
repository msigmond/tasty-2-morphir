val scala3Version = "3.3.7"

// Sub-project that compiles the Scala algorithm fixtures to .tasty files.
// Kept as a minimal project with no extra dependencies so the .tasty files
// are produced by the same Scala version (3.3.7) as the TASTy inspector.
lazy val scalaAlgorithmFixtures = project
  .in(file("test-fixtures/scala"))
  .settings(
    name := "scala-algorithm-fixtures",
    scalaVersion := scala3Version,
    target := file(".") / "target" / "scala-algorithm-fixtures",
    cleanFiles += baseDirectory.value / "target",
    publish / skip := true
  )

// Runs `morphir-elm make -f` in each per-case Elm fixture directory under test-fixtures/elm/.
lazy val generateElmIR = taskKey[Seq[File]]("Generate Morphir IR from Elm test fixtures")

lazy val root = project
  .in(file("."))
  .aggregate(scalaAlgorithmFixtures)
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
    ),

    generateElmIR := {
      import scala.sys.process.*
      val elmFixturesRoot = baseDirectory.value / "test-fixtures" / "elm"
      val morphirElm = Seq("bash", "-lc", "morphir-elm make -f")
      val projectDirs =
        (elmFixturesRoot ** "morphir.json")
          .get
          .map(_.getParentFile)
          .sortBy(_.getAbsolutePath)

      projectDirs.map { elmProjectDir =>
        val exitCode = Process(morphirElm, elmProjectDir).!
        if (exitCode != 0) sys.error(s"morphir-elm make failed with exit code $exitCode in ${elmProjectDir.getAbsolutePath}")
        elmProjectDir / "morphir-ir.json"
      }
    },

    cleanFiles += baseDirectory.value / "test-fixtures" / "scala" / "target",

    // Pass fixture paths to the test JVM via system properties.
    Test / fork := true,
    Test / javaOptions ++= Seq(
      s"-Dtest.fixtures.scala.classes=${(scalaAlgorithmFixtures / Compile / classDirectory).value}",
      s"-Dtest.fixtures.elm.root=${baseDirectory.value / "test-fixtures" / "elm"}"
    ),

    // Ensure fixtures are compiled/generated before tests run.
    Test / test := (Test / test)
      .dependsOn(scalaAlgorithmFixtures / Compile / compile, generateElmIR)
      .value
  )

commands += Command.command("clean") { state =>
  val extracted = Project.extract(state)
  val (nextState, _) = extracted.runTask(root / clean, state)
  IO.delete((file("test-fixtures/elm") ** "morphir-ir.json").get)
  IO.delete((file("test-fixtures/elm") ** "morphir-hashes.json").get)
  IO.delete(file("test-fixtures/scala/target"))
  nextState
}
