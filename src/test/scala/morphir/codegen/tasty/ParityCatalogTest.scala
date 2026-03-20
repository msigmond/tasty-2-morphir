package morphir.codegen.tasty

import munit.FunSuite

class ParityCatalogTest extends FunSuite:

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
