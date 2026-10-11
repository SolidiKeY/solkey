# AGENTS.md

**SolKey** is a fork of [KeY](https://github.com/KeYProject/key) — an interactive theorem prover
for Java — extended with `keyext.solidity.core` for formal verification of **Solidity smart
contracts**. Java 21 required.

**This file is the index: when a task needs more than it says, open the doc the tables below
point at.** Each doc is a compact, agent-facing reference; read it by section
(`grep -n '^#' doc`, then `sed -n`), not whole.

## Working efficiently

Cost is dominated by round-trips, not output size. So:

- **Never read a rules `.key` file whole** (`solidityProgramRules.key` is 4 800 lines). Use
  `scripts/taclet.sh NAME` for one taclet, `--index` for the section banners, `--list` for every
  rule name with its `file:line`.
- **Prove with `./run-key.sh`**, not with Gradle: it runs the fat jar, so there is no Gradle
  banner and no startup cost, and `--open-goals` prints the remaining sequents in the same run.
- **Batch independent shell commands** into one call.

## Build and run

```bash
./gradlew :keyext.solidity.core:test # Module tests: unit + TestSuite.sol suites (~30 s)
./gradlew :keyext.solidity.core:test --tests "org.key_project.solidity.SomeTest.methodName"
./gradlew -DENABLE_NULLNESS=true ciGates   # All three CI gates (see docs/ci.md)
./gradlew spotlessApply              # Apply formatting

./run-key.sh FILE.sol                          # prove every function
./run-key.sh FILE.sol -f fnName --open-goals   # one function, and why it did not close
./run-key.sh FILE.key -m 20000 --no-prove      # a .key problem; any CLI option works
./run-key.sh FILE.sol -O transferSemantics:withCallback   # a non-default taclet option
./run-key.sh FILE.sol --solc [-f fnName]       # compile, then run on an EVM: failing asserts
./run-key.sh FILE.sol -f fnName --print-problem # print the generated .key problem instead
./run-key.sh --help                            # every CLI option

scripts/taclet.sh --index|--list|NAME   # rule banners | every rule's file:line | one taclet
scripts/benchmark.sh                 # published contracts as published: N/M closed each
scripts/open-check.sh                # open/ functions whose status changed (now close: move them)

./gradlew :keyext.solidity.gui:solidityGui     # KeYther, the Swing GUI
./gradlew :key.ui:shadowJar                    # fat JAR
./gradlew :keyext.solidity.web:site            # browser build (needs GRAALVM_HOME, docs/web.md)
./gradlew :keyext.solidity.web:browserTest     # ... and its headless-Chromium test
```

`run-key.sh` rebuilds `keyext.solidity.core-exe.jar` only when the sources are newer;
`SOLKEY_REBUILD=1` forces it. The Gradle task `:keyext.solidity.core:solidityCli` still exists
for CI and the IDE — note that its `--args` **replaces** the whole argument list.

Without `-f`, every function is proved and a `N/M closed` summary plus a `FAILED (k): …` recap
is printed; `--quiet` drops the per-function progress lines. With `-f`, a function that has no
obligation (it returns a value, or takes a parameter with no `.key` sort) fails with the reason.

To inspect live terms, applicable rules, strategy decisions or a resumable proof session, use
the JShell API in **`docs/debug-api.md`**.

## Default scope

All tasks are assumed to be about **`keyext.solidity.core`** (source:
`keyext.solidity.core/src/main/java/org/key_project/solidity/`, tests in `src/test/`). Only look
outside if explicitly instructed. Module layout, package map and the ANTLR setup are in
**`docs/architecture.md`**.

| Purpose | Location |
|---|---|
| **Taclet examples (`.sol`)** | `keyext.solidity.examples/TestSuite.sol` — see its `README.md` |
| **Specified contracts (`.sol` + `@custom:key` clauses)** | `keyext.solidity.examples/contracts/`, published ones in `real-world/` — the spec language is in that `README.md` |
| **Problem files (`.key`)** | `keyext.solidity.core/src/test/resources/org/key_project/solidity/examples/` |
| **Proof rules (`.key`)** | `…/src/main/resources/org/key_project/solidity/proof/rules/` — `solidityProgramRules.key`, loaded via `standardSolidityRules.key` |

## Before committing

Run `./gradlew -DENABLE_NULLNESS=true ciGates` (formatting, nullness, module tests; it refuses
to start without that flag). The nullness checker is scoped to
`org.key_project.solidity.program.ast`, so **only edits under `program/ast/` can trip it**. CI
also runs four test groups that `ciGates` does not — `docs/ci.md`.

`:keyext.solidity.core:test` prints failures only; `-PverboseTests` restores progress lines.
Run it after refactoring, and prefer modifying existing test classes over creating new ones.

## Code style

Spotless enforces formatting (`scripts/tools/checkstyle/keyCodeStyle.xml`). Fields are
`@NonNull` by default.

**Do not add comments to the code.** Write self-explanatory code instead; leave existing
comments untouched unless the change makes them wrong.

## Documentation

| Doc | Read when |
|---|---|
| `key-taclets.md` | **Start here** to author a taclet — rule shape, schema variables, varconds |
| `taclets-implementation.md` | Checking what is already implemented and why it is shaped that way |
| `taclet-ideas.md` | Picking the next unimplemented construct (the backlog) |
| `bugs.md` | Known bugs: crashes, stuck proofs, unprovable true facts |
| `semantics-bugs-fix-ideas.md` | Planning a fix for a semantics bug in `bugs.md` — ranked ideas and the order of work |
| `limitations.md` | Why the supported Solidity is incomplete or buggy — what the solc and real-world ports could not prove |
| `storage.md` | Storage rules — calculus spec, three-step strategy, statement→rule table |
| `memory.md` | Memory rules — identity heap, aliasing, delete, cross-domain copies |
| `net.md` | The payment/ledger model (`net`, `msg.sender`/`msg.value`, `transfer`, invariants) |
| `require-assert.md` | `require` / `assert` rules (box vs. diamond false-branch behavior) |
| `rule-generalizations.md` | The `// generalization:` comments and `RuleGeneralizationTest` |
| `debug-api.md` | Inspecting prover objects from JShell — sessions, terms, rule diagnostics |
| `architecture.md` | Modules, the `keyext.solidity.core` package map, the ANTLR grammars |
| `solc-ast.md` | The solc AST and the in-JVM compiler that produces it |
| `ci.md` | CI gates in detail, the nullness idiom, CI-only test groups |
| `forked-key-core.md` | Editing code forked from `key.core` — which files must not be restyled |
| `web.md` | The browser build — Web Image, the JS solc bridge, the Pages deploy |
| `idea-setup.md` | IntelliJ setup — gutter-icon plugin, External Tools, `.run/` configurations |

## Taclet workflow

**When planning a new taclet:** begin with a plain-English statement of the precondition (what
must hold before the rule fires), the transformation (what sequent change it performs) and the
postcondition. Write that before any KeY syntax.

Add new taclet examples as functions of `keyext.solidity.examples/TestSuite.sol`, and
scenario/invariant examples as contracts plus `.key` obligations in
`keyext.solidity.examples/net/`; conventions for both are in that module's `README.md`. After
changing a feature, update `docs/taclets-implementation.md` (implemented) or
`docs/taclet-ideas.md` (backlog).

**When a change fixes a bug listed in `docs/bugs.md`, delete that entry in the same change,**
its section in `docs/semantics-bugs-fix-ideas.md`, and move every function it fixes out of
`solc/open/` and `real-world/open/` (into the closing file, dropping the `// open:` line and its
provenance row). `scripts/open-check.sh` lists them; CI's `OpenExamplesStayOpenTest` fails while
any is left. Record newly found bugs there.
