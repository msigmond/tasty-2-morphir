# TASTy to Morphir IR

This project converts Scala 3 TASTy (`.tasty`) files into Morphir IR JSON (distribution format `3`).

It uses the Scala 3 `scala3-tasty-inspector` API to read compiled Scala trees and emits a `VersionedDistribution` JSON suitable for Morphir-based analysis.

## Related projects

- Morphir JVM: <https://github.com/finos/morphir-jvm>
- Morphir Elm CLI: <https://github.com/finos/morphir-elm>
- Scala 3 TASTy Inspector docs: <https://docs.scala-lang.org/scala3/reference/metaprogramming/tasty-inspect.html>

## Requirements

- JDK `21`
- `sbt` `1.12.5`
- Scala `3.3.7` (managed by SBT)
- `morphir-elm` CLI available on `PATH`

## Build

```bash
sbt compile
```

## Run

Main entrypoint:

- `morphir.codegen.tasty.tastyToMorphirIR`

Examples:

```bash
# Convert one .tasty file
sbt "runMain morphir.codegen.tasty.tastyToMorphirIR /tmp/output.json /path/to/File.tasty"

# Convert multiple related .tasty files together
sbt "runMain morphir.codegen.tasty.tastyToMorphirIR /tmp/output.json /path/to/One.tasty /path/to/Two.tasty"
```

Arguments:

1. Output path for the generated Morphir IR JSON
2. One or more compiled Scala 3 `.tasty` files

## Examples

- `examples/CurrentSupportedExample.scala` shows the most complex concise Scala shape currently supported end-to-end.
- Keep this example updated as translation support expands so it remains the quick reference for current capabilities.

## Current conversion behavior

### Supported types

- `Boolean`
- `Int`
- `Long` (currently mapped to Morphir `int`)
- `Float`
- `Double` (mapped to Morphir `float`)
- `Char`
- `String`
- `BigDecimal`
- `Option[T]`
- `List[T]` for the current narrow empty-list, direct `List(...)` literal, nested-list literal, direct `length`, direct single-lambda `map` / `filter` / `flatMap`, narrow `foldLeft`, and narrow `collect` slices
- `Seq[T]` for the current normalization slice covering direct `Seq(...)` literals plus direct `map` / `flatMap` / `foldLeft` onto the proven Morphir list path
- `Map[K, V]` for the current narrow immutable type/literal slice covering direct `Map(...)` literals lowered from `key -> value` pairs
- narrow Scala `enum` custom types, including direct constructor payloads up to the current two-argument slice
- Scala tuples, currently the direct `(A, B)` value/type slice
- Scala `case class` data fields, including generic multi-parameter and nested record references

### Supported expressions

- literals, including `Boolean`, `Long`, `Char`, and direct `String` literals
- arithmetic operators: `+`, `-`, `*`, `/`
- boolean operators: `&&`, `||`
- comparison operators: `<`, `<=`, `>`, `>=`
- function application
- `if / else`
- pattern matching for `Option` constructors and supported scalar literal patterns, including `Float`
- constructor references, direct constructor application, and direct constructor pattern matches for the current narrow `enum` slice, including flat three-argument constructors
- tuple literals and tuple-typed pass-through values
- narrow tuple destructuring in `match` expressions for tuple element capture, including flat 4-tuples
- narrow tuple destructuring in local `val` bindings for direct tuple element capture, including flat 4-tuples
- local `val` bindings and block expressions
- case-class field access, including nested record access
- narrow case-class methods defined directly on case classes, including the current two-explicit-parameter slice and richer supported `if`-based method bodies
- empty list values via `List()` and `Nil`
- populated list values via `List(...)`
- nested list literals such as `List(List(1, 2), List(3))`
- direct `List.length`
- direct `List.map` with a single-argument lambda whose body stays within the supported expression surface
- direct `List.filter` with a single-argument lambda whose predicate stays within the supported expression surface
- direct `List.flatMap` with a single-argument lambda whose body returns another supported `List`
- narrow `List.foldLeft` with a two-parameter lambda whose body stays within the supported expression surface
- narrow `List.collect` with a single-case lambda `match` body that can be normalized to a `Maybe`-returning transform
- normalized immutable `Seq(...)` literals
- normalized immutable `Seq.map` / `Seq.flatMap` / `Seq.foldLeft` over the current supported lambda-expression surface
- narrow immutable `Map(...)` literals built from direct `key -> value` pairs
- narrow immutable `Map[K, V]` pass-through signatures
- narrow immutable `Map.get` lookups that return `Option` / Morphir `Maybe`
- pure list `for`-`yield` traversal shapes that lower to the already-supported `map` / `flatMap` surface
- pattern-aware list transforms via explicit `match` expressions inside supported list lambdas, starting with tuple matches

