/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.gui;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import org.key_project.solidity.gui.SolidityMain.Invocation;
import org.key_project.solidity.proof.init.SolidityProblemSpec;

import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Checks what a command line asks KeYther to open. This is the contract the IDEA plugin's gutter
/// icon speaks, so it is validated before any window exists: a line the GUI cannot act on has to
/// fail on the console rather than open an empty window.
public class SolidityMainArgsTest {

    private static final PrintStream SINK =
        new PrintStream(new ByteArrayOutputStream(), true, StandardCharsets.UTF_8);

    private static Invocation parse(String... args) {
        return SolidityMain.parse(args, SINK);
    }

    private static CommandLine.ParameterException rejects(String... args) {
        return assertThrows(CommandLine.ParameterException.class, () -> parse(args));
    }

    @Test
    void noArgumentsOpensNothing() {
        Invocation invocation = parse();
        assertNull(invocation.file());
        assertNull(invocation.spec());
    }

    @Test
    void aFileAloneLeavesTheChoiceToThePicker() {
        Invocation invocation = parse("A.sol");
        assertEquals("A.sol", invocation.file().getName());
        assertNull(invocation.spec());
    }

    @Test
    void aFunctionWithoutAContractLetsTheFileDecide() {
        assertEquals(new SolidityProblemSpec(null, "f"), parse("A.sol", "--function", "f").spec());
    }

    /// The two flags name one obligation; option order must not matter, since the plugin builds
    /// the line positionally.
    @Test
    void aContractAndFunctionNameOneObligation() {
        SolidityProblemSpec expected = new SolidityProblemSpec("C", "f");
        assertEquals(expected, parse("A.sol", "-c", "C", "-f", "f").spec());
        assertEquals(expected, parse("-f", "f", "-c", "C", "A.sol").spec());
        assertEquals(expected, parse("A.sol", "--contract", "C", "--function", "f").spec());
    }

    @Test
    void theFlagsNeedAFile() {
        assertTrue(rejects("--function", "f").getMessage().contains("need a FILE"));
        assertTrue(rejects("--contract", "C").getMessage().contains("need a FILE"));
    }

    /// Without `--function` the picker infers the contract itself, so `-c` alone would silently do
    /// nothing.
    @Test
    void aContractWithoutAFunctionIsRejected() {
        assertTrue(
            rejects("A.sol", "-c", "C").getMessage().contains("--contract needs --function"));
    }

    @Test
    void theFlagsApplyToSolidityFilesOnly() {
        assertTrue(rejects("a.key", "-f", "f").getMessage().contains(".sol files only"));
        assertEquals("a.key", parse("a.key").file().getName());
    }

    @Test
    void anUnknownOptionIsRejected() {
        rejects("A.sol", "--nope");
    }

    @Test
    void helpOpensNothing() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        assertNull(SolidityMain.parse(new String[] { "--help" },
            new PrintStream(out, true, StandardCharsets.UTF_8)));
        assertTrue(out.toString(StandardCharsets.UTF_8).contains("--function"),
            out.toString(StandardCharsets.UTF_8));
    }
}
