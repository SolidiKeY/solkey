/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.parser;

import org.key_project.solidity.logic.op.ProgramVariable;
import org.key_project.solidity.program.ast.declarations.StatementVariableDeclaration;
import org.key_project.solidity.program.ast.expressions.*;
import org.key_project.solidity.program.ast.expressions.literals.BoolLiteral;
import org.key_project.solidity.program.ast.expressions.literals.Uint256Literal;
import org.key_project.solidity.program.ast.expressions.operators.AddOperator;
import org.key_project.solidity.program.ast.expressions.operators.PlusPlusOperator;
import org.key_project.solidity.program.ast.expressions.operators.TernaryOperator;
import org.key_project.solidity.program.ast.statement.*;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.key_project.solidity.antlr.Parser.*;

public class SolidityToKeyConverterTest {

    @Test
    void emptyBlock() {
        Block block = parseBlock("{ }");
        assertEquals(0, block.getStatements().size());
    }

    @Test
    void creatingProgramVariables() {
        Block block = parseBlock("{ int x; x = 1; }");
        ProgramVariable p1 = (ProgramVariable) block.getChild(0).getChild(0).getChild(1);
        ProgramVariable p2 = (ProgramVariable) block.getChild(1).getChild(0).getChild(0);
        assertEquals("x", p1.toString());
        assertSame(p1, p2);
    }

    @Test
    void literalInt() {
        Uint256Literal exp = (Uint256Literal) parseExpression("1");
        assertEquals(1, exp.getValue().intValue());
    }

    @Test
    void literalBool() {
        BoolLiteral exp = (BoolLiteral) parseExpression("false");
        assertEquals(false, exp.getValue());
    }

    @Test
    void tupleExpression() {
        TupleExpression exp = (TupleExpression) parseExpression("(false, true)");
        assertEquals(false, ((BoolLiteral) exp.getChild(0)).getValue());
    }

    @Test
    void addiction() {
        AddOperator exp = (AddOperator) parseExpression("1 + 2");
        assertEquals(1, ((Uint256Literal) exp.getChild(0)).getValue().intValue());
    }

    @Test
    void complexOperations() {
        Expression exp = parseExpression("1 = 2 + 3 - 5 < 6");
        assertEquals("1 = 2 + 3 - 5 < 6", exp.toString());
    }

    @Test
    void plusPlusLeft() {
        PlusPlusOperator exp = (PlusPlusOperator) parseExpression("++1");
        assertEquals(1, ((Uint256Literal) exp.getChild(0)).getValue().intValue());
        assertTrue(exp.isPrefix());
    }

    @Test
    void plusPlusRight() {
        PlusPlusOperator exp = (PlusPlusOperator) parseExpression("1++");
        assertEquals(1, ((Uint256Literal) exp.getChild(0)).getValue().intValue());
        assertFalse(exp.isPrefix());
    }

    @Test
    void expStm() {
        ExpressionStatement stm = (ExpressionStatement) parseStatement("false;");
        assertFalse(((BoolLiteral) stm.getExpression()).getValue());
    }

    @Test
    void variableAssignment() {
        Expression exp = parseExpression("x = 1");
        assertEquals("x", exp.getChild(0).toString());
    }

    @Test
    void variableDeclaration() {
        DeclarationStatement stm = (DeclarationStatement) parseStatement("var x;");
        assertEquals("x", ((StatementVariableDeclaration) stm.getDeclarations().get(0))
                .getProgramVariable().toString());
    }

    @Test
    void variableDeclarationWithType() {
        DeclarationStatement stm = (DeclarationStatement) parseStatement("int x;");
        assertEquals("x", ((StatementVariableDeclaration) stm.getDeclarations().get(0))
                .getProgramVariable().toString());
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
        assertInstanceOf(ReturnStatement.class, stm);
    }

    @Test
    void returnFalse() {
        ReturnStatement stm = (ReturnStatement) parseStatement("return false;");
        assertFalse(((BoolLiteral) stm.getReturnExp()).getValue());
    }

    @Test
    void simpleStatements() {
        Block block = parseBlock("{ continue; break; }");
        block.getStatements().stream().forEach(Assertions::assertNotNull);
        Assertions.assertEquals(2, block.getStatements().size());
    }

    @Test
    void whileStatement() {
        WhileStatement stm = (WhileStatement) parseStatement("while(true) false;");
        assertTrue(((BoolLiteral) stm.getCondition()).getValue());
        assertFalse(((BoolLiteral) stm.getBody().getChild(0)).getValue());
    }

    @Test
    void doWhileStatement() {
        DoWhileStatement stm = (DoWhileStatement) parseStatement("do false; while(true);");
        assertTrue(((BoolLiteral) stm.getCondition()).getValue());
        assertFalse(((BoolLiteral) stm.getBody().getChild(0)).getValue());
    }

    @Test
    void forLoop() {
        ForStatement stm = (ForStatement) parseStatement("for(false; true; false) true;");
        assertEquals("for(false; true; false)\ntrue;", stm.toString());
    }

    @Test
    void forLoopEmptyInitial() {
        ForStatement stm = (ForStatement) parseStatement("for(; ;) true;");
        assertEquals("for(; ; )\ntrue;", stm.toString());
    }

    @Test
    void tryStm() {
        TryStatement stm = (TryStatement) parseStatement("try false { true; }");
        assertFalse(((BoolLiteral) stm.getExpression()).getValue());
        assertTrue(((BoolLiteral) stm.getBody().getStatements().get(0).getChild(0))
                .getValue());
    }

    @Test
    void tryCatch() {
        TryStatement stm = (TryStatement) parseStatement("try false catch {}");
        assertFalse(((BoolLiteral) stm.getExpression()).getValue());
        assertEquals(0, stm.getCatchClauseCount());
    }

    @Test
    void functionCall() {
        FunctionCallExpression exp = (FunctionCallExpression) parseExpression("f()");
        assertEquals("f", exp.functionExp.toString());
    }

    @Test
    void functionCallArguments() {
        FunctionCallExpression exp = (FunctionCallExpression) parseExpression("f(false)");
        assertEquals("f", exp.functionExp.toString());
        assertFalse(((BoolLiteral) exp.getArgument(0)).getValue());
    }

    @Test
    void array() {
        IndexExpression exp = (IndexExpression) parseExpression("v[false]");
        assertEquals("v", ((ProgramVariable) exp.getChild(0)).toString());
        assertFalse(((BoolLiteral) exp.getChild(1)).getValue());
    }

    @Test
    void sliceArray() {
        IndexRangeExpression exp = (IndexRangeExpression) parseExpression("v[false:true]");
        assertEquals("v", ((ProgramVariable) exp.getChild(0)).toString());
        assertFalse(((BoolLiteral) exp.getChild(1)).getValue());
        assertTrue(((BoolLiteral) exp.getChild(2)).getValue());
    }

    @Test
    void sliceEmpty() {
        IndexRangeExpression exp = (IndexRangeExpression) parseExpression("v[:]");
        assertEquals("v", ((ProgramVariable) exp.getChild(0)).toString());
        assertEquals(1, exp.getChildCount());
    }

    @Test
    void ternary() {
        TernaryOperator exp = (TernaryOperator) parseExpression("true ? true : false");
        assertTrue(((BoolLiteral) exp.getCondition()).getValue());
        assertTrue(((BoolLiteral) exp.getTrueExpression()).getValue());
        assertFalse(((BoolLiteral) exp.getFalseExpression()).getValue());
    }

}
