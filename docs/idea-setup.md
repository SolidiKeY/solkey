# IntelliJ IDEA setup

Everything in this document works from a fresh clone on any machine. The run configurations live
in `.run/` and are checked into git, so IDEA picks them up on import — there is nothing to
recreate per machine except the External Tools of the last section, which IDEA refuses to store
inside a project.

## Right-click a `.sol` file → prove it

This is the main entry point, and the one thing that needs a per-machine step:

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
```

A relative name is looked up in the example directories and made absolute
(`ext.resolveSolidityFile` in the root `build.gradle`, shared with `solidityCli`), so the same
spelling works from anywhere in the repository.

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
