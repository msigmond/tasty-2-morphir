package morphir.codegen.tasty

enum ParityLayer:
  case Language
  case ElmCore
  case MorphirSdk
  case ExcludedElmCore

final case class ParitySurface(
  id: String,
  displayName: String,
  layer: ParityLayer,
  family: String,
  goal: String
)

object ParityCatalog:
  val languageSurfaces: List[ParitySurface] = List(
    ParitySurface("language-modules-packages", "Modules and packages", ParityLayer.Language, "distribution", "Represent Scala package and module structure as Morphir package and module paths."),
    ParitySurface("language-type-aliases-records", "Type aliases and records", ParityLayer.Language, "types", "Represent Elm type aliases and records through Scala record-oriented encodings."),
    ParitySurface("language-custom-types", "Custom types", ParityLayer.Language, "types", "Represent Elm custom types through Scala enum and ADT encodings."),
    ParitySurface("language-literals", "Literals", ParityLayer.Language, "values", "Represent the pure literal surface used by Elm and Morphir."),
    ParitySurface("language-functions-lambdas", "Functions and lambdas", ParityLayer.Language, "values", "Represent top-level values, local functions, and lambda expressions as Morphir values."),
    ParitySurface("language-let-if-case", "Let, if, and case expressions", ParityLayer.Language, "values", "Represent Elm-style branching and local binding through Scala expression forms."),
    ParitySurface("language-patterns", "Patterns", ParityLayer.Language, "patterns", "Support the pattern-matching surface needed to model Elm case expressions."),
    ParitySurface("language-tuples-record-access", "Tuples and record access", ParityLayer.Language, "values", "Support tuple values, tuple destructuring, field access, and record-oriented expression forms.")
  )

  val elmCoreBackedSurfaces: List[ParitySurface] = List(
    ParitySurface("elmcore-basics-bool", "Basics and Bool", ParityLayer.ElmCore, "basics", "Cover the pure boolean and basics-oriented surface that Morphir maps into its SDK."),
    ParitySurface("elmcore-comparable-equality", "Comparable and Equality", ParityLayer.ElmCore, "basics", "Cover equality and ordering semantics that back comparisons across supported values."),
    ParitySurface("elmcore-int-float-number", "Int, Float, and Number", ParityLayer.ElmCore, "numbers", "Cover the numeric surface that can be represented cleanly in Morphir."),
    ParitySurface("elmcore-list", "List", ParityLayer.ElmCore, "collections", "Cover the list type and the high-value list function surface."),
    ParitySurface("elmcore-maybe", "Maybe", ParityLayer.ElmCore, "sdk", "Cover the Maybe type and helper combinators through Scala Option encodings."),
    ParitySurface("elmcore-result", "Result", ParityLayer.ElmCore, "sdk", "Cover the Result type and its pure success/error combinators."),
    ParitySurface("elmcore-string-char", "String and Char", ParityLayer.ElmCore, "text", "Cover the pure text surface that Morphir maps through its SDK."),
    ParitySurface("elmcore-function", "Function helpers", ParityLayer.ElmCore, "functions", "Cover function-composition and helper forms that have direct Morphir value equivalents.")
  )

  val morphirSdkSurfaces: List[ParitySurface] = List(
    ParitySurface("sdk-aggregate", "Morphir.SDK.Aggregate", ParityLayer.MorphirSdk, "domain-sdk", "Catalog the aggregate-oriented Morphir SDK surface for later Scala parity work."),
    ParitySurface("sdk-decimal", "Morphir.SDK.Decimal", ParityLayer.MorphirSdk, "numbers", "Catalog the dedicated decimal SDK surface beyond plain Elm core numbers."),
    ParitySurface("sdk-dict", "Morphir.SDK.Dict", ParityLayer.MorphirSdk, "collections", "Catalog the dictionary surface used for Elm Dict parity."),
    ParitySurface("sdk-instant", "Morphir.SDK.Instant", ParityLayer.MorphirSdk, "time", "Catalog the instant-oriented time SDK surface."),
    ParitySurface("sdk-json", "Morphir.SDK.Json.*", ParityLayer.MorphirSdk, "integration", "Catalog the JSON-related SDK surface exposed under Morphir.SDK.Json."),
    ParitySurface("sdk-key", "Morphir.SDK.Key", ParityLayer.MorphirSdk, "domain-sdk", "Catalog the key and identifier helper surface provided by Morphir."),
    ParitySurface("sdk-local-date", "Morphir.SDK.LocalDate", ParityLayer.MorphirSdk, "time", "Catalog the local-date SDK surface."),
    ParitySurface("sdk-local-time", "Morphir.SDK.LocalTime", ParityLayer.MorphirSdk, "time", "Catalog the local-time SDK surface."),
    ParitySurface("sdk-result-list", "Morphir.SDK.ResultList", ParityLayer.MorphirSdk, "sdk", "Catalog the result-list helper surface that goes beyond plain Elm Result."),
    ParitySurface("sdk-rule", "Morphir.SDK.Rule", ParityLayer.MorphirSdk, "domain-sdk", "Catalog the rule-oriented Morphir SDK surface."),
    ParitySurface("sdk-stateful-app", "Morphir.SDK.StatefulApp", ParityLayer.MorphirSdk, "domain-sdk", "Catalog the stateful-app SDK surface and decide later what pure subset is representable from Scala."),
    ParitySurface("sdk-uuid", "Morphir.SDK.UUID", ParityLayer.MorphirSdk, "identifiers", "Catalog the UUID SDK surface."),
    ParitySurface("sdk-validate", "Morphir.SDK.Validate", ParityLayer.MorphirSdk, "domain-sdk", "Catalog the validation helper surface provided by Morphir.")
  )

  val excludedElmCoreModules: List[ParitySurface] = List(
    ParitySurface("excluded-debug", "Debug", ParityLayer.ExcludedElmCore, "excluded", "Out of scope because Debug has no business-logic Morphir parity target."),
    ParitySurface("excluded-platform", "Platform", ParityLayer.ExcludedElmCore, "excluded", "Out of scope because Platform is effect-oriented and not a pure Morphir target."),
    ParitySurface("excluded-process", "Process", ParityLayer.ExcludedElmCore, "excluded", "Out of scope because Process is effect-oriented and not a pure Morphir target."),
    ParitySurface("excluded-task", "Task", ParityLayer.ExcludedElmCore, "excluded", "Out of scope because Task is effect-oriented and not a pure Morphir target.")
  )

  val allTargetSurfaces: List[ParitySurface] =
    languageSurfaces ++ elmCoreBackedSurfaces ++ morphirSdkSurfaces ++ excludedElmCoreModules

  val targetedMorphirSdkModules: List[String] =
    morphirSdkSurfaces.map(_.displayName).sorted

  val excludedElmCoreModuleNames: List[String] =
    excludedElmCoreModules.map(_.displayName).sorted
