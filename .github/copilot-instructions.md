# Copilot Instructions

## Project Overview

This project converts Scala 3 TASTy (Typed Abstract Syntax Tree) files into [Morphir IR](https://github.com/finos/morphir-jvm) JSON (distribution format v3). It reads compiled `.tasty` binary files via the `scala3-tasty-inspector` API and emits a `VersionedDistribution` JSON suitable for Morphir-based analysis.

## Build & Run

**Requirements**: SBT 1.12.5, Scala 3.3.7, JDK 21

```bash
sbt compile
sbt test                                        # run all tests (munit)
sbt "testOnly morphir.codegen.tasty.SomeTest"  # run a single test class
sbt "testOnly -- -k <pattern>"                 # run tests matching a pattern

# Run the converter
sbt "runMain morphir.codegen.tasty.tastyToMorphirIR /tmp/output.json /path/to/File.tasty"
```

To generate a `.tasty` file for testing:
```bash
echo 'object Sample:\n  def add(a: Int, b: Int): Int = a + b' > /tmp/Sample.scala
mkdir -p /tmp/sample-classes && scalac -d /tmp/sample-classes /tmp/Sample.scala
# Use /tmp/sample-classes/Sample.tasty as input
```

## Architecture

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
