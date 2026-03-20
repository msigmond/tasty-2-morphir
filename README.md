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
- `List[T]` for the current narrow empty-list, direct `List(...)` literal, nested-list literal, direct `length`, direct single-lambda `map` / `filter` / `flatMap`, and narrow `foldLeft` slices
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
- Scala `TupleN` maps to Morphir tuple types and tuple values
- Scala case classes are emitted as Morphir `type alias` records
- narrow case-class methods are emitted as module values with an explicit record receiver input, including the current two-explicit-parameter slice
- narrow Scala `enum` families are emitted as Morphir custom types
- generic case-class fields preserve declared type-parameter order and substitute concrete nested type arguments during field access

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

1. **Pattern-aware list `map` / `flatMap`**  
   Support element destructuring and explicit `match` expressions inside list transforms, starting with tuples and existing ADT shapes.
2. **List `collect`**  
   Add a narrow `collect` slice by lowering supported partial-function patterns only where exact Elm parity remains stable.
3. **`Seq[T]` normalization**  
   Normalize the most common immutable `Seq` shapes onto the proven list path, starting with literals plus `map` / `flatMap` / `foldLeft`.
4. **`Map[K, V]` types and literals**  
   Add narrow `Map` type/literal support only for key/value shapes that can be matched exactly against Elm-generated Morphir baselines.
5. **Core `Map` operations**  
   Add the safest exact-parity `Map` operations next, likely starting with lookup and fold-oriented shapes before broader transforms.
6. **`foreach` and `for`-style traversal**  
    Explore constrained support for pure traversal shapes only after the value-returning collection operators above are stable and well baselined.
7. **Collection predicates**  
    Add narrow exact-parity support for predicate-style collection queries such as `List.exists` / `List.forall` once the core transform and fold paths are stable.
8. **Collection concatenation**
    Add exact-parity support for safe list-concatenation shapes such as `++` / append once the main transform, fold, and predicate slices are stable.
9. **Collection zipping**
    Add narrow exact-parity support for tuple-producing shapes such as `zip` once fold and pattern-aware collection support are stable enough to consume the resulting tuples.
10. **Collection partitioning**
    Add exact-parity support for narrow partition-style collection splits once predicate and tuple-consuming collection operations are in place.

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
- collection support is currently limited to `List[T]` types plus direct `List(...)` literals, nested-list literals, empty-list values (`List()` and `Nil`), direct `List.length`, narrow direct-lambda `List.map` / `List.filter` / `List.flatMap` slices, and a narrow `List.foldLeft` slice
- case-class methods are currently limited to direct methods on the case class plus at most two explicit parameters, with supported bodies staying within the current expression surface
- tuple destructuring is currently limited to narrow flat tuple-match and local-`val` slices up to the current 4-tuple coverage; broader tuple patterns are still unsupported
- many Scala constructs are still unsupported, including broader ADT families, collection operations, and richer tuple or case-method shapes

## Cleanup

```bash
sbt clean
```

This removes normal build output as well as generated Elm fixture IR files used by the test suite.
