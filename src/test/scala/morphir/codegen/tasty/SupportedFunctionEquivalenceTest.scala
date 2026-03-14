package morphir.codegen.tasty

import io.circe.Json
import io.circe.parser.parse
import munit.FunSuite

import java.nio.file.{Files, Path, Paths}

/** Verifies supported Scala-to-Morphir mappings.
  *
  * Most supported mappings are tested via full JSON equivalence between a one-to-one
  * Elm fixture project and the equivalent Scala .tasty file. BigDecimal division is the
  * one exception: Scala maps `/` to `morphir.SDK.decimal.div.unsafe`, while the Morphir
  * Elm SDK exposes `Decimal.div : Decimal -> Decimal -> Maybe Decimal`, so that case is
  * covered with a targeted Scala-side mapping assertion.
  */
class SupportedFunctionEquivalenceTest extends FunSuite:

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

  private val equivalenceCases = List(
    ("add", "Add", "add"),
    ("subtract", "Subtract", "subtract"),
    ("multiply", "Multiply", "multiply"),
    ("isPositive", "IsPositive", "is-positive"),
    ("clamp", "Clamp", "clamp"),
    ("integerDivide", "IntegerDivide", "integer-divide"),
    ("decimalAdd", "DecimalAdd", "decimal-add"),
    ("decimalSubtract", "DecimalSubtract", "decimal-subtract"),
    ("decimalMultiply", "DecimalMultiply", "decimal-multiply"),
    ("decimalLt", "DecimalLt", "decimal-lt"),
    ("decimalLte", "DecimalLte", "decimal-lte"),
    ("decimalGt", "DecimalGt", "decimal-gt"),
    ("decimalGte", "DecimalGte", "decimal-gte"),
    ("maybeJust", "MaybeJust", "maybe-just"),
    ("maybeNothing", "MaybeNothing", "maybe-nothing"),
    ("maybePositive", "MaybePositive", "maybe-positive")
  )

  private def scalaIR(scalaObjectName: String): Json =
    val output = Files.createTempFile("tasty2morphir-test-", ".json")
    try
      val tastyPath = scalaClassesDir.resolve(s"arithmetic/$scalaObjectName.tasty")
      tastyToMorphirIR(output.toString, tastyPath.toString)
      parseJson(output, s"Scala IR for $scalaObjectName")
    finally
      Files.deleteIfExists(output)

  private def elmIR(elmProjectDir: String): Json =
    parseJson(elmFixturesRoot.resolve(elmProjectDir).resolve("morphir-ir.json"), s"Elm IR for $elmProjectDir")

  private def parseJson(path: Path, label: String): Json =
    parse(Files.readString(path)).fold(
      err => fail(s"Failed to parse $label: $err"),
      identity
    )

  private def assertCaseMatches(caseName: String, scalaObjectName: String, elmProjectDir: String): Unit =
    assertEquals(
      scalaIR(scalaObjectName),
      elmIR(elmProjectDir),
      s"Full JSON differs for fixture case $caseName"
    )

  private def firstValueDefinition(json: Json): Json =
    (for
      distribution <- json.hcursor.downField("distribution").focus.flatMap(_.asArray)
      library <- distribution.lift(3)
      modules <- library.hcursor.downField("modules").focus.flatMap(_.asArray)
      module <- modules.headOption
      moduleFields <- module.asArray
      moduleBody <- moduleFields.lift(1)
      values <- moduleBody.hcursor.downField("value").downField("values").focus.flatMap(_.asArray)
      value <- values.headOption
      valueFields <- value.asArray
      accessControlled <- valueFields.lift(1)
      definition <- accessControlled.hcursor.downField("value").downField("value").focus
    yield definition).getOrElse(fail("Could not extract value definition from generated Scala IR"))

  equivalenceCases.foreach { (caseName, scalaObjectName, elmProjectDir) =>
    test(s"$caseName: Scala and Elm distributions are identical") {
      assertCaseMatches(caseName, scalaObjectName, elmProjectDir)
    }
  }

  test("decimalDivide: Scala IR uses morphir.SDK.decimal.div.unsafe") {
    val valueDefinition = firstValueDefinition(scalaIR("DecimalDivide"))
    val outputType = valueDefinition.hcursor.downField("outputType").focus.getOrElse(fail("Missing outputType"))
    val body = valueDefinition.hcursor.downField("body").focus.getOrElse(fail("Missing body"))

    assert(!outputType.noSpaces.contains("\"maybe\""), s"Expected Decimal output type, got: ${outputType.noSpaces}")
    assert(body.noSpaces.contains("[\"div\",\"unsafe\"]"), s"Expected decimal.div.unsafe in body, got: ${body.noSpaces}")
  }
