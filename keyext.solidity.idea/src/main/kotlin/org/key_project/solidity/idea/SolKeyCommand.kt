package org.key_project.solidity.idea

/**
 * What a click runs.
 *
 * All three are SolKey entry points that exist independently of the IDE — KeYther, the headless
 * CLI and solc — so this enum is only a table of how to reach each one, and the arguments are
 * exactly what a shell would pass.
 */
enum class SolKeyTool(
    val displayName: String,
    /** How the tool reads in a menu. */
    val actionText: String,
    /** The fat jar, relative to the solkey checkout. */
    val jar: String,
    /** The task that builds [jar]. */
    val jarTask: String,
    /** The task to fall back to when [jar] is missing or stale. */
    val gradleTask: String,
    /** The trees whose edits make [jar] stale. */
    val sourceTrees: List<String>,
    /** Whether the run belongs in a console: a window of its own means it does not. */
    val showsConsole: Boolean,
) {
    KEY_GUI(
        displayName = "KeY GUI",
        actionText = "Prove in KeYther (KeY GUI)",
        jar = "keyext.solidity.gui/build/libs/keyext.solidity.gui-exe.jar",
        jarTask = ":keyext.solidity.gui:shadowJar",
        gradleTask = ":keyext.solidity.gui:solidityGui",
        sourceTrees = listOf("keyext.solidity.gui/src", "keyext.solidity.core/src"),
        showsConsole = false,
    ),
    KEY_HEADLESS(
        displayName = "KeY headless",
        actionText = "Prove Headless (KeY CLI)",
        jar = CORE_JAR,
        jarTask = CORE_JAR_TASK,
        gradleTask = ":keyext.solidity.core:solidityCli",
        sourceTrees = CORE_SOURCE_TREES,
        showsConsole = true,
    ),
    SOLC(
        displayName = "solc",
        actionText = "Compile and Run (solc + EVM)",
        jar = CORE_JAR,
        jarTask = CORE_JAR_TASK,
        gradleTask = ":keyext.solidity.core:solidityCli",
        sourceTrees = CORE_SOURCE_TREES,
        showsConsole = true,
    );

    val usesCoreJar: Boolean get() = jar == CORE_JAR
}

private const val CORE_JAR = "keyext.solidity.core/build/libs/keyext.solidity.core-exe.jar"
private const val CORE_JAR_TASK = ":keyext.solidity.core:shadowJar"
private val CORE_SOURCE_TREES = listOf("keyext.solidity.core/src", "key.core/src")

/** What a click runs *on*. */
sealed interface SolKeyTarget {

    /** How the target reads in a window title, a notification and the Run tool window. */
    val label: String

    data class Function(val function: SolFunction) : SolKeyTarget {
        override val label: String
            get() = function.contract?.let { "$it.${function.name}" } ?: function.name
    }

    data class Contract(val contract: SolContract) : SolKeyTarget {
        override val label: String get() = contract.name
    }
}

/**
 * The command line for a tool and a target.
 *
 * Kept free of IDE types so it can be tested, the way `SolidityMain.parse` is: what goes wrong
 * here is an argument the other side rejects, and that is worth catching without a running IDE.
 */
object SolKeyCommand {

    /** The arguments to the fat jar, `FILE` first. */
    fun arguments(tool: SolKeyTool, target: SolKeyTarget, filePath: String): List<String> =
        buildList {
            add(filePath)
            when (tool) {
                // KeYther rejects `--contract` without `--function` (a contract alone tells its
                // picker nothing it cannot infer), so a contract-level click opens the picker.
                SolKeyTool.KEY_GUI -> if (target is SolKeyTarget.Function) {
                    addContractAndFunction(target.function)
                }

                SolKeyTool.KEY_HEADLESS -> when (target) {
                    is SolKeyTarget.Function -> {
                        addContractAndFunction(target.function)
                        // An unclosed proof should say why in the run that produced it.
                        add("--open-goals")
                    }
                    // No `--function`: the CLI then proves every provable function of the
                    // contract and recaps with `N/M closed`.
                    is SolKeyTarget.Contract -> addAll(listOf("--contract", target.contract.name))
                }

                // solc compiles and then runs what was clicked, so it takes the same target.
                SolKeyTool.SOLC -> {
                    add("--solc")
                    when (target) {
                        is SolKeyTarget.Function -> addContractAndFunction(target.function)
                        is SolKeyTarget.Contract ->
                            addAll(listOf("--contract", target.contract.name))
                    }
                }
            }
        }

    /** The Gradle properties that make [gradleTask][SolKeyTool.gradleTask] do the same thing. */
    fun gradleParameters(tool: SolKeyTool, target: SolKeyTarget, filePath: String): List<String> =
        buildList {
            // Quoted: `scriptParameters` is one string, split with ParametersListUtil.parse, which
            // honours double quotes — so a path with spaces survives as one argument.
            add(if (tool.usesCoreJar) "-PkeyFile=\"$filePath\"" else "-PsolFile=\"$filePath\"")
            when (tool) {
                SolKeyTool.KEY_GUI -> if (target is SolKeyTarget.Function) {
                    addContractAndFunctionProperties(target.function)
                }

                SolKeyTool.KEY_HEADLESS -> when (target) {
                    is SolKeyTarget.Function -> {
                        addContractAndFunctionProperties(target.function)
                        add("-PcliArgs=--open-goals")
                    }
                    is SolKeyTarget.Contract -> add("-Pcontract=${target.contract.name}")
                }

                SolKeyTool.SOLC -> {
                    add("-PcliArgs=--solc")
                    when (target) {
                        is SolKeyTarget.Function ->
                            addContractAndFunctionProperties(target.function)
                        is SolKeyTarget.Contract -> add("-Pcontract=${target.contract.name}")
                    }
                }
            }
        }

    private fun MutableList<String>.addContractAndFunction(function: SolFunction) {
        function.contract?.let { addAll(listOf("--contract", it)) }
        addAll(listOf("--function", function.name))
    }

    private fun MutableList<String>.addContractAndFunctionProperties(function: SolFunction) {
        function.contract?.let { add("-Pcontract=$it") }
        add("-Pfunction=${function.name}")
    }
}
