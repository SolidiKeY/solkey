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
  proof in the same run that produced it. Use the Java debugging API below when you need live
  terms, applicable rules, strategy decisions, or a proof session you can resume.
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
scripts/open-check.sh                # open/ functions whose status changed (now close: move them)

./gradlew :keyext.solidity.gui:solidityGui     # KeYther, the Swing GUI
./gradlew :key.ui:shadowJar                    # fat JAR
./gradlew :keyext.solidity.web:site            # the browser build (needs GRAALVM_HOME, docs/web.md)
./gradlew :keyext.solidity.web:browserTest     # ... and its headless-Chromium test
```

`run-key.sh` rebuilds `keyext.solidity.core-exe.jar` only when the sources are newer;
`SOLKEY_REBUILD=1` forces it. The Gradle task `:keyext.solidity.core:solidityCli` still exists
for CI and the IDE — note that its `--args` **replaces** the whole argument list.

`-f/--function` fails with the reason when the named function has no obligation (it returns a
value, or takes a parameter with no `.key` sort) instead of generating a malformed one. Without
`-f`, every function is proved and a `N/M closed` summary plus a `FAILED (k): …` recap is
printed; `--quiet` drops the per-function progress lines.

## Java debugging API

Use inline Java through **JDK 21 JShell** to inspect prover objects or experiment with one
proof. Use `run-key.sh` for ordinary proof runs, Gradle for builds and regression tests, and
shell/Python for file processing and orchestration. Rebuild the fat jar after source edits:
JShell does not perform `run-key.sh`'s freshness check.

The API lives in `org.key_project.solidity.control`. Existing `SolidityVerifier.verify(...)`
and `ProofSession.start(...)` calls still work.

| API | Purpose |
|---|---|
| `SolidityVerifier.VerificationOptions(limits, maxGoals, withProof)` | Named verification options; `defaults()` uses 10000 steps, no search timeout, at most 3 goal texts, no saved proof |
| `SolidityVerifier.verifyDetailed(path, spec, options)` | Returns `DetailedOutcome`: ordinary `outcome()`, `search()`, and the original `exception()` |
| `SolidityVerifier.verifyOrThrow(path, spec, options)` | Propagates load/serialization errors and throws a runtime exception with the original search exception as its cause |
| `Outcome.status()` / `DetailedOutcome.status()` | `PROVED`, `OPEN`, or `ERROR`; an open proof does not establish that the property is false |
| `ProofSession.Limits.steps(n)` / `defaults()` | Search limits; refine with `withTimeoutMillis(ms)`, `withMaxSteps(n)`, `withStrategy(option, value)` (`NO_TIMEOUT` is `-1`) |
| `ProofSession.open(path, spec, limits)` | Loads a Solidity obligation **without running proof search** |
| `ProofSession.load(path, limits, false)` | Loads/replays a `.key` or `.proof` without additional search; inspect `replayErrors()` |
| `session.runAutoDetailed(null)` | Runs all open goals and returns a `SearchResult`; pass an open node serial to run only its subtree |
| `session.lastSearch()` | Snapshot of the most recent search, or `null` before search/after manual apply or prune |
| `session.ruleDiagnosticsAt(serial, offset or text)` | Matching taclet candidates, missing schema variables, current strategy cost and approval disposition, diagnostic exceptions |
| `session.sequent(serial)` / `session.termAt(serial, offset or text)` | Actual logic objects; inspect `op()`, `sort()`, `subs()`, and bound/free variables |
| `session.proof().getServices().getNamespaces()` | Inspect registered sorts, functions and variables; open goals also expose `getOverlayServices()` |
| `session.root()` / `tree()` / `node(serial)` | Root serial, node serials, branches, pretty-printed sequents and applied rules; `node(serial, false)` disables pretty syntax |
| `session.offsetOf(serial, text)` | Offset of the first occurrence of `text` in the printed sequent; throws, showing the sequent, when absent |
| `session.applyRule(serial, text, ruleName[, instantiations])` | Applies a rule by name at the first occurrence of `text`; `applyRule(serial, ruleName)` for a sequent-wide rule. Throws with the candidate names when it does not match |
| `session.apply(serial, index, instantiations)` | Applies a candidate by index from the most recent `rulesAt`/`ruleDiagnosticsAt` call; instantiations map schema-variable names to KeY syntax |
| `session.prune(serial)` / `save()` | Prune an inner node of an open branch, or serialize the current proof |
| `session.close()` | Dispose the proof; use try-with-resources. `isClosed()` reports disposal, while `summary().closed()` reports proof closure |

Inline example, from the repository root (replace the JDK path if needed):

```bash
./gradlew :keyext.solidity.core:shadowJar
/usr/lib/jvm/java-21-openjdk/bin/jshell \
  -J-Djava.util.prefs.userRoot="$TMPDIR/jprefs" \
  --class-path keyext.solidity.core/build/libs/keyext.solidity.core-exe.jar \
  --feedback concise <<'JAVA'
