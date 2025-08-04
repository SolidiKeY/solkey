/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.logic.parser;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;

import org.junit.jupiter.api.Assertions;
import org.key_project.solidity.program.parser.SolcWrapper;

import org.junit.jupiter.api.Test;

class SolcWrapperTest {

    private final Path solc = Path.of("/opt", "local", "bin", "solc");

    @Test
    void readSolBuff() throws IOException {
        SolcWrapper wrapper = new SolcWrapper(solc);
        BufferedReader result = wrapper.readSolBuff(getFile("SimpleContract2.sol"));
        String line;
        System.out.println("Output:");
        while ((line = result.readLine()) != null) {
            System.out.println(line);
        }
        result.close();
    }

    @Test
    void testReadSol() throws IOException {
        SolcWrapper wrapper = new SolcWrapper(solc);
        String result = wrapper.readSol(getFile("SimpleContract2.sol"));
        Assertions.assertNotNull(result);
    }

    @Test
    void readStringSol() throws IOException {
        SolcWrapper wrapper = new SolcWrapper(solc);
        //language=solidity
        String contract = """
            contract SimpleContract {
                uint256 balance;
            }""";
        String result = wrapper.readSol(contract);
        Assertions.assertNotNull(result);
    }

    private static URI getFile(String solFileName) {
        try {
            return SolcWrapper.class.getResource(solFileName).toURI();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}
