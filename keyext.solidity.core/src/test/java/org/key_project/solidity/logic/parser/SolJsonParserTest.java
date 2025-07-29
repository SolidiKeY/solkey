/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.logic.parser;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import org.key_project.solidity.logic.ast.SolidityProgramElement;
import org.key_project.solidity.logic.ast.declarations.ContractDeclaration;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class SolJsonParserTest {

    @Test
    void parse() throws IOException {
        SolJSONParser jsonParser = new SolJSONParser();
        String solFileName = "SimpleContract1.json";
        URI fileURI = getFile(solFileName);
        Assertions.assertNotNull(fileURI);
        List<SolidityProgramElement> unit = jsonParser.parse(fileURI);
        Assertions.assertNotNull(unit);
        Assertions.assertEquals(1, unit.size());
        SolidityProgramElement programElement = unit.get(0);
        Assertions.assertInstanceOf(ContractDeclaration.class, programElement);
        ContractDeclaration contractDeclaration = (ContractDeclaration) programElement;
        Assertions.assertTrue(contractDeclaration.getFieldDeclarations().size() == 1);
    }

    private static URI getFile(String solFileName) {
        try {
            // return FindResources.getResource(solFileName, SolJsonParserTest.class).toUri();
            return SolJSONParser.class.getResource(solFileName).toURI();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}
