package morphir.codegen.tasty

import munit.FunSuite

class ParityCatalogTest extends FunSuite:
  private val includedSurfaces = ParityCatalog.includedTargetSurfaces

  test("parity catalog entries have unique ids and non-empty goals") {
    val ids = ParityCatalog.allTargetSurfaces.map(_.id)

    assertEquals(ids.distinct.size, ids.size)
    assert(ParityCatalog.allTargetSurfaces.forall(_.goal.nonEmpty), "Every parity surface should explain its target")
    assert(ParityCatalog.allTargetSurfaces.forall(_.family.nonEmpty), "Every parity surface should belong to a family")
  }

  test("parity catalog tracks the targeted Morphir SDK module families") {
    assertEquals(
      ParityCatalog.targetedMorphirSdkModules,
      List(
        "Morphir.SDK.Aggregate",
        "Morphir.SDK.Decimal",
        "Morphir.SDK.Dict",
        "Morphir.SDK.Instant",
        "Morphir.SDK.Json.*",
        "Morphir.SDK.Key",
        "Morphir.SDK.LocalDate",
        "Morphir.SDK.LocalTime",
        "Morphir.SDK.ResultList",
        "Morphir.SDK.Rule",
        "Morphir.SDK.StatefulApp",
        "Morphir.SDK.UUID",
        "Morphir.SDK.Validate"
      )
    )
  }

  test("each included parity surface maps to at least one Scala core form") {
    assert(
      includedSurfaces.forall(_.scalaCoreForms.nonEmpty),
      "Every included parity surface should have at least one Scala core encoding"
    )
  }

  test("catalog captures sugar-oriented Scala forms for high-value targets") {
    val sugaredIds = ParityCatalog.surfacesWithScalaSugars.map(_.id).toSet

    assert(sugaredIds.contains("language-functions-lambdas"))
    assert(sugaredIds.contains("language-patterns"))
    assert(sugaredIds.contains("elmcore-list"))
    assert(sugaredIds.contains("elmcore-maybe"))
    assert(sugaredIds.contains("sdk-dict"))
  }

  test("parity catalog keeps Elm effect modules explicitly out of scope") {
    assertEquals(
      ParityCatalog.excludedElmCoreModuleNames,
      List("Debug", "Platform", "Process", "Task")
    )
  }

  test("parity catalog covers language, elm-core, Morphir SDK, and excluded layers") {
    val layers = ParityCatalog.allTargetSurfaces.map(_.layer).toSet

    assert(layers.contains(ParityLayer.Language))
    assert(layers.contains(ParityLayer.ElmCore))
    assert(layers.contains(ParityLayer.MorphirSdk))
    assert(layers.contains(ParityLayer.ExcludedElmCore))
  }

  test("excluded Elm effect modules intentionally have no Scala encodings") {
    assert(ParityCatalog.excludedElmCoreModules.forall(_.scalaCoreForms.isEmpty))
    assert(ParityCatalog.excludedElmCoreModules.forall(_.scalaSugarForms.isEmpty))
  }