### Mapping notes

- `Int /` maps to Morphir `integerDivide`
- `BigDecimal` arithmetic and comparison map to Morphir decimal SDK functions
- Scala `BigDecimal /` maps to `morphir.SDK.decimal.div.unsafe`
- Scala `Long` currently maps to Morphir `int`
- Scala `Double` maps to Morphir `float`
- Scala `Char` maps to Morphir `char`
- Scala `List[T]` maps to Morphir `morphir.SDK.list.list[T]`
- Scala `List.length` maps to Morphir `morphir.SDK.list.length`
- Scala `List.map` maps to Morphir `morphir.SDK.list.map`
- Scala `List.filter` maps to Morphir `morphir.SDK.list.filter`
- Scala `List.flatMap` maps to Morphir `morphir.SDK.list.concatMap`
- Scala `List.foldLeft` maps to Morphir `morphir.SDK.list.foldl`
- Scala `List.collect` maps to Morphir `morphir.SDK.list.filterMap` for the current single-case partial-function slice
- narrow immutable Scala `Seq[T]` currently normalizes to the same Morphir `morphir.SDK.list.list[T]` path as `List[T]`
- narrow immutable Scala `Map[K, V]` currently maps to Morphir `morphir.SDK.dict.dict[K, V]`
- narrow immutable Scala `Map(...)` literals currently map to Morphir `morphir.SDK.dict.fromList`
- narrow immutable Scala `Map.get` currently maps to Morphir `morphir.SDK.dict.get`
- Scala `TupleN` maps to Morphir tuple types and tuple values
- Scala case classes are emitted as Morphir `type alias` records
- narrow case-class methods are emitted as module values with an explicit record receiver input, including the current two-explicit-parameter slice
- narrow Scala `enum` families are emitted as Morphir custom types
- generic case-class fields preserve declared type-parameter order and substitute concrete nested type arguments during field access

## Parity target catalog

The broader parity target is now tracked explicitly in `src/main/scala/morphir/codegen/tasty/ParityCatalog.scala`.

That catalog anchors future work around three target layers:

- Morphir language surfaces such as modules, aliases, records, custom types, literals, functions, branching, tuples, and patterns
- Elm-core-backed SDK surfaces that Morphir maps into its SDK, including basics/bool, numeric families, list, maybe, result, string/char, and function helpers
- Morphir-specific SDK modules such as `Decimal`, `Dict`, `Aggregate`, `Key`, `Rule`, `Validate`, `UUID`, `Instant`, `LocalDate`, `LocalTime`, and `Json.*`

Each catalog entry now also records:

- one or more direct Scala source forms that should map to that Morphir surface
- any important Scala syntactic sugars that should eventually lower to that same Morphir IR

It also records the explicit Elm effect modules that remain out of scope because they do not have a pure Morphir parity target:

- `Debug`
- `Platform`
- `Process`
- `Task`

### Multiple input `.tasty` files

The converter can merge multiple related `.tasty` files into one Morphir distribution.

For inputs from related Scala namespaces such as:

- `a.b`
- `a.b.c`

the converter:

- computes the longest common package prefix as the output Morphir package
- moves the remaining namespace suffix into module paths
- rebases user-defined type and value references to that common package root

Unrelated package roots still fail fast.

## Upcoming feature roadmap

This is the current ordered plan for the next **10** supportable `tastyToMorphirIR` features.

This roadmap is intentionally collection-focused and prioritizes exact-parity, value-producing collection operations before broader or side-effect-oriented traversal shapes.

Keep this section updated as the roadmap changes.

1. **Collection predicates**  
     Add narrow exact-parity support for predicate-style collection queries such as `List.exists` / `List.forall` once the core transform and fold paths are stable.
2. **Collection concatenation**
     Add exact-parity support for safe list-concatenation shapes such as `++` / append once the main transform, fold, and predicate slices are stable.
3. **Collection zipping**
     Add narrow exact-parity support for tuple-producing shapes such as `zip` once fold and pattern-aware collection support are stable enough to consume the resulting tuples.
4. **Collection partitioning**
     Add exact-parity support for narrow partition-style collection splits once predicate and tuple-consuming collection operations are in place.
5. **Pattern-aware flat-mapped ADTs**
     Extend the current explicit-match collection support from tuple-based lambdas to narrow ADT-oriented `map` / `flatMap` shapes over already-supported enums.
6. **Nested collection folds**
     Extend the current fold support to safe nested collection shapes once `Seq` normalization and core list predicate/concatenation slices are stable.
