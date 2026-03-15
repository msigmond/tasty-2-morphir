package morphir.codegen.tasty

/** Verifies mixed-namespace multi-TASTy conversions match Elm-generated Morphir JSON baselines. */
class MixedNamespaceMultiTastyTest extends TastyEquivalenceSuite:

  private val cases = List(
    EquivalenceCase("mixedNamespaceTypeReference", List("a/b/c/NestedRecord.tasty", "a/b/UseNestedRecord.tasty"), "mixed-namespace-type-ref"),
    EquivalenceCase("mixedNamespaceValueReference", List("a/b/c/AddOne.tasty", "a/b/UseNestedAddOne.tasty"), "mixed-namespace-value-ref")
  )

  registerEquivalenceCases(cases)(
    testName = equivalenceCase => s"${equivalenceCase.caseName}: mixed-namespace Scala and Elm distributions are identical",
    descriptionPrefix = "mixed-namespace fixture"
  )
