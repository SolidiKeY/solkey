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
./gradlew -DENABLE_NULLNESS=true :keyext.solidity.core:compileTestJava   # Nullness checker (~1 min)
./gradlew :key.ui:shadowJar          # Build fat JAR
./gradlew :keyext.solidity.core:solidityCli                           # Run CLI on default problem2.key
./gradlew :keyext.solidity.core:solidityCli -PkeyFile=problem1.key    # Run on specific .key file
./gradlew :keyext.solidity.core:solidityCli --args="--no-prove -m 20000 problem1.key"
./run-key.sh problem1.key                                 # Wrapper script (same as above)
./run-key.sh keyext.solidity.examples/TestSuite.sol       # Every function of a .sol
./run-key.sh keyext.solidity.examples/TestSuite.sol testSimpleAssert   # One function
```

Java 21 required. Test max heap: 4GB, max parallel forks: 1.

## Verifying Problems

**Always use `solidityCli` to run/verify a problem.** Gradle's `--args` replaces the default arguments—put the entire CLI argument list in one `--args` value. CLI options: `--no-prove` (load only), `--no-replay`, `-m/--max`, `-t/--timeout`, `-s/--print-stats`, `-v/--verbose`, `-f/--function`, `-c/--contract`, `--help`.

`solidityCli` accepts a `.sol` file directly — no `.key` needed. The loader synthesizes one obligation per function (`\<{ f(x, y)@C; }\>(true)`, with one unconstrained program variable per parameter), so the specification lives in the Solidity body as `assert` — a parameterized function is box-tagged and assumes its argument values with `require`. Without `--function` every function is proved and a `N/M closed` summary is printed.

## File Locations

| Purpose | Location |
|---|---|
| **Taclet examples (`.sol`)** | `keyext.solidity.examples/TestSuite.sol` — see `keyext.solidity.examples/README.md` |
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
| `keyext.solidity.examples`         | **Main taclets examples** (`TestSuite.sol`) |

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

## CI Gates — run before committing

CI enforces three gates. **Two of them are off in a normal build**, so `./gradlew classes` and
`./gradlew test` can both pass while CI fails — this is why the nullness gate historically broke
on nearly every push.

| Gate | CI job | Local command | Cost |
|---|---|---|---|
| Formatting | `CodeQuality / formatting` | `./gradlew spotlessApply` | seconds |
| Nullness | `CodeQuality / checkerFramework` | `./gradlew -DENABLE_NULLNESS=true :keyext.solidity.core:compileTestJava` | ~1 min |
| Module tests | `Solidity / test` | `./gradlew :keyext.solidity.core:test` | ~25 min |

The nullness checker only activates under `-DENABLE_NULLNESS=true`, and it is scoped to
`org.key_project.solidity.program.ast` (`-AonlyDefs` in `keyext.solidity.core/build.gradle`).
**Only edits under `program/ast/` can trip it** — skip it for work confined to `strategy/`,
`rule/`, `speclang/`, `parser/`, etc. The CI job runs `compileTestJava` at the root; the
module-scoped command above is the fast equivalent for this fork's code.

**Nullness idiom.** Nearly every failure is a `@Nullable` value reaching a `@NonNull` parameter or
return. The checker does not refine a nullable field across two calls, so hoist it into a local
first:

```java
// rejected — getTypeReference() is called twice, so the guard does not refine the second read
if (fd.getTypeReference().getReferencedType() != null) {
    return fd.getTypeReference().getReferencedType();
}

// accepted
Type referencedType = fd.getTypeReference().getReferencedType();
if (referencedType != null) {
    return referencedType;
}
```

`MemoryReferenceTypes.asMemoryReferenceType` is the reference example: hoist to a local, return
the unchanged input when null. Prefer that over widening a parameter or field to `@Nullable`.

## Code Style

Spotless enforces formatting (`scripts/tools/checkstyle/keyCodeStyle.xml`). Run `./gradlew spotlessApply` before committing. Fields `@NonNull` by default.

**Do not add comments to the code.** Write self-explanatory code instead; leave existing comments untouched unless the change makes them wrong.

## Documentation (`docs/`)

Read the relevant doc before working on taclets — each is a compact, agent-facing reference:

| Doc | Read when |
|---|---|
| `key-taclets.md` | **Start here** to author a taclet — rule shape, schema-variable choices, varconds, verification commands. |
| `taclets-implementation.md` | Checking what is already implemented (rule families + example files) and how to run/verify it. |
| `taclet-ideas.md` | Picking the next unimplemented construct — the backlog, ordered simple → complex. |
| `storage.md` | Working on storage rules — calculus spec (schema vars, three-step strategy, statement→rule table, worked traces). |
| `memory.md` | Working on memory rules — calculus spec (identity heap, aliasing, delete, cross-domain `copySt`/`copyMem`). |
| `net.md` | Implementing the payment/ledger model (`net`, `msg.sender`/`msg.value`, `transfer`, contract invariants, proof obligations) — ordered plan from the ISoLA 2020 paper. |
| `require-assert.md` | Touching `require` / `assert` rules (box vs. diamond false-branch behavior). |
| `solidity-json-documentation.md` | Parsing `solc --ast-compact-json` (consumed by `SolJSONParser`). |
| `forked-key-core.md` | Editing code forked from `key.core` — which files are near-verbatim copies and why they must not be restyled. |

Program rules live in `keyext.solidity.core/src/main/resources/org/key_project/solidity/proof/rules/solidityProgramRules.key` (loaded automatically via `standardSolidityRules.key`). Add new taclet examples as functions of `keyext.solidity.examples/TestSuite.sol`, and scenario/invariant examples as contracts plus `.key` proof obligations in `keyext.solidity.examples/net/` — conventions for both: `keyext.solidity.examples/README.md`. After changing a feature update `docs/taclets-implementation.md` (implemented) or `docs/taclet-ideas.md` (backlog).

**When planning a new taclet:** always begin the plan with a plain-English explanation of what the taclet does — state the precondition (what must be true before the rule fires), the transformation (what sequent change it performs), and the postcondition (what is true after). Write this before any KeY syntax.

**`\addprogvars` — common mistake:** do NOT put `\addprogvars(pv)` on a capture/unfold taclet whose `\replacewith` re-emits a declaration for the fresh variable (`s#pvType s#pv = s#nse; ...`) — that declaration is consumed later by `memoryLocalDeclInitDrop`, which registers the variable itself, so the early `\addprogvars` is redundant. `\addprogvars` belongs only on the rules that consume the declaration statement itself (`memoryLocalDeclInitDrop`, `storageLocalDeclSkip`, `valueDeclSkip`, `memoryReferenceDeclFreshAlloc`, the `localDecl*crement` family).

## Testing

Run `./gradlew :keyext.solidity.core:test` after refactoring (~25 min; see **CI Gates** above for
the full pre-commit set). Prefer modifying existing test classes over creating new ones.

Examples that do not close yet are skipped via `KNOWN_OPEN` in `SolcSemanticsExamplesTest` and
listed under "Known open" in `keyext.solidity.examples/solc/README.md`. Keep the two in sync: a
suite that is green except for tracked entries is what makes a red run mean new breakage.

## Key KeY Concepts
- **`Term`** — immutable, has `Operator`, subterms, `Sort`
- **`Services`** — main DI object; provides namespaces, type info
- **`ImmutableArray`/`ImmutableList`** — prefer over mutable collections
- **Sequent** — proof state (antecedent + succedent); rules transform sequents
