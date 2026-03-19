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
- `Float`
- `Double` (mapped to Morphir `float`)
- `String`
- `BigDecimal`
- `Option[T]`
- narrow singleton Scala `enum` custom types
- Scala tuples, currently the direct `(A, B)` value/type slice
- Scala `case class` data fields, including generic multi-parameter and nested record references

### Supported expressions

- literals, including `Boolean`
- arithmetic operators: `+`, `-`, `*`, `/`
- boolean operators: `&&`, `||`
- comparison operators: `<`, `<=`, `>`, `>=`
- function application
- `if / else`
- pattern matching for `Option` constructors and supported scalar literal patterns
- constructor references and direct constructor pattern matches for the current singleton-`enum` slice
- tuple literals and tuple-typed pass-through values
- local `val` bindings and block expressions
- case-class field access, including nested record access

### Mapping notes

- `Int /` maps to Morphir `integerDivide`
- `BigDecimal` arithmetic and comparison map to Morphir decimal SDK functions
- Scala `BigDecimal /` maps to `morphir.SDK.decimal.div.unsafe`
- Scala `Double` maps to Morphir `float`
- Scala `TupleN` maps to Morphir tuple types and tuple values
- Scala case classes are emitted as Morphir `type alias` records
- narrow singleton Scala `enum` families are emitted as Morphir custom types
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

This is the current ordered plan for the next **5** supportable `tastyToMorphirIR` features.

Keep this section updated as the roadmap changes.

1. **More literal widening**  
   Continue the literal-expansion path beyond `Double`, starting with candidates like `Long` and `Char` where the Morphir target type is clear.
2. **Broad collections support**  
   Add a narrow first slice of collection support, starting with list-oriented operations whose Elm and Morphir shapes are already well understood.
3. **Case-class methods**  
   Add a narrow slice of methods defined on case classes when they can be lowered cleanly without breaking the current data-alias model.
4. **Tuple destructuring**  
   Extend tuple support from direct tuple values and types into tuple destructuring and tuple-pattern coverage where Elm-baseline parity is stable.
5. **Richer user-defined ADTs**  
   Extend the current singleton-`enum` slice toward constructor arguments and broader sealed families once exact Elm parity is established for those shapes.

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
- user-defined ADTs are currently limited to singleton Scala `enum` cases with direct constructor references and direct constructor matches
- methods on case classes are not part of case-class data conversion
- many Scala constructs are still unsupported, including tuple destructuring, broader ADTs with constructor arguments, and collection support

## Cleanup

```bash
sbt clean
```

This removes normal build output as well as generated Elm fixture IR files used by the test suite.
