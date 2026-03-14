# Copilot Instructions

## Project Overview

This project converts Scala 3 TASTy (Typed Abstract Syntax Tree) files into [Morphir IR](https://github.com/finos/morphir-jvm) JSON (distribution format v3). It reads compiled `.tasty` binary files via the `scala3-tasty-inspector` API and emits a `VersionedDistribution` JSON suitable for Morphir-based analysis.

## Build & Run

**Requirements**: SBT 1.12.5, Scala 3.3.7, JDK 21, `morphir-elm` CLI (installed to `~/.local/bin/morphir-elm`)

```bash
sbt compile
sbt test                                                          # run all tests (munit)
sbt "testOnly morphir.codegen.tasty.SupportedFunctionEquivalenceTest"   # run a single test class
sbt "testOnly -- -k add"                                         # run tests matching a pattern

# Run the converter
sbt "runMain morphir.codegen.tasty.tastyToMorphirIR /tmp/output.json /path/to/File.tasty"
```

`sbt test` automatically:
1. Compiles `test-fixtures/scala/` (the `scalaAlgorithmFixtures` sub-project) to produce one `.tasty` file per Scala fixture object.
2. Runs `morphir-elm make -f` in each per-case Elm fixture directory under `test-fixtures/elm/` to regenerate per-case `morphir-ir.json` files.

> **Note**: The system `scalac` may be a newer Scala version than 3.3.7.  Always compile test fixture Scala files via SBT (`scalaAlgorithmFixtures/compile`) so the correct TASTy version is used.

## Test Infrastructure

Tests live in `src/test/scala/morphir/codegen/tasty/`. The test strategy is **cross-language equivalence**: each algorithm has its own Elm fixture project and its own Scala `.tasty` file, and the full generated JSON distributions are compared directly.

### Fixture layout

```
test-fixtures/
  elm/
    add/
      morphir.json
      src/Arithmetic/Add.elm
      morphir-ir.json
    subtract/
      ...
    multiply/
      ...
    is-positive/
      ...
    clamp/
      ...
    integer-divide/
      ...
    decimal-add/
      ...
    decimal-subtract/
      ...
    decimal-multiply/
      ...
    decimal-lt/
      ...
    decimal-lte/
      ...
    decimal-gt/
      ...
    decimal-gte/
      ...
    maybe-just/
      ...
    maybe-nothing/
      ...
    maybe-positive/
      ...
  scala/
    src/main/scala/arithmetic/
      Add.scala
      Subtract.scala
      Multiply.scala
      IsPositive.scala
      Clamp.scala
      IntegerDivide.scala
      DecimalAdd.scala
      DecimalSubtract.scala
      DecimalMultiply.scala
      DecimalDivide.scala
      DecimalLt.scala
      DecimalLte.scala
      DecimalGt.scala
      DecimalGte.scala
      MaybeJust.scala
      MaybeNothing.scala
      MaybePositive.scala
```

The Scala fixtures are a separate SBT sub-project (`scalaAlgorithmFixtures`) compiled with Scala 3.3.7, so `.tasty` files are version-compatible with the inspector.

### Adding a new test algorithm

1. Create a new Elm fixture directory under `test-fixtures/elm/<case>/` with its own `morphir.json` and one module file under `src/Arithmetic/<Case>.elm`.
2. Add the matching Scala fixture object under `test-fixtures/scala/src/main/scala/arithmetic/<Case>.scala`.
3. Add a new entry to the `equivalenceCases` table in `SupportedFunctionEquivalenceTest.scala` when the case is a direct Elm/Scala equivalence case.
4. Keep namespaces aligned: Elm should use package `Arithmetic` and module `Arithmetic.<Case>`, while Scala should use package `arithmetic` and object `<Case>`.

### Comparison strategy

The test suite compares the full parsed JSON distributions directly. This works because each fixture pair is one-to-one and the namespaces are intentionally synchronized:

- Elm package `Arithmetic` maps to Morphir package `["arithmetic"]`
- Elm module `Arithmetic.Add` maps to Morphir module `["add"]`
- Scala package `arithmetic` + object `Add` produces the same package/module path

`SupportedFunctionEquivalenceTest.scala` compares most supported mappings through direct full-JSON Elm/Scala equivalence.

One intentional exception exists today: Scala `BigDecimal /` is mapped to `morphir.SDK.decimal.div.unsafe`, while the Morphir Elm SDK exposes `Decimal.div : Decimal -> Decimal -> Maybe Decimal`. That case is covered with a targeted Scala-side mapping assertion instead of a direct Elm equivalence test.

The conversion pipeline is:

1. **Entry point** (`TastyToMorphir.scala`): `tastyToMorphirIR(outputPath, tastyPath)` calls `TastyInspector.inspectTastyFiles`, then writes the resulting JSON.
2. **Tree traversal** (`TreeMorph.scala`): Handles `PackageDef`, produces a `VersionedDistribution`.
3. **Module extraction** (`TypeDefMorph.scala`): Converts type definitions into Morphir modules.
4. **Function conversion** (`DefDefMorph.scala`): Converts method definitions into Morphir values.
5. **Expression converters** — each handles one TASTy node type:
   - `ApplyMorph` — function application
   - `IdentMorph` — identifiers / variable references
   - `LiteralMorph` — literal values
   - `SelectMorph` — field/method selection
   - `IfMorph` — if/else expressions
6. **Shared infrastructure**:
   - `TreeResolver.scala` — trait mixed into converters; resolves types and qualified names
   - `StandardTypes.scala` — maps Scala built-in types to Morphir types
   - `StandardFunctions.scala` — maps Scala operators/functions to Morphir equivalents
   - `MorphUtils.scala` — extension methods that attach `.toValue()`, `.toPath()`, `.toFQN()` etc. to TASTy tree types

All conversion methods return `Try[T]`. The project is intentionally fail-fast: unsupported AST shapes throw with descriptive messages rather than being silently skipped.

## Key Conventions

**Naming**
- Files named `*Morph.scala` each handle one TASTy node type (e.g., `IdentMorph`, `SelectMorph`).
- Conversion methods are consistently named: `toValue`, `toModule`, `toPath`, `toFQN`.
- Objects are singletons (not classes) — converters are called as `IdentMorph.toPath(id)`.

**Error handling**
- All conversions return `Try[T]`; compose with `for`-comprehensions or `.flatMap`.
- Use `Failure(new NotImplementedError(...))` for unsupported TASTy shapes — do not silently return a default value.

**Extension methods (MorphUtils)**
- `MorphUtils.scala` provides extension methods on TASTy tree types so callers can write `id.toPath` instead of `IdentMorph.toPath(id)`.
- Add new extension methods here when adding a converter.

**Implicit context threading**
- Converter methods take `(using Quotes)(using Contexts.Context)` — always thread these through.
- `TreeResolver` trait provides shared resolution logic; mix it in rather than duplicating logic.

**Imports pattern**
```scala
import dotty.tools.dotc.core.Contexts
import morphir.codegen.tasty.MorphUtils.*
import morphir.ir.{Type as MorphType, *}
import scala.quoted.Quotes
import scala.util.{Failure, Success, Try}
```
