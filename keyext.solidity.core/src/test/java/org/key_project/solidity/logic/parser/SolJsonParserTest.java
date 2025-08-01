/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.logic.parser;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import org.key_project.logic.SyntaxElement;
import org.key_project.solidity.program.ast.SolidityProgramElement;
import org.key_project.solidity.program.ast.declarations.ContractDeclaration;
import org.key_project.solidity.program.ast.declarations.FunctionDeclaration;
import org.key_project.solidity.program.ast.declarations.StateVariableDeclaration;
import org.key_project.solidity.program.ast.expressions.Expression;
import org.key_project.solidity.program.ast.expressions.literals.BoolLiteral;
import org.key_project.solidity.program.ast.expressions.literals.Uint256Literal;
import org.key_project.solidity.program.ast.expressions.operators.*;
import org.key_project.solidity.program.ast.references.StateVariableReference;
import org.key_project.solidity.program.ast.statement.Block;
import org.key_project.solidity.program.ast.statement.ExpressionStatement;
import org.key_project.solidity.program.ast.statement.Statement;
import org.key_project.solidity.program.parser.SolJSONParser;

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
        Assertions.assertInstanceOf(AddOperator.class, initializer);
        Assertions.assertInstanceOf(StateVariableReference.class,
            ((AddOperator) initializer).getChild(0));
        Assertions.assertInstanceOf(Uint256Literal.class, ((AddOperator) initializer).getChild(1));
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
        Assertions.assertEquals(0, block.getChildCount());
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
        Assertions.assertEquals(1, block.getChildCount());
    }

    @Test
    void parseSimpleAssignment() throws IOException {
        SolidityProgramElement programElement = getSolidityProgramElement("SimpleAssignment.json");
        Assertions.assertInstanceOf(ContractDeclaration.class, programElement);
        ContractDeclaration contractDeclaration = (ContractDeclaration) programElement;
        Assertions.assertEquals(1, contractDeclaration.getFunctions().size());
        FunctionDeclaration function = contractDeclaration.getFunctions().getFirst();
        Block block = function.getBody();
        Assertions.assertNotNull(block);
        Assertions.assertEquals(1, block.getChildCount());
        Assertions.assertEquals(1, block.getStatements().size());
        Statement exprStmnt = block.getStatements().get(0);
        Assertions.assertInstanceOf(ExpressionStatement.class, exprStmnt);
        Assertions.assertInstanceOf(AssignmentExpression.class, exprStmnt.getChild(0));
    }

    @Test
    void parseContractWithOperations() throws IOException {
        SolidityProgramElement programElement = getSolidityProgramElement("Operations.json");
        Assertions.assertInstanceOf(ContractDeclaration.class, programElement);
        ContractDeclaration contractDeclaration = (ContractDeclaration) programElement;
        Assertions.assertEquals(1, contractDeclaration.getFieldDeclarations().size());
        Assertions.assertInstanceOf(ExponentialOperator.class,
            contractDeclaration.getFieldDeclarations().get(0).getChild(1));
    }

    @Test
    void parseContractWithBoolOperations() throws IOException {
        SolidityProgramElement programElement = getSolidityProgramElement("BoolOperations.json");
        Assertions.assertInstanceOf(ContractDeclaration.class, programElement);
        ContractDeclaration contractDeclaration = (ContractDeclaration) programElement;
        Assertions.assertEquals(1, contractDeclaration.getFieldDeclarations().size());
        Assertions.assertInstanceOf(OrOperator.class,
            contractDeclaration.getFieldDeclarations().get(0).getInitializer());
    }

    @Test
    void parseContractWithBoolIntOperations() throws IOException {
        SolidityProgramElement programElement = getSolidityProgramElement("BoolIntOperations.json");
        Assertions.assertInstanceOf(ContractDeclaration.class, programElement);
        ContractDeclaration contractDeclaration = (ContractDeclaration) programElement;
        Assertions.assertEquals(1, contractDeclaration.getFieldDeclarations().size());
        Assertions.assertInstanceOf(AndOperator.class,
            contractDeclaration.getFieldDeclarations().get(0).getInitializer());
    }

    @Test
    void parseUnaryOperations() throws IOException {
        SolidityProgramElement programElement = getSolidityProgramElement("UnaryOperator.json");
        Assertions.assertInstanceOf(ContractDeclaration.class, programElement);
        ContractDeclaration contractDeclaration = (ContractDeclaration) programElement;
        Assertions.assertEquals(3, contractDeclaration.getFieldDeclarations().size());
        SyntaxElement exp = contractDeclaration.getChild(2).getChild(1).getChild(0);
        Assertions.assertInstanceOf(PlusPlusOperator.class, exp);
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
