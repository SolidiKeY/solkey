package org.key_project.solidity.idea

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test

/**
 * What goes wrong here is an argument the other side rejects — and the other side is a separate
 * process, so a mistake costs a dialog nobody reads rather than a compile error.
 */
class SolKeyCommandTest {

    private val path = "/tmp/TestSuite.sol"
    private val function = SolKeyTarget.Function(SolFunction("TestSuite", "testAssert", 0, 10))
    private val freeFunction = SolKeyTarget.Function(SolFunction(null, "free", 0, 10))
    private val contract = SolKeyTarget.Contract(SolContract("TestSuite", 0, 100))

    private fun arguments(tool: SolKeyTool, target: SolKeyTarget) =
        SolKeyCommand.arguments(tool, target, path)

    @Test
    fun `the GUI opens straight on one function`() {
        assertEquals(
            listOf(path, "--contract", "TestSuite", "--function", "testAssert"),
            arguments(SolKeyTool.KEY_GUI, function),
        )
    }

    /** A free function has no contract to name, and KeYther infers it. */
    @Test
    fun `a free function is passed without a contract`() {
        assertEquals(
            listOf(path, "--function", "free"),
            arguments(SolKeyTool.KEY_GUI, freeFunction),
        )
    }

    /**
     * `SolidityMain.parse` rejects `--contract` without `--function`, so the contract-level click
     * has to leave the choice to KeYther's own picker.
     */
    @Test
    fun `the GUI on a contract passes the file alone`() {
        assertEquals(listOf(path), arguments(SolKeyTool.KEY_GUI, contract))
    }

    @Test
    fun `the headless prover explains an unclosed proof of one function`() {
        assertEquals(
            listOf(path, "--contract", "TestSuite", "--function", "testAssert", "--open-goals"),
            arguments(SolKeyTool.KEY_HEADLESS, function),
        )
    }

    /** No `--function`: that is what makes the CLI prove all of them and recap `N/M closed`. */
    @Test
    fun `the headless prover on a contract proves every function of it`() {
        assertEquals(
            listOf(path, "--contract", "TestSuite"),
            arguments(SolKeyTool.KEY_HEADLESS, contract),
        )
    }

    /** solc compiles and then runs, so it needs to know what was clicked. */
    @Test
    fun `solc runs the target that was clicked`() {
        assertEquals(
            listOf(path, "--solc", "--contract", "TestSuite", "--function", "testAssert"),
            arguments(SolKeyTool.SOLC, function),
        )
        assertEquals(
            listOf(path, "--solc", "--contract", "TestSuite"),
            arguments(SolKeyTool.SOLC, contract),
        )
    }

    @Test
    fun `the Gradle fallback names the file the way its task expects`() {
        assertEquals(
            listOf("-PsolFile=\"$path\"", "-Pcontract=TestSuite", "-Pfunction=testAssert"),
            SolKeyCommand.gradleParameters(SolKeyTool.KEY_GUI, function, path),
        )
        assertEquals(
            listOf("-PkeyFile=\"$path\"", "-Pcontract=TestSuite"),
            SolKeyCommand.gradleParameters(SolKeyTool.KEY_HEADLESS, contract, path),
        )
        assertEquals(
            listOf("-PkeyFile=\"$path\"", "-PcliArgs=--solc", "-Pcontract=TestSuite"),
            SolKeyCommand.gradleParameters(SolKeyTool.SOLC, contract, path),
        )
    }

    /** The jars differ, and so does everything that has to be true of them. */
    @Test
    fun `the GUI and the headless tools come from different jars`() {
        assertFalse(SolKeyTool.KEY_GUI.usesCoreJar)
        assertEquals(SolKeyTool.KEY_HEADLESS.jar, SolKeyTool.SOLC.jar)
        assertEquals(true, SolKeyTool.KEY_HEADLESS.usesCoreJar)
    }
}
