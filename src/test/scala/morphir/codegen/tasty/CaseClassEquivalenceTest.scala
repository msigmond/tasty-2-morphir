package morphir.codegen.tasty

import io.circe.Json
import io.circe.parser.parse
import munit.FunSuite

import java.nio.file.{Files, Path, Paths}

/** Verifies Scala case classes are converted to the same Morphir JSON as equivalent Elm type aliases. */
class CaseClassEquivalenceTest extends FunSuite:

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
    ("personBasic", "PersonBasic", "person-basic"),
    ("accountBalance", "AccountBalance", "account-balance"),
    ("maybeAge", "MaybeAge", "maybe-age"),
    ("mixedRecord", "MixedRecord", "mixed-record")
  )

  private def scalaIR(scalaTypeName: String): Json =
    val output = Files.createTempFile("tasty2morphir-type-test-", ".json")
    try
      val tastyPath = scalaClassesDir.resolve(s"arithmetic/$scalaTypeName.tasty")
      tastyToMorphirIR(output.toString, tastyPath.toString)
      parseJson(output, s"Scala IR for $scalaTypeName")
    finally
      Files.deleteIfExists(output)

  private def elmIR(elmProjectDir: String): Json =
    parseJson(elmFixturesRoot.resolve(elmProjectDir).resolve("morphir-ir.json"), s"Elm IR for $elmProjectDir")

  private def parseJson(path: Path, label: String): Json =
    parse(Files.readString(path)).fold(
      err => fail(s"Failed to parse $label: $err"),
      identity
    )

  private def assertCaseMatches(caseName: String, scalaTypeName: String, elmProjectDir: String): Unit =
    assertEquals(
      scalaIR(scalaTypeName),
      elmIR(elmProjectDir),
      s"Full JSON differs for case class fixture $caseName"
    )

  cases.foreach { (caseName, scalaTypeName, elmProjectDir) =>
    test(s"$caseName: Scala case class and Elm type alias distributions are identical") {
      assertCaseMatches(caseName, scalaTypeName, elmProjectDir)
    }
  }
