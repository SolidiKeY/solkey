# IntelliJ IDEA setup

Everything in this document works from a fresh clone on any machine. The run configurations live
in `.run/` and are checked into git, so IDEA picks them up on import — there is nothing to
recreate per machine except the plugin of the next section and the External Tools further down,
neither of which IDEA can store inside a project.

Three entry points, narrowest first:

| Want | Use |
|---|---|
| Prove **one function** or **one contract** | the ▶ gutter icon (the plugin, below) |
| Prove **a file**, picking the function in a dialog | the External Tools or `.run/` configurations |
| Prove **from a shell** | `./run-key.sh Contract.sol`, `./gradlew :keyext.solidity.gui:solidityGui …` |

## Click ▶ beside a function or a contract → run it

The `keyext.solidity.idea` module is an IDEA plugin that draws a ▶ in the gutter beside every
public function of an open `.sol` file, and beside every `contract` in it. Clicking a function's
icon opens KeYther straight on that function's proof — no picker in between. A function no
obligation can be generated for (it returns a value, or takes a parameter with no `.key` sort) is
refused with the reason rather than falling back to the picker.

**Right-click** either icon for the three tools a SolKey checkout offers, all on whatever the icon
sits beside:

| Tool | What it runs | Where it reports |
|---|---|---|
| **Prove in KeYther (KeY GUI)** | `keyext.solidity.gui-exe.jar` | its own window |
| **Prove Headless (KeY CLI)** | `keyext.solidity.core-exe.jar` — the same thing `./run-key.sh` runs | a console in the Run tool window |
| **Compile and Run (solc + EVM)** | `keyext.solidity.core-exe.jar --solc` | a console in the Run tool window |

Left click stays what it always was: KeYther on that target.

On a **contract**, the headless prover gets `--contract Foo` and no `--function`, which is what
makes the CLI prove every provable function of it and end with `N/M closed` plus a
`FAILED (k): …` recap. On a **function** it also gets `--open-goals`, so an unclosed proof prints
its remaining sequents in the run that produced it. KeYther is the exception: it rejects
`--contract` without `--function` (a contract alone tells its picker nothing it cannot infer), so
a contract-level click there opens the picker on that file.

The solc entry does two things, because compiling alone answers a question nobody asks — a proof
that will not close usually has a contract that compiles perfectly well. So after printing solc's
errors and warnings it deploys the contract on an **in-process Besu EVM** and calls what you
clicked, one function or every provable function of a contract:

```
solc 0.8.34+commit.80d5c536.Emscripten.clang: 0 errors, 0 warnings
OK   assertHolds
FAIL assertFails — ASSERT_FAILED: Panic(0x01 assert failed)
1/2 ran without a failing assert
```

A violated `assert` reaches the caller as `Panic(0x01)`, and that is what `ASSERT_FAILED` is. It
takes about a second, and it is a *counterexample finder*, not a prover: one call with one set of
arguments says nothing about the states a proof quantifies over, but a `Panic(0x01)` here cannot
be argued away. Use it before reaching for KeY, not instead of it.

Two things it declines rather than answers, because the honest answer is unavailable:

- A function taking parameters that no leading `require` pins to a value is `SKIPPED` — there is
  nothing to call it with. `PinnedArguments` recovers the rest.
- A contract with a constructor or an initialized state variable is `SKIPPED` whole. The runner
  installs runtime bytecode without executing creation code, so its storage would start all-zero,
  which is not the contract you deploy — every verdict from it would be about an unreachable state.

This is the same machinery `SolidityRuntimeExecutionTest` has always used to cross-check the
calculus against a real EVM; it moved from the test source set into `org.key_project.solidity.runtime`
so the CLI can drive it too.

```bash
./gradlew -p keyext.solidity.idea buildPlugin
```

Then **Settings → Plugins → ⚙ → Install Plugin from Disk…**, pick
`keyext.solidity.idea/build/distributions/keyext.solidity.idea-0.1.0.zip`, and **restart the
IDE**. Nothing in the repository makes the icon appear on its own: the plugin has to be installed,
and **reinstalled after every change to it** — there is no hot reload. Use
`./gradlew -p keyext.solidity.idea runIde` while working on the plugin itself, which starts a
sandbox IDE with it already loaded.

All three are on the editor context menu under **SolKey**, and in *Find Action* as **Prove in
KeYther (KeY GUI)**, **Prove Headless (KeY CLI)** and **Compile with solc** — so each can take a
keyboard shortcut. From there they resolve the target from the caret: the public function it is
in, or else the contract around it.

