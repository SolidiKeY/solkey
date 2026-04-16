/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.logic.parser;

import java.io.IOException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

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
}
