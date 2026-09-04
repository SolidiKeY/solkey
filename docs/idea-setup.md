# IntelliJ IDEA setup

Everything in this document works from a fresh clone on any machine. The run configurations live
in `.run/` and are checked into git, so IDEA picks them up on import — there is nothing to
recreate per machine.

## Shared run configurations (`.run/`)

| Configuration | What it does |
|---|---|
| **KeYther on current file** | Launches the GUI on the file open in the editor (`$FilePath$`) |
| **KeYther** | Launches the GUI with no file, so you pick one from `File → Open` |
| **SolidityCLI on current file** | Runs `org.key_project.solidity.CLI` on the file open in the editor |
| **solkey [:keyext.solidity.gui:run]** | Gradle task, GUI with no file |
| **solkey [:keyext.solidity.core:test]** | Gradle task, the fast module test group |

Verify the setup: open `keyext.solidity.examples/TestSuite.sol`, select **KeYther on current
file** in the run-configuration dropdown and press **Run** (Shift+F10). The function picker opens
listing the contract's functions; choose one and press **Start Proof**.

### Why `Application` and not `Gradle` configurations

The three file-driven configurations are `Application` configurations on purpose. IDEA expands
macros such as `$FilePath$` only for run configurations going through
`ProgramParametersConfigurator` (Application, JAR, …). The Gradle plugin does no macro expansion,
so a Gradle configuration would pass the literal text `$FilePath$` to the program.

### Why the module names are stable

They name modules `solkey.keyext.solidity.gui.main` and `solkey.keyext.solidity.core.main`.
`settings.gradle` pins `rootProject.name = "solkey"` so those names do not depend on the name of
the directory the repository was cloned into — without the pin, a clone into a differently named
directory would break every configuration here.

### Paths inside the configurations

Use only IDEA macros (`$PROJECT_DIR$`, `$FilePath$`) and never an absolute path or a named SDK
(`ALTERNATIVE_JRE_PATH`) when editing these files: both are machine-local and turn a shared
configuration into one that only runs on the machine it was created on.

`SolidityCLI on current file` passes `$FilePath$`, the absolute path, rather than `$FileName$`.
The Gradle `solidityCli` task runs with its working directory set to
`keyext.solidity.core/src/test/resources/org/key_project/solidity/examples`, so a bare file name
resolves inside *that* directory and any file elsewhere fails with
`Error: "…/examples/TestSuite.sol" is not found.`

## Optional: right-click a file in the Project view

Run configurations act on the *editor's* current file. To run one from the Project view's
right-click menu instead, add an External Tool. External Tools are IDE-level, not project-level,
configuration — IDEA stores them under `~/.config/JetBrains/<IDE>/tools/`, so unlike the `.run/`
configurations they cannot be shared through git and have to be repeated on each machine. Prefer
the run configurations above; add this only if you want the Project-view context menu.

1. Open **Settings → Tools → External Tools**
2. Click **+** to add a new tool
3. Configure with these values:
   - **Name:** `Verify in KeYther (GUI)`
   - **Description:** `Open the selected file in the Solidity prover GUI`
   - **Program:** `$ProjectFileDir$/gradlew`
   - **Arguments:** `:keyext.solidity.gui:run --args="$FilePath$"`
   - **Working directory:** `$ProjectFileDir$`
4. Under **Advanced Options**, ensure **Open console for tool output** is checked, and tick the
   *Show in* boxes (editor, project view, main menu) — a tool with all of them unticked is
   reachable only through Search Everywhere
5. Click **OK**

Then right-click any `.sol` file and select **External Tools → Verify in KeYther (GUI)**.

For the CLI instead of the GUI, the same recipe with
`:keyext.solidity.core:solidityCli -PkeyFile=$FilePath$`.

## Recommended settings

- **Java SDK:** Java 21 (required)
- **Gradle JVM:** Use project JDK
- **Build and run using:** Gradle — the `Make` step then produces the same classpath Gradle does,
  including the `solc` binary `processResources` downloads

## What stays out of git

`.idea/` is in `.gitignore`: it holds per-user state (`workspace.xml`, `shelf/`, resolved SDK
paths) that must not be shared. Store run configurations in `.run/` — the **Store as project
file** checkbox in the run-configuration dialog writes there — never in
`.idea/runConfigurations/`.
