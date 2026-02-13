package org.key_project.solidity.parser;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.key_project.solidity.parser.SolidityParser.*;
import org.key_project.solidity.program.ast.declarations.StatementVariableDeclaration;
import org.key_project.solidity.program.ast.expressions.Expression;
import org.key_project.solidity.program.ast.expressions.TupleExpression;
import org.key_project.solidity.program.ast.expressions.literals.BoolLiteral;
import org.key_project.solidity.program.ast.expressions.literals.Uint256Literal;
import org.key_project.solidity.program.ast.expressions.operators.AddOperator;
import org.key_project.solidity.program.ast.expressions.operators.PlusPlusOperator;
import org.key_project.solidity.program.ast.statement.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.key_project.solidity.antlr.Parser.*;

public class SolidityToKeyConverterTest {

    @Test
    void emptyBlock(){
        Block block = parseBlock("{ }");
        assertEquals(0, block.getStatements().size());
    }

    @Test
    void literalInt(){
        Uint256Literal exp = (Uint256Literal) parseExpression("1");
        assertEquals(1, exp.getValue().intValue());
    }

    @Test
    void literalBool(){
        BoolLiteral exp = (BoolLiteral) parseExpression("false");
        assertEquals(false, exp.getValue());
    }

    @Test
    void tupleExpression(){
        TupleExpression exp = (TupleExpression) parseExpression("(false, true)");
        assertEquals(false, ((BoolLiteral) exp.getChild(0)).getValue());
    }

    @Test
    void addiction() {
        AddOperator exp = (AddOperator) parseExpression("1 + 2");
        assertEquals(1, ((Uint256Literal) exp.getChild(0)).getValue().intValue());
    }

    @Test
    void plusPlusLeft() {
        PlusPlusOperator exp = (PlusPlusOperator) parseExpression("++1");
        assertEquals(1, ((Uint256Literal) exp.getChild(0)).getValue().intValue());
        assertTrue(exp.isPrefix());
    }

    @Test
    void expStm() {
        ExpressionStatement stm = (ExpressionStatement) parseStatement("false;");
        assertFalse(((BoolLiteral) stm.expression).getValue());
    }

    @Test
    void variableAssignment() {
        Expression exp = parseExpression("x = 1");
        assertEquals("x", exp.getChild(0).toString());
    }

    @Test
    void variableDeclaration() {
        DeclarationStatement stm = (DeclarationStatement) parseStatement("var x;");
        assertEquals("x", ((StatementVariableDeclaration) stm.getDeclarations().get(0)).getProgramVariable().toString());
    }

    @Test
    void variableDeclarationWithType() {
        DeclarationStatement stm = (DeclarationStatement) parseStatement("int x;");
        assertEquals("x", ((StatementVariableDeclaration) stm.getDeclarations().get(0)).getProgramVariable().toString());
    }

    @Test
    void ifStatement() {
        ConditionStatement stm = (ConditionStatement) parseStatement("if(false) true;");
        assertFalse(((BoolLiteral) stm.getCondition()).getValue());
        assertTrue(((BoolLiteral) stm.getTrueBody().getChild(0)).getValue());
    }

    @Test
    void ifAndElseStatement() {
        ConditionStatement stm = (ConditionStatement) parseStatement("if(false) true; else false;");
        assertFalse(((BoolLiteral) stm.getCondition()).getValue());
        assertTrue(((BoolLiteral) stm.getTrueBody().getChild(0)).getValue());
        assertFalse(((BoolLiteral) stm.getFalseBody().getChild(0)).getValue());
    }

    @Test
    void returnStatement() {
        Statement stm = parseStatement("return;");
        assertInstanceOf(ReturnStatment.class, stm);
    }

    @Test
    void returnFalse() {
        ReturnStatment stm = (ReturnStatment) parseStatement("return false;");
        assertFalse(((BoolLiteral) stm.getReturnExp()).getValue());
    }

    @Test
    void simpleStatements() {
        Block block = parseBlock("{ continue; break; }");
        block.getStatements().stream().forEach(Assertions::assertNotNull);
        Assertions.assertEquals(2, block.getStatements().size());
    }

}
