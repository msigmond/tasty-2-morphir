package morphir.codegen.tasty

/** Verifies supported Scala-to-Morphir mappings.
  *
  * Most supported mappings are tested via full JSON equivalence between a one-to-one
  * Elm fixture project and the equivalent Scala .tasty file. BigDecimal division is the
  * one exception: Scala maps `/` to `morphir.SDK.decimal.div.unsafe`, while the Morphir
  * Elm SDK exposes `Decimal.div : Decimal -> Decimal -> Maybe Decimal`, so that case is
  * covered with a targeted Scala-side mapping assertion.
  */
class SupportedFunctionEquivalenceTest extends TastyEquivalenceSuite:

  private val equivalenceCases = List(
    arithmeticCase("add", "Add", "add"),
    arithmeticCase("subtract", "Subtract", "subtract"),
    arithmeticCase("multiply", "Multiply", "multiply"),
    arithmeticCase("isPositive", "IsPositive", "is-positive"),
    arithmeticCase("clamp", "Clamp", "clamp"),
    arithmeticCase("integerDivide", "IntegerDivide", "integer-divide"),
    arithmeticCase("decimalAdd", "DecimalAdd", "decimal-add"),
    arithmeticCase("decimalSubtract", "DecimalSubtract", "decimal-subtract"),
    arithmeticCase("decimalMultiply", "DecimalMultiply", "decimal-multiply"),
    arithmeticCase("decimalLt", "DecimalLt", "decimal-lt"),
    arithmeticCase("decimalLte", "DecimalLte", "decimal-lte"),
    arithmeticCase("decimalGt", "DecimalGt", "decimal-gt"),
    arithmeticCase("decimalGte", "DecimalGte", "decimal-gte"),
    arithmeticCase("maybeJust", "MaybeJust", "maybe-just"),
    arithmeticCase("maybeNothing", "MaybeNothing", "maybe-nothing"),
    arithmeticCase("maybePositive", "MaybePositive", "maybe-positive")
  )

  registerEquivalenceCases(equivalenceCases)(
    testName = equivalenceCase => s"${equivalenceCase.caseName}: Scala and Elm distributions are identical",
    descriptionPrefix = "fixture case"
  )

  test("decimalDivide: Scala IR uses morphir.SDK.decimal.div.unsafe") {
    val valueDefinition = firstValueDefinition(scalaIR(List("arithmetic/DecimalDivide.tasty"), "Scala IR for arithmetic/DecimalDivide.tasty"))
    val outputType = valueDefinition.hcursor.downField("outputType").focus.getOrElse(fail("Missing outputType"))
    val body = valueDefinition.hcursor.downField("body").focus.getOrElse(fail("Missing body"))

    assert(!outputType.noSpaces.contains("\"maybe\""), s"Expected Decimal output type, got: ${outputType.noSpaces}")
    assert(body.noSpaces.contains("[\"div\",\"unsafe\"]"), s"Expected decimal.div.unsafe in body, got: ${body.noSpaces}")
  }
