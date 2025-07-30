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
import org.key_project.solidity.logic.ast.expressions.AddOperation;
import org.key_project.solidity.logic.ast.expressions.Expression;
import org.key_project.solidity.logic.ast.expressions.StateVariableReference;
import org.key_project.solidity.logic.ast.expressions.Uint256Literal;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


class SolJsonParserTest {

    @Test
    void parse() throws IOException {
        SolidityProgramElement programElement = getSolidityProgramElement("SimpleContract1.json");
        Assertions.assertInstanceOf(ContractDeclaration.class, programElement);
        ContractDeclaration contractDeclaration = (ContractDeclaration) programElement;
        Assertions.assertEquals(1, contractDeclaration.getFieldDeclarations().size());
    }

    @Test
    void parseContractWithIntAndBool() throws IOException {
        SolidityProgramElement programElement = getSolidityProgramElement("SimpleContract2.json");
        Assertions.assertInstanceOf(ContractDeclaration.class, programElement);
        ContractDeclaration contractDeclaration = (ContractDeclaration) programElement;
        Assertions.assertEquals(2, contractDeclaration.getFieldDeclarations().size());
    }

    @Test
    void parseContractWithIntAndBoolSet() throws IOException {
        SolidityProgramElement programElement = getSolidityProgramElement("SimpleContract3.json");
        Assertions.assertInstanceOf(ContractDeclaration.class, programElement);
        ContractDeclaration contractDeclaration = (ContractDeclaration) programElement;
        Assertions.assertEquals(2, contractDeclaration.getFieldDeclarations().size());
    }

    @Test
    void parseContractWithAddition() throws IOException {
        SolidityProgramElement programElement = getSolidityProgramElement("SimpleContract4.json");
        Assertions.assertInstanceOf(ContractDeclaration.class, programElement);
        ContractDeclaration contractDeclaration = (ContractDeclaration) programElement;
        Assertions.assertEquals(2, contractDeclaration.getFieldDeclarations().size());
    }

    @Test
    void parseContractWithReferenceAddition() throws IOException {
        SolidityProgramElement programElement = getSolidityProgramElement("SimpleContract5.json");
        Assertions.assertInstanceOf(ContractDeclaration.class, programElement);
        ContractDeclaration contractDeclaration = (ContractDeclaration) programElement;
        Assertions.assertEquals(2, contractDeclaration.getFieldDeclarations().size());
        Expression initializer = contractDeclaration.getFieldDeclarations().get(1).getInitializer();
        Assertions.assertNotNull(initializer);
        Assertions.assertInstanceOf(AddOperation.class, initializer);
        Assertions.assertInstanceOf(StateVariableReference.class,
            ((AddOperation) initializer).getChild(0));
        Assertions.assertInstanceOf(Uint256Literal.class, ((AddOperation) initializer).getChild(1));
    }


    @Test
    void parseContractWithBoth() throws IOException {
        SolidityProgramElement programElement = getSolidityProgramElement("SimpleContract6.json");
        Assertions.assertInstanceOf(ContractDeclaration.class, programElement);
        ContractDeclaration contractDeclaration = (ContractDeclaration) programElement;
        Assertions.assertEquals(2, contractDeclaration.getFieldDeclarations().size());
    }

    private static SolidityProgramElement getSolidityProgramElement(String solFileName)
            throws IOException {
        SolJSONParser jsonParser = new SolJSONParser();
        URI fileURI = getFile(solFileName);
        Assertions.assertNotNull(fileURI);
        List<SolidityProgramElement> unit = jsonParser.parse(fileURI);
        Assertions.assertNotNull(unit);
        Assertions.assertEquals(1, unit.size());
        SolidityProgramElement programElement = unit.get(0);
        return programElement;
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
