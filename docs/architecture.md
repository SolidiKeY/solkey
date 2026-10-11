# Architecture

Read this when a task needs the module layout, the package map of `keyext.solidity.core`, or
the ANTLR grammar setup.

## Modules

| Module | Role |
|---|---|
| `key.util` | Foundation utilities |
| `key.ncore` | Language-independent AST/logic (`Term`, `Sort`, `Operator`) |
| `key.ncore.calculus` | Proof rule infrastructure |
| `key.core` | Java-specific logic, parsers, proof management |
| `key.ui` | GUI + CLI entry point |
| `keyext.solidity.core` | **Solidity verification** — main focus |
| `keyext.solidity.gui` | **KeYther**, the standalone Swing GUI for the Solidity prover |
| `keyext.solidity.idea` | IntelliJ IDEA plugin — ▶ gutter icon on public functions and contracts; left click opens KeYther, right click also offers the headless prover and solc+EVM. Standalone Gradle build, deliberately **not** in `settings.gradle`. See `idea-setup.md` |
| `keyext.solidity.web` | The prover compiled to WebAssembly by GraalVM Web Image, as a static page on GitHub Pages. Included only when `GRAALVM_HOME` has Web Image. See `web.md` |
| `keyext.solidity.examples` | **Main taclet examples** (`TestSuite.sol`) |

Dependencies: `keyext.*` → `key.core` → `key.ncore` → `key.util`.

## keyext.solidity.core pipeline

```
Solidity → ANTLR → SolidityToKeyConverter → AST → TypeResolver → AbstractPO → Strategy + Rules → Proof
```

- **`program/ast/`** — AST nodes; **`expressions/`**, **`statement/`** beneath it;
  **`abstractions/`** — type system (`Type`, `KeYSolidityType`, `PrimitiveType`)
- **`parser/`** — `ParsingFacade`, `SolidityToKeyConverter`; **`builder/`** — AST builders
- **`logic/`** — `TermFactory`, `TermBuilder`; **`logic/op/`** — `ProgramVariable`
- **`proof/`** — `Proof`, `Goal`; **`proof/init/`** — proof obligations
- **`rule/`** — Rule interface, meta-constructs; **`speclang/`** — contracts and specifications
- **`keyfile/`** — typed AST of a generated `.key` problem (`Key` builds it, `KeyPrinter` is the
  only code that writes `.key` syntax); `SolidityProblemSynthesizer` and `SpecCompiler` build it
- **`strategy/`** — `Strategy`, `ApplyStrategy`
- **`common/`** — `SolidityInfo`, the registry for Solidity types (int8–int256, uint8–uint256,
  bytes1–bytes32, bool, address). Register new types here.
- **`runtime/`** — the in-process Besu EVM. `SolidityRuntimeCheck` compiles a contract, deploys it
  and calls its functions, reporting `Panic(0x01)` as a failed `assert`; drives `--solc` and
  `SolidityRuntimeExecutionTest`
- **`program/parser/SolJSONParser`** — parses solc's compact JSON AST; `SolcWrapper` and
  `WasmSolcCompiler` produce it by running solc's WebAssembly build on the JVM, with no
  external compiler. See `solc-ast.md`

## ANTLR grammars

Grammars live in `keyext.solidity.core/src/main/antlr/`, generated sources in
`build/generated-src/antlr/main/`. One lexer/parser pair reads both logic and Solidity: a
modality opener pushes the lexer's `SOL` mode, whose tokens `KeYSolidityDLLexer` imports from
`SolidityLexer.g4`, and `KeYSolidityDLParser` imports the Solidity rules from
`SolidityRules.g4`, so a modality body is a subtree that `ExpressionBuilder` hands to
`SolidityToKeyConverter`. `SolidityLexer.g4` needs its one default-mode fragment: ANTLR rejects
a lexer grammar that starts with `mode`. Syntax errors fail the load (`ThrowingErrorListener`).