import java.nio.file.Path;
import java.util.Map;
import org.key_project.solidity.control.*;
import org.key_project.solidity.proof.init.SolidityProblemSpec;

var source = Path.of("keyext.solidity.examples/TestSuite.sol");
var spec = SolidityProblemSpec.of("TestSuite", "storageRootReadWrite");
try (var session = ProofSession.open(source, spec, ProofSession.Limits.steps(1))) {
    int root = session.root();
    System.out.println(session.node(root).sequent());
    var term = session.termAt(root, "storageRootReadWrite");
    if (term != null) System.out.println(term.op() + " : " + term.sort());
    session.ruleDiagnosticsAt(root, "storageRootReadWrite").forEach(System.out::println);
    System.out.println(session.applyRule(root, "storageRootReadWrite", "functionBodyExpand"));
    System.out.println(session.runAutoDetailed(null));
    session.openGoalTexts(3).forEach(System.out::println);
    session.configure(ProofSession.Limits.defaults());
    System.out.println(session.runAutoDetailed(null));
    System.out.println(session.summary());
}
/exit
JAVA
```

For a one-shot result, with the same imports and `source`/`spec`:

```java
var options = new SolidityVerifier.VerificationOptions(
    ProofSession.Limits.steps(20000).withTimeoutMillis(30000), 3, true);
