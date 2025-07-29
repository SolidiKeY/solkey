/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.logic.parser;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.key_project.solidity.logic.ast.SolidityProgramElement;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class SolJsonParserTest {

    @Test
    void parse() throws IOException {
        SolJsonParser jsonParser = new SolJsonParser();
        String solFileName = "SimpleContract1.json";
        URI fileURI = getFile(solFileName);
        Assertions.assertNotNull(fileURI);
        SolidityProgramElement se = jsonParser.parse(fileURI);
    }

    private static URI getFile(String solFileName) {
        try {
            // return FindResources.getResource(solFileName, SolJsonParserTest.class).toUri();
            return SolJsonParser.class.getResource(solFileName).toURI();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}
