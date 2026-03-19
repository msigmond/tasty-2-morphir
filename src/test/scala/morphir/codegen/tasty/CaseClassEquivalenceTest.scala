package morphir.codegen.tasty

/** Verifies Scala case classes are converted to the same Morphir JSON as equivalent Elm type aliases. */
class CaseClassEquivalenceTest extends TastyEquivalenceSuite:

  private val cases = List(
    arithmeticCase("personBasic", "PersonBasic", "person-basic"),
    arithmeticCase("personWithMethods", "PersonWithMethods", "person-with-methods"),
    arithmeticCase("accountBalance", "AccountBalance", "account-balance"),
    arithmeticCase("maybeAge", "MaybeAge", "maybe-age"),
    arithmeticCase("mixedRecord", "MixedRecord", "mixed-record"),
    arithmeticCase("genericBox", "GenericBox", "generic-box"),
    arithmeticCase("dualBox", "DualBox", "dual-box"),
    arithmeticMultiFileCase("dualBoxContainer", List("DualBox", "DualBoxContainer"), "dual-box-container"),
    arithmeticMultiFileCase("nestedBoxContainer", List("GenericBox", "NestedBoxContainer"), "nested-box-container")
  )

  registerEquivalenceCases(cases)(
    testName = equivalenceCase => s"${equivalenceCase.caseName}: Scala case class and Elm type alias distributions are identical",
    descriptionPrefix = "case class fixture"
  )
