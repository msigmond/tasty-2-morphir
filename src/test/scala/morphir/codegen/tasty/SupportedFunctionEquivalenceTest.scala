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
    arithmeticCase("booleanLiteral", "BooleanLiteral", "boolean-literal"),
    arithmeticCase("booleanAnd", "BooleanAnd", "boolean-and"),
    arithmeticCase("booleanOr", "BooleanOr", "boolean-or"),
    arithmeticCase("doubleLiteral", "DoubleLiteral", "double-literal"),
    arithmeticCase("doubleAdd", "DoubleAdd", "double-add"),
    arithmeticCase("tupleLiteral", "TupleLiteral", "tuple-literal"),
    arithmeticCase("tuplePassThrough", "TuplePassThrough", "tuple-pass-through"),
    arithmeticCase("clamp", "Clamp", "clamp"),
    arithmeticCase("integerDivide", "IntegerDivide", "integer-divide"),
    arithmeticCase("localVal", "LocalVal", "local-val"),
    arithmeticCase("localValChain", "LocalValChain", "local-val-chain"),
    arithmeticCase("localValHelperCall", "LocalValHelperCall", "local-val-helper-call"),
    arithmeticCase("helperCall", "HelperCall", "helper-call"),
    arithmeticCase("curriedHelperCall", "CurriedHelperCall", "curried-helper-call"),
    arithmeticCase("decimalAdd", "DecimalAdd", "decimal-add"),
    arithmeticCase("decimalSubtract", "DecimalSubtract", "decimal-subtract"),
    arithmeticCase("decimalMultiply", "DecimalMultiply", "decimal-multiply"),
    arithmeticCase("decimalLt", "DecimalLt", "decimal-lt"),
    arithmeticCase("decimalLte", "DecimalLte", "decimal-lte"),
    arithmeticCase("decimalGt", "DecimalGt", "decimal-gt"),
    arithmeticCase("decimalGte", "DecimalGte", "decimal-gte"),
    arithmeticCase("maybeJust", "MaybeJust", "maybe-just"),
    arithmeticCase("maybeNothing", "MaybeNothing", "maybe-nothing"),
    arithmeticCase("maybePositive", "MaybePositive", "maybe-positive"),
    arithmeticCase("booleanLiteralMatch", "BooleanLiteralMatch", "boolean-literal-match"),
    arithmeticCase("intLiteralMatch", "IntLiteralMatch", "int-literal-match"),
    arithmeticCase("stringLiteralMatch", "StringLiteralMatch", "string-literal-match"),
    arithmeticCase("maybeMatchDefault", "MaybeMatchDefault", "maybe-match-default"),
    arithmeticCase("maybeMatchIncrement", "MaybeMatchIncrement", "maybe-match-increment"),
    arithmeticCase("maybeMatchMap", "MaybeMatchMap", "maybe-match-map")
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
