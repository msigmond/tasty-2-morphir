# TASTy to Morphir IR

## Overview

This repository converts Scala 3 TASTy files into Morphir IR JSON.

- Input: compiled Scala 3 `.tasty` files
- Output: Morphir IR JSON distribution
- Goal: make business logic captured in Scala available for Morphir-based analysis and visualization

Related projects:

- Morphir JVM: <https://github.com/finos/morphir-jvm>
- Scala 3 TASTy Inspector docs: <https://docs.scala-lang.org/scala3/reference/metaprogramming/tasty-inspect.html>

## Tech Stack

- Scala: `3.3.7`
- Build: `sbt`
- Core parser API: `org.scala-lang:scala3-tasty-inspector`
- IR model: `org.morphir:morphir-ir`
- Logging: `scala-logging` + `slf4j-simple`
- Tests: `munit` (no tests committed yet)

## Repository Structure

- `src/main/scala/morphir/codegen/tasty/TastyToMorphir.scala`: app entrypoint and TASTy inspector integration
- `src/main/scala/morphir/codegen/tasty/TreeMorph.scala`: package-level conversion to Morphir distribution
- `src/main/scala/morphir/codegen/tasty/TypeDefMorph.scala`: module extraction from Scala type definitions
- `src/main/scala/morphir/codegen/tasty/DefDefMorph.scala`: function/method conversion
- `src/main/scala/morphir/codegen/tasty/TreeResolver.scala`: type/value resolution logic
- `src/main/scala/morphir/codegen/tasty/StandardTypes.scala`: Scala-to-Morphir type mapping
- `src/main/scala/morphir/codegen/tasty/StandardFunctions.scala`: Scala operator/function mapping

## Conversion Flow

1. `tastyToMorphirIR` receives output JSON path and one `.tasty` file.
1. `TastyInspector.inspectTastyFiles(...)` loads and inspects the typed AST.
1. AST nodes are transformed into Morphir IR values/types.
1. A `VersionedDistribution` (format version `3`) is encoded and written as JSON.

## Supported Features (Current)

Supported scalar and container types:

- `Boolean`
- `Int`
- `Float`
- `String`
- `BigDecimal`
- `Option[T]` (`Some` / `None`)

Supported expressions and control flow:

- Literals: `Int`, `Float`, `String`
- Arithmetic operators: `+`, `-`, `*`, `/`
- Comparison operators: `<`, `<=`, `>`, `>=`
- Function/subroutine calling
- `if / else` expression mapping

Notes:

- `Int` division is mapped to Morphir `integerDivide`.
- `BigDecimal` arithmetic/comparison maps to Morphir `decimal` functions.

## Current Limitations

- Exactly one input `.tasty` file is currently supported per execution.
- Type and function coverage is intentionally narrow; unsupported constructs fail fast.
- Case class data model extraction is not implemented yet.
- Pattern matching and many advanced Scala constructs are not implemented.

## Build

```bash
sbt compile
```

## Prerequisites

- JDK `21` (project currently builds with Java 21)
- `sbt` `1.12.x` or newer
- Scala 3 compiler (managed by SBT for this project)

## Run

Main entrypoint discovered by SBT:

- `morphir.codegen.tasty.tastyToMorphirIR`

Example:

```bash
sbt "runMain morphir.codegen.tasty.tastyToMorphirIR /tmp/morphir-ir.json /path/to/CompiledFile.tasty"
```

Arguments:

1. Output path for Morphir IR JSON.
1. Path to a compiled Scala 3 `.tasty` file.

## Generating a `.tasty` File (Example)

If you need a quick input file for local testing:

```bash
cat > /tmp/Sample.scala <<'SCALA'
object Sample:
  def add(a: Int, b: Int): Int = a + b
SCALA

mkdir -p /tmp/sample-classes
scalac -d /tmp/sample-classes /tmp/Sample.scala
```

The corresponding TASTy file will be available under:

- `/tmp/sample-classes/Sample.tasty`

## Validate the Generated IR

Install Morphir CLI (if needed):

- <https://github.com/finos/morphir-elm?tab=readme-ov-file#installation>

Then run in the directory where the IR JSON was generated:

```bash
morphir-elm gen
```

If generation succeeds, compare the generated Scala representation with the source logic.
They do not need to be text-identical, but the algorithmic intent should match.

## Development Notes

- This project is exploratory and focused on incremental feature support.
- Error messages are designed to highlight unsupported AST shapes quickly.
- When extending support, update `TreeResolver.scala`, `StandardTypes.scala`, and `StandardFunctions.scala` together to keep mappings consistent.