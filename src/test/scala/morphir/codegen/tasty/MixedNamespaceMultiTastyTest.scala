package morphir.codegen.tasty

import io.circe.Json
import io.circe.parser.parse
import munit.FunSuite

import java.nio.file.{Files, Path, Paths}

/** Verifies mixed-namespace multi-TASTy conversions match Elm-generated Morphir JSON baselines. */
class MixedNamespaceMultiTastyTest extends FunSuite:

  private val scalaClassesDir: Path = Paths.get(
    sys.props.getOrElse(
      "test.fixtures.scala.classes",
      "target/scala-algorithm-fixtures/scala-3.3.7/classes"
    )
  )

  private val elmFixturesRoot: Path = Paths.get(
    sys.props.getOrElse(
      "test.fixtures.elm.root",
      "test-fixtures/elm"
    )
  )

  private val cases = List(
    (
      "mixedNamespaceTypeReference",
      List("a/b/c/NestedRecord.tasty", "a/b/UseNestedRecord.tasty"),
      "mixed-namespace-type-ref"
    ),
    (
      "mixedNamespaceValueReference",
      List("a/b/c/AddOne.tasty", "a/b/UseNestedAddOne.tasty"),
      "mixed-namespace-value-ref"
    )
  )

  private def scalaIR(relativeTastyPaths: List[String]): Json =
    val output = Files.createTempFile("tasty2morphir-mixed-namespace-test-", ".json")
    try
      val tastyPaths = relativeTastyPaths.map(path => scalaClassesDir.resolve(path).toString)
      tastyToMorphirIR(output.toString, tastyPaths*)
      parseJson(output, s"Scala IR for ${relativeTastyPaths.mkString(", ")}")
    finally
      Files.deleteIfExists(output)

  private def elmIR(elmProjectDir: String): Json =
    parseJson(elmFixturesRoot.resolve(elmProjectDir).resolve("morphir-ir.json"), s"Elm IR for $elmProjectDir")

  private def parseJson(path: Path, label: String): Json =
    parse(Files.readString(path)).fold(
      err => fail(s"Failed to parse $label: $err"),
      identity
    )

  private def assertCaseMatches(caseName: String, relativeTastyPaths: List[String], elmProjectDir: String): Unit =
    assertEquals(
      scalaIR(relativeTastyPaths),
      elmIR(elmProjectDir),
      s"Full JSON differs for mixed-namespace fixture $caseName"
    )

  cases.foreach { (caseName, relativeTastyPaths, elmProjectDir) =>
    test(s"$caseName: mixed-namespace Scala and Elm distributions are identical") {
      assertCaseMatches(caseName, relativeTastyPaths, elmProjectDir)
    }
  }
