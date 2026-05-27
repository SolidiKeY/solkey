# AGENTS.md

**SolKey** is a fork of [KeY](https://github.com/KeYProject/key) — an interactive theorem prover for Java — extended with `keyext.solidity.core`, a module for formal verification of **Solidity smart contracts**. The goal is to verify functional correctness of Solidity contracts against formal specifications.

## Build Commands

```bash
# Compile
./gradlew classes

# Run full test suite (can take hours)
./gradlew test

# Run fast/lightweight tests
./gradlew testFast

# Run tests for a specific module
./gradlew :keyext.solidity.core:test

# Run a single test class or method
./gradlew :keyext.solidity.core:test --tests "org.key_project.solidity.SomeTest.methodName"

# Check code formatting
./gradlew spotlessCheck

# Apply code formatting
./gradlew spotlessApply

# Build fat JAR
./gradlew :key.ui:shadowJar

# Generate ANTLR parsers (done automatically during build)
./gradlew generateGrammarSource
```

Java 21 is required. Test max heap is 4GB, max parallel forks is 1.

## Module Architecture

The project uses a layered, modular Gradle structure:

| Module | Role |
|---|---|
| `key.util` | Foundation utilities, bean/lookup system |
| `key.ncore` | Language-independent AST/logic abstractions (`Term`, `Sort`, `Operator`) — **no Java-specific code allowed** |
| `key.ncore.calculus` | Proof rule infrastructure (no proof search) |
| `key.core` | Java-specific logic, parsers, proof management, strategy engine |
| `key.ui` | GUI + CLI entry point (`de.uka.ilkd.key.core.Main`) |
| `keyext.solidity.core` | **Solidity verification** — the main focus of this fork |

Extensions (`keyext.*`) depend on `key.core`; `key.core` depends on `key.ncore`; `key.ncore` depends on `key.util`.

## Default Scope

All questions and tasks are assumed to be about **`keyext.solidity.core`** (source: `keyext.solidity.core/src/main/java/org/key_project/solidity/`) and its tests (`org.key_project.solidity` in `keyext.solidity.core/src/test/`). Only look outside these directories if explicitly instructed.

## keyext.solidity.core Architecture

This is the primary module being developed. Source root: `keyext.solidity.core/src/main/java/org/key_project/solidity/`

### Verification Pipeline

```
Solidity source
    ↓ ANTLR (Solidity.g4)
Parse tree
    ↓ SolidityToKeyConverter (ANTLR visitor)
AST (ContractDeclaration, FunctionDeclaration, ...)
    ↓ TypeResolver
Type-resolved AST
    ↓ AbstractPO / FunctionalOperationContractPO
Proof obligation (sequent)
    ↓ Strategy + Rules
Proof tree (closed = verified)
```

### Key Packages

- **`program/ast/`** — Solidity AST nodes. `ContractDeclaration`, `FunctionDeclaration`, `StateVariableDeclaration` are the main declaration types. `SolidityProgramElement` is the root interface with visitor support.
- **`program/ast/expressions/`** — All expression nodes (binary/unary ops, function calls, literals).
- **`program/ast/statement/`** — Statement nodes (`Block`, `ForStatement`, `WhileStatement`, etc.).
- **`program/ast/abstractions/`** — Type system: `Type`, `KeYSolidityType`, `PrimitiveType`, `TupleType`, parametric array/mapping sorts.
- **`parser/`** — `ParsingFacade` (entry point), `SolidityToKeyConverter` (ANTLR visitor converting parse tree to AST), `KeYIO`.
- **`parser/builder/`** — `ExpressionBuilder`, `DeclarationBuilder` for constructing AST nodes.
- **`logic/`** — `TermFactory`, `TermBuilder`, `NamespaceSet`, `SortImpl` — bridges to KeY's logic layer.
- **`logic/op/`** — `ProgramVariable` (extends `AbstractSortedOperator`, implements `Expression` and `UpdateableOperator`).
- **`proof/`** — `Proof`, `Goal`, event listeners. `proof/init/` contains proof obligations (`AbstractPO`, `FunctionalOperationContractPO`).
- **`rule/`** — Rule interface and meta-constructs (`MetaMul`, `MetaBinaryAnd`, `Polynomial`, etc.).
- **`speclang/`** — `Contract`, `OperationContract`, `FunctionalOperationContract`, `LoopSpecification`.
- **`strategy/`** — `Strategy`, `ApplyStrategy`, goal choosers.
- **`common/`** — `SolidityInfo` (central registry for types, functions, contracts; includes all Solidity primitive types).

### ANTLR Grammars

Located in `keyext.solidity.core/src/main/antlr/`:
- `Solidity.g4` — Main Solidity language grammar
- `KeYSolidityDLLexer.g4` / `KeYSolidityDLParser.g4` — KeY Dynamic Logic extensions for Solidity
- `SchemaLexer.g4` — Schema variable lexer

Generated parser code goes to `build/generated-src/antlr/main/`.

### `SolidityInfo`

Central singleton-like registry accessed via `Services`. Contains all registered Solidity primitive types (int8–int256, uint8–uint256, bytes1–bytes32, bool, address) and their KeY sort mappings. When adding new types or built-ins, register them here.

### `SolJSONParser`

Parses the JSON AST produced by the `solc` compiler (`--ast-compact-json`) into the internal Solidity AST. When refactoring or extending `SolJSONParser`, refer to **`solidity-json-documentation.md`** (at the repository root) to understand the structure of `solc`'s JSON output — what node types exist, what fields each node has, and what the type descriptor format looks like.

## Code Style

Formatting is enforced by **Spotless** using the Eclipse style defined in `scripts/tools/checkstyle/keyCodeStyle.xml`. Run `./gradlew spotlessApply` before committing. Nullness is checked via the EISOP Checker Framework — fields are `@NonNull` by default; use `@Nullable` explicitly.

## Testing After Refactoring

After every refactor related to Solidity code, run the Solidity module tests to verify the changes pass:

```bash
./gradlew :keyext.solidity.core:test
```

This ensures that modifications to the Solidity verification pipeline do not break existing functionality.

**Test Creation Policy:** When adding tests, prefer modifying existing test classes over creating new ones. Only create new test files when it is not reasonably possible to add tests to existing files, or when explicitly instructed to do so.

## Key Inherited KeY Concepts

- **`Term`** (`key.ncore`) — immutable, has an `Operator`, subterms, bound variables, and a `Sort`.
- **`Services`** — the main dependency injection object passed through most of the codebase; provides access to namespaces, type info, and registries.
- **`ImmutableArray` / `ImmutableList`** — used pervasively; prefer these over mutable collections in AST nodes.
- **Sequent** — a proof state consisting of antecedent and succedent formulas; proof rules transform sequents.