No Solidity language plugin is required. `LineMarkerProvider`, the usual way to put something in
the gutter, is registered per language, and IDEA only knows Solidity if a third-party plugin
supplies it; the icons are therefore added straight to the editor's markup model, and the
functions are found by scanning the file (`SolFunctionScanner`). The scanner is deliberately
approximate — the authority on what can be proved is `SolidityOutline`, which reads solc's AST,
and KeYther applies it on launch. A false positive costs one error dialog, a false negative one
missing icon.

Inside this repository the plugin needs no jar configured: it walks up from the `.sol` file to the
checkout containing `keyext.solidity.gui/build.gradle`. For a Solidity project of your own there
is no such checkout, so point **Settings → Tools → SolKey** at a `keyext.solidity.gui-exe.jar` and
a `keyext.solidity.core-exe.jar`, built with
`./gradlew :keyext.solidity.gui:shadowJar :keyext.solidity.core:shadowJar`.

### It has to be a JDK 21

solc runs as WebAssembly on the JVM, and the Truffle release that interprets it calls a
`sun.misc.Unsafe` method JDK 24 removed — so **every** tool here, KeYther included, dies on the
first contract under a newer JVM. `run-key.sh` hunts for a 21 for the same reason.

The plugin used to run whatever JVM the IDE itself runs on, which works only for as long as the
JBR stays 21. It now takes the first JDK 21 of: **Settings → Tools → SolKey → JDK 21 home**,
`SOLKEY_JAVA_HOME`, `JAVA_HOME`, the project SDK, the IDE's own JVM. None of them a 21 and the
click says so in a notification instead of failing with a Truffle stack trace.

Inside a checkout it also picks up the Graal compiler jars `shadowJar` stages beside the fat jar
(`keyext.solidity.core/build/libs/graal-compiler/`) and passes the `--upgrade-module-path` that
keeps solc compiled rather than interpreted — about ten times the speed, exactly as `run-key.sh`
does. Outside one there is nowhere to find them, so solc is slower there, not broken.

### What a click actually runs, and why it matters

The fat jar the tool needs — `keyext.solidity.gui-exe.jar` for KeYther,
`keyext.solidity.core-exe.jar` for the headless prover and solc — whenever it exists and nothing
under the trees it is built from is newer than it. That is a plain `java -jar`, and the whole
launch of a proof — JVM start, taclet base, proof load — takes about six seconds.

Going through Gradle instead costs **eight seconds before any work happens at all**: that is what
`:keyext.solidity.gui:solidityGui --dry-run` measures on a warm daemon with nothing to compile,
because Gradle has to configure this repository's two dozen subprojects first. Launched from the
IDE it is worse — the Tooling API injects half a dozen init scripts and re-syncs the project model
on top. The first version of this plugin always took that route and it made every click feel
broken.

So Gradle is the fallback, used when the jar is missing or stale. It runs that jar's `shadowJar`
*and then* the matching task (`solidityGui` for KeYther, `solidityCli` for the other two), which
means that launch also leaves a fresh jar behind: after editing SolKey sources exactly one click
is slow, and the rest are fast again. Output goes to the Run tool window, and it uses the IDE's
configured Gradle JVM.

The fallback is also the only route that holds a Gradle daemon for as long as its KeYther window
is open. The fast route is just a detached process.

One limitation: solc permits overloaded functions while `--function` takes only a name, so two
icons on two overloads both prove the first. That is the whole `--function` API, the CLI included.
Proving the contract instead covers both, since it never names a function.

### Why the plugin is a separate Gradle build

`keyext.solidity.idea` has its own `settings.gradle.kts` and is **deliberately not in the root
`settings.gradle`**. Do not add it. Everything in the root `subprojects { }` block is applied
blindly to every included module, so an `include` would enrol an IDE plugin in:

- **spotless**, with the KeY GPL header on every `.java` — and `ciGates` builds its task list from
  `subprojects`, so a new module joins the formatting gate automatically;
- the **Checker Framework**, which CI runs repo-wide as `compileTestJava`;
- **maven-publish + signing**, i.e. the nightly `publishMavenJavaPublicationToKEYLABRepository`;
- the repo-wide **`assemble` and `test`** of the nightly and release-test workflows;
- the blanket **dependency block** (junit-bom, logback, `project(':key.util')` test fixtures).