7. **Pattern-aware collection predicates**
     Extend the current explicit-match collection support to safe predicate-style collection queries over tuples and already-supported ADTs.
8. **Map transformation pipelines**
     Extend the current narrow dict surface from literals and simple signatures to safe transformation pipelines once the first lookup/fold operations are stable.
9. **Broader map folds**
     Extend the current narrow dict surface from `get` and literals to exact-parity fold-style map traversals once lookup and pipeline shapes are stable.
10. **Pure foreach-style traversal results**
     Explore whether any additional expression-only traversal syntax beyond the current pure `for`-`yield` slice can be supported without introducing effect-shaped semantics.

## Test suite

The repository has a fixture-driven MUnit test suite under `src/test/scala/morphir/codegen/tasty/`.

The baseline is always the JSON generated from equivalent Elm source using `morphir-elm make -f`.

Main suites:

- `SupportedFunctionEquivalenceTest`
- `CaseClassEquivalenceTest`
- `CaseClassFieldAccessEquivalenceTest`
- `MixedNamespaceMultiTastyTest`

### Test workflow

```bash
sbt test

sbt "testOnly morphir.codegen.tasty.SupportedFunctionEquivalenceTest"
sbt "testOnly morphir.codegen.tasty.CaseClassEquivalenceTest"
sbt "testOnly morphir.codegen.tasty.CaseClassFieldAccessEquivalenceTest"
sbt "testOnly morphir.codegen.tasty.MixedNamespaceMultiTastyTest"
```

Both `test` and `testOnly` automatically:

1. compile the Scala fixtures in `test-fixtures/scala/`
2. generate Elm baseline IR in each fixture project under `test-fixtures/elm/`

## Test fixtures

Fixture inputs live in:

- `test-fixtures/scala/` for Scala sources compiled to `.tasty`
- `test-fixtures/elm/` for Elm projects compiled to `morphir-ir.json`

The tests compare full generated JSON distributions directly, so Scala and Elm namespaces are intentionally aligned.

## Repository structure

- `src/main/scala/morphir/codegen/tasty/TastyToMorphir.scala` - entrypoint and multi-file merge logic
- `src/main/scala/morphir/codegen/tasty/TreeMorph.scala` - package-level conversion to Morphir distributions
- `src/main/scala/morphir/codegen/tasty/TypeDefMorph.scala` - object and case-class module extraction
- `src/main/scala/morphir/codegen/tasty/DefDefMorph.scala` - method definition conversion
- `src/main/scala/morphir/codegen/tasty/ApplyMorph.scala` - function application conversion
- `src/main/scala/morphir/codegen/tasty/IdentMorph.scala` - identifier and FQName conversion
- `src/main/scala/morphir/codegen/tasty/SelectMorph.scala` - selection and field-access conversion
- `src/main/scala/morphir/codegen/tasty/IfMorph.scala` - `if / else` conversion
- `src/main/scala/morphir/codegen/tasty/TreeResolver.scala` - shared type resolution
- `src/main/scala/morphir/codegen/tasty/StandardTypes.scala` - Scala-to-Morphir type mappings
- `src/main/scala/morphir/codegen/tasty/StandardFunctions.scala` - Scala operator/function mappings

## Limitations

- support is intentionally narrow and fail-fast
- generic case classes are supported for the current narrow slice, including multi-parameter data-only records and nested record references
- user-defined ADTs are currently limited to Scala `enum` cases with the current direct-constructor slice up to three constructor arguments
- additional literal widening currently covers `Long`, `Char`, direct `String` literals, and `Float` literal patterns; other scalar literal expansions remain unsupported
- collection support is currently limited to `List[T]` types plus direct `List(...)` literals, nested-list literals, empty-list values (`List()` and `Nil`), direct `List.length`, narrow direct-lambda `List.map` / `List.filter` / `List.flatMap` slices, a narrow `List.foldLeft` slice, a narrow single-case `List.collect` slice, a narrow immutable `Seq[T]` normalization slice for literals, `map`, `flatMap`, and `foldLeft`, and a narrow immutable `Map[K, V]` slice for direct `Map(...)` literals and pass-through signatures
- case-class methods are currently limited to direct methods on the case class plus at most two explicit parameters, with supported bodies staying within the current expression surface
- tuple destructuring is currently limited to narrow flat tuple-match and local-`val` slices up to the current 4-tuple coverage; broader tuple patterns are still unsupported
- many Scala constructs are still unsupported, including broader ADT families, collection operations, and richer tuple or case-method shapes

## Cleanup

```bash
sbt clean
```

This removes normal build output as well as generated Elm fixture IR files used by the test suite.
