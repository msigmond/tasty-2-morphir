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

## Current conversion behavior

### Supported types

- `Boolean`
- `Int`
- `Float`
- `String`
- `BigDecimal`
- `Option[T]`
- top-level, non-generic Scala `case class` data fields

### Supported expressions

- literals
- arithmetic operators: `+`, `-`, `*`, `/`
- comparison operators: `<`, `<=`, `>`, `>=`
- function application
- `if / else`
- case-class field access

### Mapping notes

- `Int /` maps to Morphir `integerDivide`
- `BigDecimal` arithmetic and comparison map to Morphir decimal SDK functions
- Scala `BigDecimal /` maps to `morphir.SDK.decimal.div.unsafe`
- Scala case classes are emitted as Morphir `type alias` records

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
- generic case classes are not supported
- methods on case classes are not part of case-class data conversion
- many Scala constructs are still unsupported, including broader pattern-matching coverage

## Cleanup

```bash
sbt clean
```

This removes normal build output as well as generated Elm fixture IR files used by the test suite.