The last two are the expensive ones: the IntelliJ Platform Gradle Plugin downloads a ~1 GB IDE
distribution, and it would be downloaded in every job that runs a repo-wide task. Keeping the
module out of the tree costs one `-p` flag and leaves the repository build unchanged by its
existence.

### Building against the IDE you actually run

The build downloads IDEA Community 2025.2 by default. `-PidePath=/path/to/idea` (the directory
holding `bin/` and `lib/`) builds against an IDE already on the machine instead — no download, and
it turns "probably compatible" into a compile, which is worth doing before installing into an IDE
several releases newer than the default:

```bash
./gradlew -p keyext.solidity.idea buildPlugin -PidePath=/path/to/idea
```

The Kotlin plugin version must be **at least the Kotlin the newest target IDE ships**. IDEA 2026.2
carries Kotlin 2.4.0 metadata, and a 2.1 compiler rejects its jars outright with dozens of
`Module was compiled with an incompatible version of Kotlin` errors — which is why this build is
pinned to 2.4.0 rather than something older. Compiling with a newer Kotlin against an older
platform is fine, so one version serves both.

`buildSearchableOptions` is disabled in that build: it spawns a whole headless IDE to index the
settings page for Settings search, which only matters for a Marketplace listing and is the one
step that needs a working display server. Two properties help with `runIde`:
`-PideJdk=/path/to/jdk-home` overrides the JVM it runs on, for distributions where the downloaded
IDE's bundled JBR cannot find the system X libraries (NixOS is the one this was hit on, failing
with `libXext.so.6: cannot open shared object file`), and `-PideProject=/path/to/dir` opens a
project in the sandbox — `keyext.solidity.examples` is a good one, small enough to import
instantly and full of `.sol` files to see icons on.

## Right-click a `.sol` file → prove it

This is the whole-file entry point — it opens the function picker, where the gutter icon above
goes straight to one proof. It is the one thing besides the plugin that needs a per-machine step:

```bash
scripts/install-idea-external-tools.sh      # then restart the IDE
```

It writes a `SolKey` toolset into every IntelliJ configuration directory it finds
(`~/.config/JetBrains/<IDE>/tools/SolKey.xml` on Linux, `~/Library/Application
Support/JetBrains/…` on macOS, `%APPDATA%\JetBrains\…` on Windows) with two entries:

| Tool | Runs |
|---|---|
| **Verify in KeYther (GUI)** | `./gradlew :keyext.solidity.gui:solidityGui -PsolFile=$FilePath$` |
| **Verify with Solidity CLI** | `./gradlew :keyext.solidity.core:solidityCli -PkeyFile=$FilePath$` |

Then right-click any `.sol` (or `.key`) file in the Project view or the editor →
**External Tools → SolKey → Verify in KeYther (GUI)**.

`--list` shows which IDEs would be written to, `--dry-run` prints without writing, `--all` widens
the search from IntelliJ to every JetBrains IDE. The script writes a toolset file of its own and
does not rewrite other toolsets — the one exception is the hand-made `Run with solidity` tool
that `Verify with Solidity CLI` replaces: its `External Tools.xml` is deleted when that tool is
all it holds, and otherwise left alone with a note to remove the entry in **Settings → Tools →
External Tools**.

Run it with the IDE closed: toolsets are read at startup, and a running IDE writes its own copy
back on exit, which can resurrect the tool that was just removed. The script warns when it sees
an IDE running.

External Tools cannot be shared through git — IDEA stores them in the IDE configuration
directory, not the project — which is why this is a script instead of a checked-in file.

## Shared run configurations (`.run/`)

| Configuration | What it does |
|---|---|
| **KeYther** | Gradle `:keyext.solidity.gui:solidityGui`, GUI with no file — pick one from `File → Open` |
| **KeYther on current file** | Launches the GUI on the file open in the editor (`$FilePath$`) |
| **SolidityCLI on current file** | Runs `org.key_project.solidity.CLI` on the file open in the editor |
| **solkey [:keyext.solidity.core:test]** | Gradle task, the fast module test group |

Verify the setup: open `keyext.solidity.examples/TestSuite.sol`, select **KeYther on current
file** in the run-configuration dropdown and press **Run** (Shift+F10). The function picker opens
listing the contract's functions; choose one and press **Start Proof**.

### Nothing shared may name a module

A shared configuration must never contain a `<module name="…"/>` element, which rules out
`Application` configurations. IDEA derives Gradle module names from the project path, and it
changed how it sanitizes the dots in names like `keyext.solidity.gui` between releases: the same
checkout imported by IDEA 2025.3 produced `solkey.keyext.solidity.gui.main`, and by 2026.2
`solkey.keyext_solidity_gui.main`. Either spelling is red — *"module not specified"* — in the
other IDE. That is what broke the earlier `Application` configurations.

