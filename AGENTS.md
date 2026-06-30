# AGENTS.md

**SolKey** is a fork of [KeY](https://github.com/KeYProject/key) — an interactive theorem prover for Java — extended with `keyext.solidity.core` for formal verification of **Solidity smart contracts**.

## Build Commands

```bash
./gradlew classes                    # Compile
./gradlew test                       # Full test suite (can take hours)
./gradlew testFast                   # Fast/lightweight tests
./gradlew :keyext.solidity.core:test # Module tests
./gradlew :keyext.solidity.core:test --tests "org.key_project.solidity.SomeTest.methodName"
./gradlew spotlessCheck              # Check formatting
./gradlew spotlessApply              # Apply formatting
./gradlew :key.ui:shadowJar          # Build fat JAR
./gradlew :keyext.solidity.core:solidityCli                           # Run CLI on default problem2.key
./gradlew :keyext.solidity.core:solidityCli -PkeyFile=problem1.key    # Run on specific .key file
./gradlew :keyext.solidity.core:solidityCli --args="--no-prove -m 20000 problem1.key"
./run-key.sh problem1.key            # Wrapper script (same as above)
```

Java 21 required. Test max heap: 4GB, max parallel forks: 1.

## Verifying `.key` Problems

**Always use `solidityCli` to run/verify `.key` files.** Gradle's `--args` replaces the default arguments—put the entire CLI argument list in one `--args` value. CLI options: `--no-prove` (load only), `--no-replay`, `-m/--max`, `-t/--timeout`, `-s/--print-stats`, `-v/--verbose`, `--help`.

## File Locations

| Purpose | Location |
|---|---|
| **Problem files (`.key`)** | `keyext.solidity.core/src/test/resources/org/key_project/solidity/examples/` |
| **Proof rules (`.key`)** | `keyext.solidity.core/src/main/resources/org/key_project/solidity/proof/rules/` |

## Module Architecture

| Module                             | Role |
|------------------------------------|---|
| `key.util`                         | Foundation utilities |
| `key.ncore`                        | Language-independent AST/logic (`Term`, `Sort`, `Operator`) |
| `key.ncore.calculus`               | Proof rule infrastructure |
| `key.core`                         | Java-specific logic, parsers, proof management |
| `key.ui`                           | GUI + CLI entry point |
| `keyext.solidity.core`             | **Solidity verification** — main focus |
| `keyext.solidity.examples.taclets` | **Main taclets examples** |

Dependencies: `keyext.*` → `key.core` → `key.ncore` → `key.util`.

## Default Scope

All tasks are assumed to be about **`keyext.solidity.core`** (source: `keyext.solidity.core/src/main/java/org/key_project/solidity/`, tests in `src/test/`). Only look outside if explicitly instructed.

## keyext.solidity.core Architecture

### Pipeline
```
Solidity → ANTLR → SolidityToKeyConverter → AST → TypeResolver → AbstractPO → Strategy + Rules → Proof
```

### Key Packages
- **`program/ast/`** — AST nodes (`ContractDeclaration`, `FunctionDeclaration`, `StateVariableDeclaration`)
- **`program/ast/expressions/`** — Expression nodes; **`statement/`** — Statement nodes
- **`program/ast/abstractions/`** — Type system (`Type`, `KeYSolidityType`, `PrimitiveType`)
- **`parser/`** — `ParsingFacade`, `SolidityToKeyConverter`; **`builder/`** — AST builders
- **`logic/`** — `TermFactory`, `TermBuilder`; **`logic/op/`** — `ProgramVariable`
- **`proof/`** — `Proof`, `Goal`; **`proof/init/`** — proof obligations
- **`rule/`** — Rule interface, meta-constructs (`MetaMul`, `Polynomial`)
- **`speclang/`** — Contracts, specifications
- **`strategy/`** — `Strategy`, `ApplyStrategy`
- **`common/`** — `SolidityInfo` (central type registry)

### Key Classes
- **`SolidityInfo`** — Registry for Solidity types (int8–int256, uint8–uint256, bytes1–bytes32, bool, address). Register new types here.
- **`SolJSONParser`** — Parses `solc --ast-compact-json`. See `docs/solidity-json-documentation.md`.

### ANTLR
Grammars in `keyext.solidity.core/src/main/antlr/`: `Solidity.g4`, `KeYSolidityDLLexer.g4`/`KeYSolidityDLParser.g4`. Generated: `build/generated-src/antlr/main/`.

## Code Style

Spotless enforces formatting (`scripts/tools/checkstyle/keyCodeStyle.xml`). Run `./gradlew spotlessApply` before committing. Fields `@NonNull` by default.

## Documentation (`docs/`)

Read the relevant doc before working on taclets — each is a compact, agent-facing reference:

| Doc | Read when |
|---|---|
| `key-taclets.md` | **Start here** to author a taclet — rule shape, schema-variable choices, varconds, verification commands. |
| `taclets-implementation.md` | Checking what is already implemented (rule families + example files) and how to run/verify it. |
| `taclet-ideas.md` | Picking the next unimplemented construct — the backlog, ordered simple → complex. |
| `storage.md` | Working on storage rules — calculus spec (schema vars, three-step strategy, statement→rule table, worked traces). |
| `memory.md` | Working on memory rules — calculus spec (identity heap, aliasing, delete, cross-domain `copySt`/`copyMem`). |
| `require-assert.md` | Touching `require` / `assert` rules (box vs. diamond false-branch behavior). |
| `solidity-json-documentation.md` | Parsing `solc --ast-compact-json` (consumed by `SolJSONParser`). |

Program rules live in `keyext.solidity.core/src/main/resources/org/key_project/solidity/proof/rules/solidityProgramRules.key` (loaded automatically via `standardSolidityRules.key`). Add new example problems under `keyext.solidity.examples/taclets`, and after changing a feature update `docs/taclets-implementation.md` (implemented) or `docs/taclet-ideas.md` (backlog).

**When planning a new taclet:** always begin the plan with a plain-English explanation of what the taclet does — state the precondition (what must be true before the rule fires), the transformation (what sequent change it performs), and the postcondition (what is true after). Write this before any KeY syntax.

## Testing

Run `./gradlew :keyext.solidity.core:test` after refactoring. Prefer modifying existing test classes over creating new ones.

## Key KeY Concepts
- **`Term`** — immutable, has `Operator`, subterms, `Sort`
- **`Services`** — main DI object; provides namespaces, type info
- **`ImmutableArray`/`ImmutableList`** — prefer over mutable collections
- **Sequent** — proof state (antecedent + succedent); rules transform sequents
