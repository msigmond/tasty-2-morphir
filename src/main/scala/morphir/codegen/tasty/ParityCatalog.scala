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
  goal: String,
  scalaCoreForms: List[String],
  scalaSugarForms: List[String]
)

object ParityCatalog:
  private def language(
    id: String,
    displayName: String,
    family: String,
    goal: String,
    scalaCoreForms: List[String],
    scalaSugarForms: List[String] = List.empty
  ): ParitySurface =
    ParitySurface(id, displayName, ParityLayer.Language, family, goal, scalaCoreForms, scalaSugarForms)

  private def elmCore(
    id: String,
    displayName: String,
    family: String,
    goal: String,
    scalaCoreForms: List[String],
    scalaSugarForms: List[String] = List.empty
  ): ParitySurface =
    ParitySurface(id, displayName, ParityLayer.ElmCore, family, goal, scalaCoreForms, scalaSugarForms)

  private def sdk(
    id: String,
    displayName: String,
    family: String,
    goal: String,
    scalaCoreForms: List[String],
    scalaSugarForms: List[String] = List.empty
  ): ParitySurface =
    ParitySurface(id, displayName, ParityLayer.MorphirSdk, family, goal, scalaCoreForms, scalaSugarForms)

  private def excluded(id: String, displayName: String, goal: String): ParitySurface =
    ParitySurface(id, displayName, ParityLayer.ExcludedElmCore, "excluded", goal, List.empty, List.empty)

  val languageSurfaces: List[ParitySurface] = List(
    language(
      "language-modules-packages",
      "Modules and packages",
      "distribution",
      "Represent Scala package and module structure as Morphir package and module paths.",
      scalaCoreForms = List("package a.b", "object Orders", "def evaluate = ..."),
      scalaSugarForms = List("multiple related .tasty inputs", "nested package clauses")
    ),
    language(
      "language-type-aliases-records",
      "Type aliases and records",
      "types",
      "Represent Elm type aliases and records through Scala record-oriented encodings.",
      scalaCoreForms = List("case class Person(name: String, age: Int)", "case class Wrapper(value: Option[Int])"),
      scalaSugarForms = List("case-class companion apply", "generic case-class type arguments")
    ),
    language(
      "language-custom-types",
      "Custom types",
      "types",
      "Represent Elm custom types through Scala enum and ADT encodings.",
      scalaCoreForms = List("enum Color { case Red, Blue }", "enum Shape { case Circle(radius: Int) }"),
      scalaSugarForms = List("sealed trait with case object/case class descendants", "constructor references used as values")
    ),
    language(
      "language-literals",
      "Literals",
      "values",
      "Represent the pure literal surface used by Elm and Morphir.",
      scalaCoreForms = List("true", "42", "42L", "1.5", "'x'", "\"hello\"", "BigDecimal(\"1.25\")"),
      scalaSugarForms = List("negative numeric literals", "infix arithmetic over literals")
    ),
    language(
      "language-functions-lambdas",
      "Functions and lambdas",
      "values",
      "Represent top-level values, local functions, and lambda expressions as Morphir values.",
      scalaCoreForms = List("def add(a: Int, b: Int): Int = a + b", "(value: Int) => value + 1"),
      scalaSugarForms = List("placeholder lambda syntax", "eta-expanded method values", "curried helper calls")
    ),
    language(
      "language-let-if-case",
      "Let, if, and case expressions",
      "values",
      "Represent Elm-style branching and local binding through Scala expression forms.",
      scalaCoreForms = List("val base = 1; base + 1", "if value > 0 then value else 0", "value match { case _ => ... }"),
      scalaSugarForms = List("block expressions", "for-yield desugaring into map/flatMap")
    ),
    language(
      "language-patterns",
      "Patterns",
      "patterns",
      "Support the pattern-matching surface needed to model Elm case expressions.",
      scalaCoreForms = List("case Some(value) => value", "case (left, right) => left + right", "case Red => 1"),
      scalaSugarForms = List("pattern matching inside lambdas", "destructuring val bindings", "pattern generators in for-comprehensions")
    ),
    language(
      "language-tuples-record-access",
      "Tuples and record access",
      "values",
      "Support tuple values, tuple destructuring, field access, and record-oriented expression forms.",
      scalaCoreForms = List("(left, right)", "pair._1", "person.age"),
      scalaSugarForms = List("tuple destructuring in val bindings", "case-class field access through copy/update pipelines")
    )
  )

  val elmCoreBackedSurfaces: List[ParitySurface] = List(
    elmCore(
      "elmcore-basics-bool",
      "Basics and Bool",
      "basics",
      "Cover the pure boolean and basics-oriented surface that Morphir maps into its SDK.",
      scalaCoreForms = List("value && other", "value || other", "if flag then left else right"),
      scalaSugarForms = List("infix boolean operators", "prefix not-style helper encodings")
    ),
    elmCore(
      "elmcore-comparable-equality",
      "Comparable and Equality",
      "basics",
      "Cover equality and ordering semantics that back comparisons across supported values.",
      scalaCoreForms = List("left == right", "left < right", "left >= right"),
      scalaSugarForms = List("ordered infix operators inside guards", "comparison helpers used in collection predicates")
    ),
    elmCore(
      "elmcore-int-float-number",
      "Int, Float, and Number",
      "numbers",
      "Cover the numeric surface that can be represented cleanly in Morphir.",
      scalaCoreForms = List("left + right", "left - right", "left * right", "left / right"),
      scalaSugarForms = List("numeric helper methods", "widened numeric literals", "BigDecimal companion constructors")
    ),
    elmCore(
      "elmcore-list",
      "List",
      "collections",
      "Cover the list type and the high-value list function surface.",
      scalaCoreForms = List("List(1, 2, 3)", "values.map(f)", "values.filter(p)", "values.foldLeft(z)(f)"),
      scalaSugarForms = List("Nil", "for value <- values yield ...", "collect { case ... => ... }")
    ),
    elmCore(
      "elmcore-maybe",
      "Maybe",
      "sdk",
      "Cover the Maybe type and helper combinators through Scala Option encodings.",
      scalaCoreForms = List("Option[Int]", "Some(value)", "None", "value match { case Some(x) => ...; case None => ... }"),
      scalaSugarForms = List("map/flatMap over Option", "for-comprehensions over Option")
    ),
    elmCore(
      "elmcore-result",
      "Result",
      "sdk",
      "Cover the Result type and its pure success/error combinators.",
      scalaCoreForms = List("Either[Error, Value]", "Right(value)", "Left(error)"),
      scalaSugarForms = List("map/flatMap over Either", "for-comprehensions over right-biased Either")
    ),
    elmCore(
      "elmcore-string-char",
      "String and Char",
      "text",
      "Cover the pure text surface that Morphir maps through its SDK.",
      scalaCoreForms = List("\"text\"", "'x'", "left + right"),
      scalaSugarForms = List("string interpolation lowered to concatenation", "char pattern matches")
    ),
    elmCore(
      "elmcore-function",
      "Function helpers",
      "functions",
      "Cover function-composition and helper forms that have direct Morphir value equivalents.",
      scalaCoreForms = List("(value: Int) => value + 1", "helper(value)"),
      scalaSugarForms = List("compose/andThen-style helper chains", "placeholder syntax for one-argument functions")
    )
  )

  val morphirSdkSurfaces: List[ParitySurface] = List(
    sdk(
      "sdk-aggregate",
      "Morphir.SDK.Aggregate",
      "domain-sdk",
      "Catalog the aggregate-oriented Morphir SDK surface for later Scala parity work.",
      scalaCoreForms = List("aggregate helper objects", "domain aggregate functions over case classes"),
      scalaSugarForms = List("extension-style aggregate helpers", "curried aggregate pipelines")
    ),
    sdk(
      "sdk-decimal",
      "Morphir.SDK.Decimal",
      "numbers",
      "Catalog the dedicated decimal SDK surface beyond plain Elm core numbers.",
      scalaCoreForms = List("BigDecimal(\"1.25\") + BigDecimal(\"2.0\")", "left < right", "left / right"),
      scalaSugarForms = List("BigDecimal companion literals", "decimal arithmetic in collection lambdas")
    ),
    sdk(
      "sdk-dict",
      "Morphir.SDK.Dict",
      "collections",
      "Catalog the dictionary surface used for Elm Dict parity.",
      scalaCoreForms = List("Map(\"a\" -> 1)", "values.get(key)"),
      scalaSugarForms = List("Map.apply with arrow pairs", "map/filter/fold pipelines over immutable maps")
    ),
    sdk(
      "sdk-instant",
      "Morphir.SDK.Instant",
      "time",
      "Catalog the instant-oriented time SDK surface.",
      scalaCoreForms = List("java.time.Instant.parse(\"2024-01-01T00:00:00Z\")"),
      scalaSugarForms = List("instant helper methods", "domain wrappers around Instant")
    ),
    sdk(
      "sdk-json",
      "Morphir.SDK.Json.*",
      "integration",
      "Catalog the JSON-related SDK surface exposed under Morphir.SDK.Json.",
      scalaCoreForms = List("JSON encoder/decoder helper values", "JSON-like ADT definitions"),
      scalaSugarForms = List("companion helpers for JSON codecs", "builder-style JSON combinators")
    ),
    sdk(
      "sdk-key",
      "Morphir.SDK.Key",
      "domain-sdk",
      "Catalog the key and identifier helper surface provided by Morphir.",
      scalaCoreForms = List("case class CustomerKey(value: String)", "key helper functions"),
      scalaSugarForms = List("opaque wrapper-style key constructors", "companion apply helpers for keys")
    ),
    sdk(
      "sdk-local-date",
      "Morphir.SDK.LocalDate",
      "time",
      "Catalog the local-date SDK surface.",
      scalaCoreForms = List("java.time.LocalDate.parse(\"2024-01-01\")"),
      scalaSugarForms = List("date helper methods", "domain wrappers around LocalDate")
    ),
    sdk(
      "sdk-local-time",
      "Morphir.SDK.LocalTime",
      "time",
      "Catalog the local-time SDK surface.",
      scalaCoreForms = List("java.time.LocalTime.parse(\"10:15:30\")"),
      scalaSugarForms = List("time helper methods", "domain wrappers around LocalTime")
    ),
    sdk(
      "sdk-result-list",
      "Morphir.SDK.ResultList",
      "sdk",
      "Catalog the result-list helper surface that goes beyond plain Elm Result.",
      scalaCoreForms = List("List[Either[Error, Value]]", "sequence-style helpers over Either lists"),
      scalaSugarForms = List("for-comprehensions combining lists and Either", "collection traversals that accumulate results")
    ),
    sdk(
      "sdk-rule",
      "Morphir.SDK.Rule",
      "domain-sdk",
      "Catalog the rule-oriented Morphir SDK surface.",
      scalaCoreForms = List("domain rule functions returning booleans or results", "rule evaluation helpers"),
      scalaSugarForms = List("predicate combinators", "rule pipelines over collections")
    ),
    sdk(
      "sdk-stateful-app",
      "Morphir.SDK.StatefulApp",
      "domain-sdk",
      "Catalog the stateful-app SDK surface and decide later what pure subset is representable from Scala.",
      scalaCoreForms = List("pure state-transition functions", "record-based app state models"),
      scalaSugarForms = List("copy/update state pipelines", "state transition helpers composed with andThen-style functions")
    ),
    sdk(
      "sdk-uuid",
      "Morphir.SDK.UUID",
      "identifiers",
      "Catalog the UUID SDK surface.",
      scalaCoreForms = List("java.util.UUID.fromString(\"00000000-0000-0000-0000-000000000000\")"),
      scalaSugarForms = List("UUID wrapper case classes", "companion helpers for UUID parsing")
    ),
    sdk(
      "sdk-validate",
      "Morphir.SDK.Validate",
      "domain-sdk",
      "Catalog the validation helper surface provided by Morphir.",
      scalaCoreForms = List("validation helper functions over Either or custom ADTs", "predicate-based validation rules"),
      scalaSugarForms = List("validation pipelines", "for-comprehension-based validation flows")
    )
  )

  val excludedElmCoreModules: List[ParitySurface] = List(
    excluded("excluded-debug", "Debug", "Out of scope because Debug has no business-logic Morphir parity target."),
    excluded("excluded-platform", "Platform", "Out of scope because Platform is effect-oriented and not a pure Morphir target."),
    excluded("excluded-process", "Process", "Out of scope because Process is effect-oriented and not a pure Morphir target."),
    excluded("excluded-task", "Task", "Out of scope because Task is effect-oriented and not a pure Morphir target.")
  )

  val allTargetSurfaces: List[ParitySurface] =
    languageSurfaces ++ elmCoreBackedSurfaces ++ morphirSdkSurfaces ++ excludedElmCoreModules

  val includedTargetSurfaces: List[ParitySurface] =
    allTargetSurfaces.filter(_.layer != ParityLayer.ExcludedElmCore)

  val targetedMorphirSdkModules: List[String] =
    morphirSdkSurfaces.map(_.displayName).sorted

  val excludedElmCoreModuleNames: List[String] =
    excludedElmCoreModules.map(_.displayName).sorted

  val surfacesWithScalaSugars: List[ParitySurface] =
    includedTargetSurfaces.filter(_.scalaSugarForms.nonEmpty)