var result = SolidityVerifier.verifyDetailed(source, spec, options);
System.out.println(result.status());
System.out.println(result.search());
if (result.exception() != null) result.exception().printStackTrace();
System.out.println(result.outcome().summary());
result.outcome().openGoals().forEach(System.out::println);
```

`SearchResult` exposes `stopReason`, the original engine `message`, applied-rule and closed-goal
counts, search time, a non-closeable goal serial when available, and the original exception.
Reasons are `PROVED`, `TARGET_CLOSED` (the selected subtree closed while other goals remain),
`STEP_LIMIT`, `TIMEOUT`, `NO_APPLICABLE_RULE`, `NON_CLOSEABLE_GOAL`, `INTERRUPTED`, `ERROR`, or
`OTHER` for an unrecognized/custom stop condition. For the default stop condition, a reached
step limit takes precedence if both limits have been reached. Limits apply to each search run;
the timeout is in milliseconds and does not include source compilation/loading. `maxSteps=0`
or `timeout=0` allows a search that immediately stops. `-1` disables the timeout.

Rule dispositions are `ACCEPTED`, `NEEDS_INSTANTIATION`, `INFINITE_COST`, `NOT_APPROVED`, or
`ERROR`. These describe matching candidates at the selected position, not every rule in the
calculus or an explanation of every internal strategy feature. `ACCEPTED` is not a guarantee
that the scheduler will choose that rule. Diagnostic calls do not apply rules. Offsets refer to
the latest `node(serial, prettySyntax).sequent()` text (the `text` overloads use the pretty one); `-1` in a rule query means sequent-wide
rules. Rule indices expire after a new rule query or any apply/prune/search operation.

For parser debugging, use existing `SolidityOutline.of(path)` (in `program.parser`) for
contracts, function types, specs and reasons a function has no obligation; use `ParsingFacade`
(in `parser`) with `CharStreams.fromString(...)` to inspect logic expressions, sequents or
Solidity blocks. These parsed syntax objects are distinct from resolved logic terms in a loaded
session. `SolidityVerifier.problem(path, spec)` returns the generated `.key` problem.

Sessions are mutable and should be used on one thread. Detailed/ordinary one-shot verification
disposes its session before returning; returned proof text and diagnostic snapshots remain
usable. JShell reports snippet failures in its output; its process exit alone is not an
acceptance test. Convert a useful reproducer into an existing JUnit test and run it with Gradle.

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
| `keyext.solidity.web` | The prover compiled to WebAssembly by GraalVM Web Image, as a static page on GitHub Pages. Included only when `GRAALVM_HOME` has Web Image. See `docs/web.md` |
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
  external compiler. See `docs/solc-ast.md`

ANTLR grammars live in `keyext.solidity.core/src/main/antlr/`, generated sources in
`build/generated-src/antlr/main/`. One lexer/parser pair reads both logic and Solidity: a
modality opener pushes the lexer's `SOL` mode, whose tokens `KeYSolidityDLLexer` imports from
`SolidityLexer.g4`, and `KeYSolidityDLParser` imports the Solidity rules from
`SolidityRules.g4`, so a modality body is a subtree that `ExpressionBuilder` hands to
`SolidityToKeyConverter`. `SolidityLexer.g4` needs its one default-mode fragment: ANTLR rejects
a lexer grammar that starts with `mode`. Syntax errors fail the load (`ThrowingErrorListener`).

## CI gates — run before committing

| Gate | Local command |
|---|---|
| All three at once | `./gradlew -DENABLE_NULLNESS=true ciGates` |
| Formatting | `./gradlew spotlessApply` |
| Nullness | `./gradlew -DENABLE_NULLNESS=true :keyext.solidity.core:compileTestJava` |
| Module tests | `./gradlew :keyext.solidity.core:test` |

`ciGates` refuses to start without `-DENABLE_NULLNESS=true`. The nullness checker is scoped to
`org.key_project.solidity.program.ast`, so **only edits under `program/ast/` can trip it**. CI
also runs four test groups that `ciGates` does not. Details, the nullness idiom and the JDK
caveat: `docs/ci.md`.

## Code style

Spotless enforces formatting (`scripts/tools/checkstyle/keyCodeStyle.xml`). Fields are
`@NonNull` by default.

**Do not add comments to the code.** Write self-explanatory code instead; leave existing
comments untouched unless the change makes them wrong.

## Testing

`./gradlew :keyext.solidity.core:test` is the fast local set: unit tests plus the `TestSuite.sol`
suites (`TacletStarterExamplesTest`, `PaperTestExamplesTest`), ~30 s. It prints failures only;
`-PverboseTests` restores the per-test progress lines. The `solidityExamples`,
`ruleGeneralization` and `openExamples` groups are CI-only — see `docs/ci.md`.
Run `test` after refactoring, and prefer modifying existing test classes over creating new ones.

## Documentation

Read the relevant doc before working on taclets. Each is a compact, agent-facing reference.

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
| `solc-ast.md` | The solc AST and the in-JVM compiler that produces it |
| `ci.md` | CI gates in detail, the nullness idiom, CI-only test groups |
| `forked-key-core.md` | Editing code forked from `key.core` — which files must not be restyled |
| `web.md` | The browser build — Web Image, the JS solc bridge, the Pages deploy |
| `idea-setup.md` | IntelliJ setup — gutter-icon plugin, External Tools, `.run/` configurations |

Program rules live in `…/proof/rules/solidityProgramRules.key`, loaded via
`standardSolidityRules.key`. Add new taclet examples as functions of
`keyext.solidity.examples/TestSuite.sol`, and scenario/invariant examples as contracts plus
`.key` obligations in `keyext.solidity.examples/net/`; conventions for both are in
`keyext.solidity.examples/README.md`. After changing a feature, update
`docs/taclets-implementation.md` (implemented) or `docs/taclet-ideas.md` (backlog).
**When a change fixes a bug listed in `docs/bugs.md`, delete that entry in the same change,**
its section in `docs/semantics-bugs-fix-ideas.md`, and move every function it fixes out of
`solc/open/` and `real-world/open/` (into the closing file, dropping the `// open:` line and its
provenance row). `scripts/open-check.sh` lists them; CI's `OpenExamplesStayOpenTest` fails while
any is left. Record newly found bugs there.

**When planning a new taclet:** begin with a plain-English statement of the precondition (what
must hold before the rule fires), the transformation (what sequent change it performs) and the
postcondition. Write that before any KeY syntax.
