# AGENTS.md

**SolKey** is a fork of [KeY](https://github.com/KeYProject/key) — an interactive theorem prover
for Java — extended with `keyext.solidity.core` for formal verification of **Solidity smart
contracts**. Java 21 required.

## Working efficiently

These keep a task to few tool calls. Cost is dominated by round-trips, not by output size.

- **Never read a rules `.key` file whole** (`solidityProgramRules.key` is 4 800 lines). Use
  `scripts/taclet.sh NAME` for one taclet, `--index` for the section banners, `--list` for every
  rule name with its `file:line`.
- **Prove with `./run-key.sh`**, not with Gradle: it runs the fat jar, so there is no Gradle
  banner and no startup cost, and `--open-goals` prints the remaining sequents of an unclosed
  proof in the same run that produced it.
- **Read a long doc by section** (`grep -n '^#' doc`, then `sed -n`), not whole.
- **Batch independent shell commands** into one call.
- Read a doc from the table below only when the task needs it.

## Build and run

```bash
./gradlew classes                    # Compile
./gradlew :keyext.solidity.core:test # Module tests: unit + TestSuite.sol suites (~30 s)
./gradlew :keyext.solidity.core:test --tests "org.key_project.solidity.SomeTest.methodName"
./gradlew -DENABLE_NULLNESS=true ciGates   # All three CI gates (see docs/ci.md)
./gradlew spotlessApply              # Apply formatting

./run-key.sh FILE.sol                          # prove every function
./run-key.sh FILE.sol fnName                   # prove one function
./run-key.sh FILE.sol -f fnName --open-goals   # ... and show why it did not close
./run-key.sh FILE.key -m 20000 --no-prove      # a .key problem; any CLI option works
./run-key.sh FILE.sol -O transferSemantics:withCallback   # prove under a non-default taclet option
./run-key.sh FILE.sol --solc                   # compile, then run on an EVM: reports a failing assert
./run-key.sh FILE.sol --solc -f fnName         # ... for one function
./run-key.sh FILE.sol -f fnName --print-problem # print the generated .key problem instead
./run-key.sh --help                            # every CLI option

scripts/taclet.sh requireSimple      # print one taclet with its file:line
scripts/taclet.sh --index            # the rule-section banners
scripts/taclet.sh --list             # every rule name with its file:line
scripts/benchmark.sh                 # published contracts as published: N/M closed each

./gradlew :keyext.solidity.gui:solidityGui     # KeYther, the Swing GUI
./gradlew :key.ui:shadowJar                    # fat JAR
```

`run-key.sh` rebuilds `keyext.solidity.core-exe.jar` only when the sources are newer;
`SOLKEY_REBUILD=1` forces it. The Gradle task `:keyext.solidity.core:solidityCli` still exists
for CI and the IDE — note that its `--args` **replaces** the whole argument list.

`-f/--function` fails with the reason when the named function has no obligation (it returns a
value, or takes a parameter with no `.key` sort) instead of generating a malformed one. Without
`-f`, every function is proved and a `N/M closed` summary plus a `FAILED (k): …` recap is
printed; `--quiet` drops the per-function progress lines.

## Default scope

All tasks are assumed to be about **`keyext.solidity.core`** (source:
`keyext.solidity.core/src/main/java/org/key_project/solidity/`, tests in `src/test/`). Only look
outside if explicitly instructed.

| Purpose | Location |
|---|---|
| **Taclet examples (`.sol`)** | `keyext.solidity.examples/TestSuite.sol` — see its `README.md` |
| **Specified contracts (`.sol` + `@custom:key` clauses)** | `keyext.solidity.examples/contracts/`, published ones in `real-world/` — the spec language is in that `README.md` |
| **Problem files (`.key`)** | `keyext.solidity.core/src/test/resources/org/key_project/solidity/examples/` |
| **Proof rules (`.key`)** | `keyext.solidity.core/src/main/resources/org/key_project/solidity/proof/rules/` |

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
| `keyext.solidity.idea` | IntelliJ IDEA plugin — ▶ gutter icon on public functions and contracts; left click opens KeYther, right click also offers the headless prover and solc+EVM. Standalone Gradle build, deliberately **not** in `settings.gradle`. See `docs/idea-setup.md` |
| `keyext.solidity.examples` | **Main taclet examples** (`TestSuite.sol`) |

Dependencies: `keyext.*` → `key.core` → `key.ncore` → `key.util`.

## keyext.solidity.core architecture

```
Solidity → ANTLR → SolidityToKeyConverter → AST → TypeResolver → AbstractPO → Strategy + Rules → Proof
```

- **`program/ast/`** — AST nodes; **`expressions/`**, **`statement/`** beneath it;
  **`abstractions/`** — type system (`Type`, `KeYSolidityType`, `PrimitiveType`)
- **`parser/`** — `ParsingFacade`, `SolidityToKeyConverter`; **`builder/`** — AST builders
- **`logic/`** — `TermFactory`, `TermBuilder`; **`logic/op/`** — `ProgramVariable`
- **`proof/`** — `Proof`, `Goal`; **`proof/init/`** — proof obligations
- **`rule/`** — Rule interface, meta-constructs; **`speclang/`** — contracts and specifications
- **`strategy/`** — `Strategy`, `ApplyStrategy`
- **`common/`** — `SolidityInfo`, the registry for Solidity types (int8–int256, uint8–uint256,
  bytes1–bytes32, bool, address). Register new types here.
- **`runtime/`** — the in-process Besu EVM. `SolidityRuntimeCheck` compiles a contract, deploys it
  and calls its functions, reporting `Panic(0x01)` as a failed `assert`; drives `--solc` and
  `SolidityRuntimeExecutionTest`
- **`program/parser/SolJSONParser`** — parses solc's compact JSON AST; `SolcWrapper` and
  `WasmSolcCompiler` produce it by running solc's WebAssembly build on the JVM, with no
  external compiler. See `docs/solc-ast.md`

ANTLR grammars live in `keyext.solidity.core/src/main/antlr/`, generated sources in
`build/generated-src/antlr/main/`.

## CI gates — run before committing

| Gate | Local command |
|---|---|
| All three at once | `./gradlew -DENABLE_NULLNESS=true ciGates` |
| Formatting | `./gradlew spotlessApply` |
| Nullness | `./gradlew -DENABLE_NULLNESS=true :keyext.solidity.core:compileTestJava` |
| Module tests | `./gradlew :keyext.solidity.core:test` |

`ciGates` refuses to start without `-DENABLE_NULLNESS=true`. The nullness checker is scoped to
`org.key_project.solidity.program.ast`, so **only edits under `program/ast/` can trip it**. CI
also runs three test groups that `ciGates` does not. Details, the nullness idiom and the JDK
caveat: `docs/ci.md`.

## Code style

Spotless enforces formatting (`scripts/tools/checkstyle/keyCodeStyle.xml`). Fields are
`@NonNull` by default.

**Do not add comments to the code.** Write self-explanatory code instead; leave existing
comments untouched unless the change makes them wrong.

## Testing

`./gradlew :keyext.solidity.core:test` is the fast local set: unit tests plus the `TestSuite.sol`
suites (`TacletStarterExamplesTest`, `PaperTestExamplesTest`), ~30 s. It prints failures only;
`-PverboseTests` restores the per-test progress lines. The `solidityExamples` and
`ruleGeneralization` groups are CI-only — see `docs/ci.md`.
Run `test` after refactoring, and prefer modifying existing test classes over creating new ones.

## Documentation

Read the relevant doc before working on taclets. Each is a compact, agent-facing reference.

| Doc | Read when |
|---|---|
| `key-taclets.md` | **Start here** to author a taclet — rule shape, schema variables, varconds |
| `taclets-implementation.md` | Checking what is already implemented and why it is shaped that way |
| `taclet-ideas.md` | Picking the next unimplemented construct (the backlog) |
| `bugs.md` | Known bugs: crashes, stuck proofs, unprovable true facts |
| `storage.md` | Storage rules — calculus spec, three-step strategy, statement→rule table |
| `memory.md` | Memory rules — identity heap, aliasing, delete, cross-domain copies |
| `net.md` | The payment/ledger model (`net`, `msg.sender`/`msg.value`, `transfer`, invariants) |
| `require-assert.md` | `require` / `assert` rules (box vs. diamond false-branch behavior) |
| `rule-generalizations.md` | The `// generalization:` comments and `RuleGeneralizationTest` |
| `solc-ast.md` | The solc AST and the in-JVM compiler that produces it |
| `ci.md` | CI gates in detail, the nullness idiom, CI-only test groups |
| `forked-key-core.md` | Editing code forked from `key.core` — which files must not be restyled |
| `idea-setup.md` | IntelliJ setup — gutter-icon plugin, External Tools, `.run/` configurations |

Program rules live in `…/proof/rules/solidityProgramRules.key`, loaded via
`standardSolidityRules.key`. Add new taclet examples as functions of
`keyext.solidity.examples/TestSuite.sol`, and scenario/invariant examples as contracts plus
`.key` obligations in `keyext.solidity.examples/net/`; conventions for both are in
`keyext.solidity.examples/README.md`. After changing a feature, update
`docs/taclets-implementation.md` (implemented) or `docs/taclet-ideas.md` (backlog).
**When a change fixes a bug listed in `docs/bugs.md`, delete that entry in the same change;**
record newly found bugs there.

**When planning a new taclet:** begin with a plain-English statement of the precondition (what
must hold before the rule fires), the transformation (what sequent change it performs) and the
postcondition. Write that before any KeY syntax.
