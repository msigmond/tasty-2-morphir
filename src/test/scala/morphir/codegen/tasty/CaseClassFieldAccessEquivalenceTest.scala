package morphir.codegen.tasty

import io.circe.Json
import io.circe.parser.parse
import munit.FunSuite

import java.nio.file.{Files, Path, Paths}

/** Verifies case-class field access is converted to the same Morphir JSON as equivalent Elm record access. */
class CaseClassFieldAccessEquivalenceTest extends FunSuite:

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
    ("personAgePlusOne", List("PersonAgePlusOne", "PersonAgePlusOneRecord"), "person-age-plus-one"),
    ("accountBalanceWithFee", List("AccountBalanceWithFee", "AccountBalanceWithFeeRecord"), "account-balance-with-fee"),
    ("maybeAgeSelection", List("MaybeAgeSelection", "MaybeAgeSelectionRecord"), "maybe-age-selection")
  )

  private def scalaIR(scalaTypeNames: List[String]): Json =
    val output = Files.createTempFile("tasty2morphir-field-access-test-", ".json")
    try
      val tastyPaths = scalaTypeNames.map(name => scalaClassesDir.resolve(s"arithmetic/$name.tasty").toString)
      tastyToMorphirIR(output.toString, tastyPaths*)
      parseJson(output, s"Scala IR for ${scalaTypeNames.mkString(", ")}")
    finally
      Files.deleteIfExists(output)

  private def elmIR(elmProjectDir: String): Json =
    parseJson(elmFixturesRoot.resolve(elmProjectDir).resolve("morphir-ir.json"), s"Elm IR for $elmProjectDir")

  private def parseJson(path: Path, label: String): Json =
    parse(Files.readString(path)).fold(
      err => fail(s"Failed to parse $label: $err"),
      identity
    )

  private def assertCaseMatches(caseName: String, scalaTypeNames: List[String], elmProjectDir: String): Unit =
    assertEquals(
      scalaIR(scalaTypeNames),
      elmIR(elmProjectDir),
      s"Full JSON differs for case class field-access fixture $caseName"
    )

  cases.foreach { (caseName, scalaTypeNames, elmProjectDir) =>
    test(s"$caseName: Scala case class field access and Elm record access distributions are identical") {
      assertCaseMatches(caseName, scalaTypeNames, elmProjectDir)
    }
  }