The two types used here have no module reference:

- **Gradle** configurations (`KeYther`, the test one) name a task, resolved by Gradle.
- **JAR Application** configurations (the two *on current file* ones) name a jar:
  `$PROJECT_DIR$/keyext.solidity.gui/build/libs/keyext.solidity.gui-exe.jar`, built by a
  `:keyext.solidity.gui:shadowJar` before-launch step. Both `shadowJar` tasks set
  `archiveVersion = ""` precisely so this path does not carry `3.0.0-dev` and stays valid.

`JarApplication` is also the only module-free type that still expands `$FilePath$`: IDEA expands
those macros for configurations going through `ProgramParametersConfigurator` (Application, JAR,
…), while the Gradle plugin passes the literal text `$FilePath$` through. So a file-driven
configuration is a JAR one, and a Gradle one covers the no-file case.

The trade-off is the fat jar: the first launch after a code change re-runs `shadowJar` (a few
seconds), whereas the External Tools above go through the ordinary Gradle classpath.

### Paths inside the configurations

Use only IDEA macros (`$PROJECT_DIR$`, `$FilePath$`) and never an absolute path or a named SDK
(`ALTERNATIVE_JRE_PATH`) when editing these files: both are machine-local and turn a shared
configuration into one that only runs on the machine it was created on.

The configurations pass `$FilePath$`, the absolute path, rather than `$FileName$`. The Gradle
`solidityCli` task runs with its working directory set to
`keyext.solidity.core/src/test/resources/org/key_project/solidity/examples`, so a bare file name
resolves inside *that* directory and any file elsewhere fails with
`Error: "…/examples/TestSuite.sol" is not found.`

## Launching the GUI without the IDE

`:keyext.solidity.gui:solidityGui` is the GUI twin of `:keyext.solidity.core:solidityCli` and
takes the same kind of file property:

```bash
./gradlew :keyext.solidity.gui:solidityGui                                          # empty GUI
./gradlew :keyext.solidity.gui:solidityGui -PsolFile=keyext.solidity.examples/TestSuite.sol
./gradlew :keyext.solidity.gui:solidityGui -PkeyFile=problem2.key                   # a .key problem

# straight to one function's proof, no picker — what the ▶ gutter icon runs
./gradlew :keyext.solidity.gui:solidityGui -PsolFile=keyext.solidity.examples/TestSuite.sol \
    -Pcontract=TestSuite -Pfunction=testSimpleAssert
```

A relative name is looked up in the example directories and made absolute
(`ext.resolveSolidityFile` in the root `build.gradle`, shared with `solidityCli`), so the same
spelling works from anywhere in the repository.

`-Pcontract` / `-Pfunction` become `--contract` / `--function` on the command line, spelled
exactly as `solidityCli` spells them; the fat jar takes the same flags:

```bash
java -jar keyext.solidity.gui/build/libs/keyext.solidity.gui-exe.jar \
    keyext.solidity.examples/TestSuite.sol --contract TestSuite --function testSimpleAssert
```

`-Pcontract` is only meaningful with `-Pfunction` — without one the picker infers the contract
itself — and both apply to `.sol` files only. Either mistake is rejected before the window opens.

## Recommended settings

- **Java SDK:** Java 21 (required)
- **Gradle JVM:** Use project JDK
- **Build and run using:** Gradle — the `Make` step then produces the same classpath Gradle does,
  including the `solc` binary `processResources` downloads

## What stays out of git

`.idea/` is in `.gitignore`: it holds per-user state (`workspace.xml`, `shelf/`, resolved SDK
paths) that must not be shared. Store run configurations in `.run/` — the **Store as project
file** checkbox in the run-configuration dialog writes there — never in
`.idea/runConfigurations/`. If you add one, check it for a `<module>` element before committing;
see the section above.

The plugin build generates three directories, none of which belong in git.
`keyext.solidity.idea/build/` (the sandbox IDE and the distribution zip) and
`keyext.solidity.idea/.gradle/` were already covered by the unanchored `**/build/` and `.gradle`
entries. `.intellijPlatform/` was not, and needed its own rule: it is the IntelliJ Platform Gradle
plugin's local artifact cache — a coroutines javaagent plus one XML per bundled module of every
IDE built against, so roughly 160 files that name the exact IDE builds present on one machine.
