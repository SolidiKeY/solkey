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
import org.key_project.solidity.logic.ast.declarations.FunctionDeclaration;
import org.key_project.solidity.logic.ast.declarations.StateVariableDeclaration;
import org.key_project.solidity.logic.ast.expressions.Expression;
import org.key_project.solidity.logic.ast.expressions.literals.BoolLiteral;
import org.key_project.solidity.logic.ast.expressions.literals.Uint256Literal;
import org.key_project.solidity.logic.ast.expressions.operators.AddOperation;
import org.key_project.solidity.logic.ast.references.StateVariableReference;
import org.key_project.solidity.logic.ast.statement.Block;

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
        StateVariableDeclaration firstField = contractDeclaration.getFieldDeclarations().get(0);
        Assertions.assertInstanceOf(Uint256Literal.class, firstField.getInitializer());
        Assertions.assertEquals(1000,
            ((Uint256Literal) firstField.getInitializer()).getValue().longValue());
        StateVariableDeclaration secondField = contractDeclaration.getFieldDeclarations().get(1);
        Assertions.assertInstanceOf(BoolLiteral.class, secondField.getInitializer());
        Assertions.assertSame(BoolLiteral.TRUE, secondField.getInitializer());
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

    @Test
    void parseFunction() throws IOException {
        SolidityProgramElement programElement = getSolidityProgramElement("SimpleContract7.json");
        Assertions.assertInstanceOf(ContractDeclaration.class, programElement);
        ContractDeclaration contractDeclaration = (ContractDeclaration) programElement;
        Assertions.assertEquals(1, contractDeclaration.getFunctions().size());
        FunctionDeclaration functionDeclaration = contractDeclaration.getFunctions().get(0);
        Block block = functionDeclaration.getBody();
        Assertions.assertNotNull(block);
        Assertions.assertTrue(block.getChildCount() == 0);
    }

    @Test
    void parseComplexFunction() throws IOException {
        SolidityProgramElement programElement = getSolidityProgramElement("SimpleContract8.json");
        Assertions.assertInstanceOf(ContractDeclaration.class, programElement);
        ContractDeclaration contractDeclaration = (ContractDeclaration) programElement;
        Assertions.assertEquals(1, contractDeclaration.getFunctions().size());
        FunctionDeclaration function = contractDeclaration.getFunctions().getFirst();
        Assertions.assertEquals(1, function.getInputParameters().size());
        Assertions.assertEquals(1, function.getReturnParameters().size());
        Block block = function.getBody();
        Assertions.assertNotNull(block);
        Assertions.assertTrue(block.getChildCount() == 1);
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
