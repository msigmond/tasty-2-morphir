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

  private val scalarCases = List(
    arithmeticCase("add", "Add", "add"),
    arithmeticCase("subtract", "Subtract", "subtract"),
    arithmeticCase("multiply", "Multiply", "multiply"),
    arithmeticCase("isPositive", "IsPositive", "is-positive"),
    arithmeticCase("booleanLiteral", "BooleanLiteral", "boolean-literal"),
    arithmeticCase("booleanAnd", "BooleanAnd", "boolean-and"),
    arithmeticCase("booleanOr", "BooleanOr", "boolean-or"),
    arithmeticCase("doubleLiteral", "DoubleLiteral", "double-literal"),
    arithmeticCase("charLiteral", "CharLiteral", "char-literal"),
    arithmeticCase("stringLiteral", "StringLiteral", "string-literal"),
    arithmeticCase("doubleAdd", "DoubleAdd", "double-add"),
    arithmeticCase("longLiteral", "LongLiteral", "long-literal"),
    arithmeticCase("longAdd", "LongAdd", "long-add"),
    arithmeticCase("clamp", "Clamp", "clamp"),
    arithmeticCase("integerDivide", "IntegerDivide", "integer-divide"),
    arithmeticCase("helperCall", "HelperCall", "helper-call"),
    arithmeticCase("curriedHelperCall", "CurriedHelperCall", "curried-helper-call"),
    arithmeticCase("decimalAdd", "DecimalAdd", "decimal-add"),
    arithmeticCase("decimalSubtract", "DecimalSubtract", "decimal-subtract"),
    arithmeticCase("decimalMultiply", "DecimalMultiply", "decimal-multiply"),
    arithmeticCase("decimalLt", "DecimalLt", "decimal-lt"),
    arithmeticCase("decimalLte", "DecimalLte", "decimal-lte"),
    arithmeticCase("decimalGt", "DecimalGt", "decimal-gt"),
    arithmeticCase("decimalGte", "DecimalGte", "decimal-gte")
  )

  private val collectionCases = List(
    arithmeticCase("listEmpty", "ListEmpty", "list-empty"),
    arithmeticCase("listLiteral", "ListLiteral", "list-literal"),
    arithmeticCase("nestedListLiteral", "NestedListLiteral", "nested-list-literal"),
    arithmeticCase("listLength", "ListLength", "list-length"),
    arithmeticCase("listMapIncrement", "ListMapIncrement", "list-map-increment"),
    arithmeticCase("listFilterPositive", "ListFilterPositive", "list-filter-positive"),
    arithmeticCase("listFlatMapExpand", "ListFlatMapExpand", "list-flatmap-expand"),
    arithmeticCase("listFoldSum", "ListFoldSum", "list-fold-sum"),
    arithmeticCase("listMapTupleMatch", "ListMapTupleMatch", "list-map-tuple-match"),
    arithmeticCase("listCollectSomeIncrement", "ListCollectSomeIncrement", "list-collect-some-increment"),
    arithmeticCase("seqLiteral", "SeqLiteral", "seq-literal"),
    arithmeticCase("seqMapIncrement", "SeqMapIncrement", "seq-map-increment"),
    arithmeticCase("seqFlatMapExpand", "SeqFlatMapExpand", "seq-flatmap-expand"),
    arithmeticCase("seqFoldSum", "SeqFoldSum", "seq-fold-sum"),
    arithmeticCase("mapLiteral", "MapLiteral", "map-literal"),
    arithmeticCase("mapPassThrough", "MapPassThrough", "map-pass-through"),
    arithmeticCase("mapGet", "MapGet", "map-get"),
    arithmeticCase("forYieldIncrement", "ForYieldIncrement", "for-yield-increment"),
    arithmeticCase("forYieldExpand", "ForYieldExpand", "for-yield-expand")
  )

  private val tupleCases = List(
    arithmeticCase("tupleLiteral", "TupleLiteral", "tuple-literal"),
    arithmeticCase("tuplePassThrough", "TuplePassThrough", "tuple-pass-through"),
    arithmeticCase("extractFirst", "ExtractFirst", "extract-first"),
    arithmeticCase("extractFirstOf3", "ExtractFirstOf3", "extract-first-of-3"),
    arithmeticCase("extractFirstOf4", "ExtractFirstOf4", "extract-first-of-4"),
    arithmeticCase("extractSecond", "ExtractSecond", "extract-second"),
    arithmeticCase("tupleDestructureAdd", "TupleDestructureAdd", "tuple-destructure-add"),
    arithmeticCase("tupleDestructureAdd3", "TupleDestructureAdd3", "tuple-destructure-add-3"),
    arithmeticCase("tupleDestructureAdd4", "TupleDestructureAdd4", "tuple-destructure-add-4")
  )

  private val bindingCases = List(
    arithmeticCase("localVal", "LocalVal", "local-val"),
    arithmeticCase("localValChain", "LocalValChain", "local-val-chain"),
    arithmeticCase("localValHelperCall", "LocalValHelperCall", "local-val-helper-call")
  )

  private val maybeCases = List(
    arithmeticCase("maybeJust", "MaybeJust", "maybe-just"),
    arithmeticCase("maybeNothing", "MaybeNothing", "maybe-nothing"),
    arithmeticCase("maybePositive", "MaybePositive", "maybe-positive")
  )

  private val matchCases = List(
    arithmeticCase("booleanLiteralMatch", "BooleanLiteralMatch", "boolean-literal-match"),
    arithmeticCase("intLiteralMatch", "IntLiteralMatch", "int-literal-match"),
    arithmeticCase("floatLiteralMatch", "FloatLiteralMatch", "float-literal-match"),
    arithmeticCase("stringLiteralMatch", "StringLiteralMatch", "string-literal-match"),
    arithmeticCase("maybeMatchDefault", "MaybeMatchDefault", "maybe-match-default"),
    arithmeticCase("maybeMatchIncrement", "MaybeMatchIncrement", "maybe-match-increment"),
    arithmeticCase("maybeMatchMap", "MaybeMatchMap", "maybe-match-map")
  )

  private val adtCases = List(
    arithmeticMultiFileCase("simpleColorAdt", List("Color", "MatchRed", "WrapRed"), "simple-color-adt"),
    arithmeticMultiFileCase("colorWithValueAdt", List("ColorWithValue", "GetIntensity", "RedFortyTwo"), "color-with-value-adt"),
    arithmeticMultiFileCase("colorWithTwoValuesAdt", List("ColorWithTwoValues", "GetNamedIntensity", "NamedRed"), "color-with-two-values-adt"),
    arithmeticMultiFileCase("colorWithThreeValuesAdt", List("ColorWithThreeValues", "GetPrimaryValue", "NamedRedPriority"), "color-with-three-values-adt")
  )

  private val equivalenceCases =
    scalarCases ++
      collectionCases ++
      tupleCases ++
      bindingCases ++
      maybeCases ++
      matchCases ++
      adtCases

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
