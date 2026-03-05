/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.logic.parser;

import java.io.IOException;

import org.key_project.solidity.program.parser.SolcWrapper;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class SolcWrapperTest {

    @Test
    void readStringSol() throws IOException {
        SolcWrapper wrapper = new SolcWrapper();
        // language=solidity
        String contract = """
                contract SimpleContract {
                    uint256 balance;
                }""";
        String result = wrapper.readSol(contract);
        Assertions.assertNotNull(result);
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
}
