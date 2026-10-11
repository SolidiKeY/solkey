# Java debugging API

Read this when a task needs live terms, applicable rules, strategy decisions, or a resumable
proof session. For ordinary proof runs use `./run-key.sh`; for builds and regression tests use
Gradle.

Use inline Java through **JDK 21 JShell** to inspect prover objects or experiment with one
proof. Rebuild the fat jar after source edits: JShell does not perform `run-key.sh`'s
freshness check.

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
