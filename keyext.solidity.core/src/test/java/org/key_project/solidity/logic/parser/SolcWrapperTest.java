/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.logic.parser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.key_project.solidity.CLI;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.key_project.solidity.program.parser.SolcWrapper.readSol;

public class SolcWrapperTest {
    @Test
    void readStringSol() throws IOException {
        // language=solidity
        String contract = """
                contract SimpleContract {
                    uint256 balance;
                }""";
        String result = readSol(contract);
        assertNotNull(result);
    }

    @Test
    void compilationFail() throws IOException {
        // language=solidity
        String contract = """
                contract SimpleContract {
                    uint256 balance
                }""";
        try {
            readSol(contract);
            Assertions.fail();
        } catch (RuntimeException exception) {
        } ;
    }

    @Test
    void solcModeExitsOnErrorsOnly(@TempDir Path directory) throws IOException {
        // language=solidity
        Path good = Files.writeString(directory.resolve("Good.sol"), """
                contract Good {
                    uint256 balance;
                    function f() public { balance = 1; }
                }""");
        assertEquals(0, CLI.execute(good.toString(), "--solc"));

        // language=solidity
        Path broken = Files.writeString(directory.resolve("Broken.sol"), """
                contract Broken {
                    uint256 balance
                }""");
        assertEquals(1, CLI.execute(broken.toString(), "--solc"));
    }

    /// `--solc` does not stop at compiling: it deploys the contract on an in-process EVM and
    /// calls the function, so a violated `assert` is reported as one without any proof.
    @Test
    void solcModeRunsTheFunctionAndCatchesAFailingAssert(@TempDir Path directory)
            throws IOException {
        // language=solidity
        Path file = Files.writeString(directory.resolve("Demo.sol"), """
                contract Demo {
                    uint256 balance;
                    function holds() public { balance = 5; assert(balance == 5); }
                    function fails() public { balance = 5; assert(balance == 6); }
                }""");

        assertEquals(0, CLI.execute(file.toString(), "--solc", "--function", "holds"));
        assertEquals(1, CLI.execute(file.toString(), "--solc", "--function", "fails"));
        // Both together: the failing one decides the exit code.
        assertEquals(1, CLI.execute(file.toString(), "--solc"));
    }

    /// Taclet options select proof rules, and `--solc` proves nothing.
    @Test
    void solcModeRejectsTacletOptions(@TempDir Path directory) throws IOException {
        // language=solidity
        Path file = Files.writeString(directory.resolve("Good.sol"), """
                contract Good {
                    function f() public { }
                }""");
        assertEquals(1, CLI.execute(file.toString(), "--solc", "-O", "intRules:javaSemantics"));
    }
}
