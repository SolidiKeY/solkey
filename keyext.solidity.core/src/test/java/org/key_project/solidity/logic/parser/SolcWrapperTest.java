/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.logic.parser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.io.TempDir;
import org.key_project.solidity.program.ast.declarations.ContractDeclaration;
import org.key_project.solidity.program.parser.SolJSONParser;
import org.key_project.solidity.program.parser.SolcWrapper;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.key_project.solidity.program.parser.SolcWrapper.getDeclStrJsonParser;

class SolcWrapperTest {
    @TempDir
    Path tempDir;

    @Test
    void readStringSol() throws IOException {
        SolcWrapper wrapper = new SolcWrapper();
        // language=solidity
        String contract = """
                contract SimpleContract {
                    uint256 balance;
                }""";
        String result = wrapper.readSol(contract);
        assertNotNull(result);
    }

    @Test
    void compilationFail() throws IOException {
        SolcWrapper wrapper = new SolcWrapper();
        // language=solidity
        String contract = """
                contract SimpleContract {
                    uint256 balance
                }""";
        try {
            wrapper.readSol(contract);
            Assertions.fail();
        } catch (RuntimeException exception) {
        } ;
    }

    @Test
    void withPath() throws IOException {
        Path file = tempDir.resolve("contract.sol");
        // language=solidity
        String contract = """
                contract SimpleContract { }""";
        Files.writeString(file, contract);

        SolJSONParser jsonParser = new SolJSONParser();
        ContractDeclaration ctrl = getDeclStrJsonParser(jsonParser, file);
        assertNotNull(ctrl);
    }
}
