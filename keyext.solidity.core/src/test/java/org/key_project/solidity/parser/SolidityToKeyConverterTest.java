/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.parser;

import org.key_project.solidity.logic.op.ProgramVariable;
import org.key_project.solidity.program.ast.abstractions.*;
import org.key_project.solidity.program.ast.declarations.*;
import org.key_project.solidity.program.ast.expressions.*;
import org.key_project.solidity.program.ast.expressions.literals.*;
import org.key_project.solidity.program.ast.expressions.operators.*;
import org.key_project.solidity.program.ast.statement.*;
import org.key_project.solidity.rule.sv.ProgramSV;

import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.key_project.solidity.parser.ParserForTesting.*;
import static org.key_project.solidity.program.ast.abstractions.PrimitiveType.*;
import static org.key_project.solidity.program.ast.declarations.FunctionEnums.DataLocation.*;

public class SolidityToKeyConverterTest {

    @Test
    void emptyBlock() {
        Block block = parseBlock("{ }");
        assertEquals(0, block.getStatements().size());
    }

    @Test
    void creatingProgramVariables() {
        Block block = parseBlock("{ int x; x = 1; }");
        ProgramVariable p1 =
            ((StatementVariableDeclaration) ((DeclarationStatement) block.getStatements().get(0))
                    .getDeclarations().get(0)).getProgramVariable();
        ProgramVariable p2 =
            (ProgramVariable) ((BinaryExpression) ((ExpressionStatement) block.getStatements()
                    .get(1))
                    .getExpression()).getLeft();
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
        assertFalse(exp.getValue());
    }

    @Test
    void schemaVariable() {
        ProgramSV exp = (ProgramSV) parseExpression("s#v");
        assertEquals("v", exp.toString());
    }

    @Test
    void tupleExpression() {
        TupleExpression exp = (TupleExpression) parseExpression("(false, true)");
        assertFalse(((BoolLiteral) exp.getExpression(0)).getValue());
    }

    @Test
    void addition() {
        BinaryExpression exp = (BinaryExpression) parseExpression("1 + 2");
        assertEquals(1, ((Uint256Literal) exp.getLeft()).getValue().intValue());
    }

    @Test
    void complexOperations() {
        Expression exp = parseExpression("1 = 2 + 3 - 5 < 6");
        assertEquals("1 = 2 + 3 - 5 < 6", exp.toString());
    }

    @Test
    void plusPlusLeft() {
        UnaryExpression exp = (UnaryExpression) parseExpression("++1");
        assertEquals(1, ((Uint256Literal) exp.getExp()).getValue().intValue());
        assertTrue(exp.getOperator().isPrefix());
        assertTrue(exp.getOperator() == Operator.PRE_INC);
    }

    @Test
    void plusPlusRight() {
        UnaryExpression exp = (UnaryExpression) parseExpression("1++");
        assertEquals(1, ((Uint256Literal) exp.getExp()).getValue().intValue());
        assertTrue(exp.getOperator().isPostfix());
        assertTrue(exp.getOperator() == Operator.POST_INC);

    }

    @Test
    void expStm() {
        ExpressionStatement stm = (ExpressionStatement) parseStatement("false;");
        assertFalse(((BoolLiteral) stm.getExpression()).getValue());
    }

    @Test
    void variableAssignment() {
        Expression exp = parseExpression("x = 1");
        assertEquals("x", ((BinaryExpression) exp).getLeft().toString());
    }

    @Test
    @Disabled("It is not possible to type check unknown type")
    void variableDeclaration() {
        DeclarationStatement stm = (DeclarationStatement) parseStatement("var x;");
        assertEquals("x", ((StatementVariableDeclaration) stm.getDeclarations().get(0))
                .getProgramVariable().toString());
    }

    @Test
    void variableDeclarationWithType() {
        DeclarationStatement stm = (DeclarationStatement) parseStatement("int x;");
        ProgramVariable x =
            ((StatementVariableDeclaration) stm.getDeclarations().get(0)).getProgramVariable();
        assertEquals("x", x.toString());
        assertEquals(INT, x.getType());
    }

    @Test
    void ifStatement() {
        ConditionStatement stm = (ConditionStatement) parseStatement("if(false) true;");
        assertFalse(((BoolLiteral) stm.getCondition()).getValue());
        assertTrue(
            ((BoolLiteral) ((ExpressionStatement) stm.getThenBody()).getExpression()).getValue());
    }

    @Test
    void ifAndElseStatement() {
        ConditionStatement stm = (ConditionStatement) parseStatement("if(false) true; else false;");
        assertFalse(((BoolLiteral) stm.getCondition()).getValue());
        assertTrue(
            ((BoolLiteral) ((ExpressionStatement) stm.getThenBody()).getExpression()).getValue());
        assertFalse(
            ((BoolLiteral) ((ExpressionStatement) stm.getElseBody()).getExpression()).getValue());
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
        assertFalse(
            ((BoolLiteral) ((ExpressionStatement) stm.getBody()).getExpression()).getValue());
    }

    @Test
    void doWhileStatement() {
        DoWhileStatement stm = (DoWhileStatement) parseStatement("do false; while(true);");
        assertTrue(((BoolLiteral) stm.getCondition()).getValue());
        assertFalse(
            ((BoolLiteral) ((ExpressionStatement) stm.getBody()).getExpression()).getValue());
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
        assertTrue(((BoolLiteral) ((ExpressionStatement) stm.getBody().getStatements().get(0))
                .getExpression())
                .getValue());
    }

    @Test
    void tryWithReturn() {
        TryStatement stm =
            (TryStatement) parseStatement("try false returns (bool a) { a = false; }");
        assertFalse(((BoolLiteral) stm.getExpression()).getValue());
        assertEquals(1, stm.getReturnCount());
        ProgramVariable ra = stm.getReturnParameter(0);
        ProgramVariable ba = (ProgramVariable) ((BinaryExpression) ((ExpressionStatement) stm
                .getBody().getStatements().get(0)).getExpression()).getLeft();
        assertSame(ra, ba);
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
        assertEquals("v", ((ProgramVariable) exp.getLeftExp()).toString());
        assertFalse(((BoolLiteral) exp.getIndexExp()).getValue());

        assertEquals(2, exp.getChildCount());
        assertEquals("v", ((ProgramVariable) exp.getChild(0)).toString());
        assertFalse(((BoolLiteral) exp.getChild(1)).getValue());
    }

    @Test
    void indexExpressionGetChildOutOfBounds() {
        IndexExpression exp = (IndexExpression) parseExpression("v[1]");
        assertThrows(IndexOutOfBoundsException.class, () -> exp.getChild(2));
        assertThrows(IndexOutOfBoundsException.class, () -> exp.getChild(-1));
    }

    @Test
    void dynamicArrayDefinition() {
        DeclarationStatement stm = (DeclarationStatement) parseStatement("bool[] b;");
        ProgramVariable b =
            ((StatementVariableDeclaration) stm.getDeclarations().get(0)).getProgramVariable();
        DynamicArrayType type = (DynamicArrayType) b.getType();
        assertEquals(BOOL, type.getElementType());
    }

    @Test
    void staticArrayDefinition() {
        DeclarationStatement stm = (DeclarationStatement) parseStatement("bool[10] b;");
        ProgramVariable b =
            ((StatementVariableDeclaration) stm.getDeclarations().get(0)).getProgramVariable();
        ArrayType type = (ArrayType) b.getType();
        assertEquals(BOOL, type.getElementType());
        assertEquals(10, type.length());
    }

    @Test
    void sliceArrayBothPresent() {
        IndexRangeExpression exp = (IndexRangeExpression) parseExpression("v[false:true]");
        assertEquals("v", ((ProgramVariable) exp.getBaseExp()).toString());
        assertFalse(((BoolLiteral) exp.getStartExp()).getValue());
        assertTrue(((BoolLiteral) exp.getEndExp()).getValue());

        assertEquals(3, exp.getChildCount());
        assertEquals("v", ((ProgramVariable) exp.getChild(0)).toString());
        assertFalse(((BoolLiteral) exp.getChild(1)).getValue());
        assertTrue(((BoolLiteral) exp.getChild(2)).getValue());
        assertThrows(IndexOutOfBoundsException.class, () -> exp.getChild(3));
        assertThrows(IndexOutOfBoundsException.class, () -> exp.getChild(-1));
    }

    @Test
    void sliceArrayOnlyStart() {
        IndexRangeExpression exp = (IndexRangeExpression) parseExpression("v[false:]");
        assertEquals("v", ((ProgramVariable) exp.getBaseExp()).toString());
        assertFalse(((BoolLiteral) exp.getStartExp()).getValue());
        assertNull(exp.getEndExp());

        assertEquals(2, exp.getChildCount());
        assertEquals("v", ((ProgramVariable) exp.getChild(0)).toString());
        assertFalse(((BoolLiteral) exp.getChild(1)).getValue());
        assertThrows(IndexOutOfBoundsException.class, () -> exp.getChild(2));
        assertThrows(IndexOutOfBoundsException.class, () -> exp.getChild(-1));
    }

    @Test
    void sliceArrayOnlyEnd() {
        IndexRangeExpression exp = (IndexRangeExpression) parseExpression("v[:true]");
        assertEquals("v", ((ProgramVariable) exp.getBaseExp()).toString());
        assertNull(exp.getStartExp());
        assertTrue(((BoolLiteral) exp.getEndExp()).getValue());

        assertEquals(2, exp.getChildCount());
        assertEquals("v", ((ProgramVariable) exp.getChild(0)).toString());
        assertTrue(((BoolLiteral) exp.getChild(1)).getValue());
        assertThrows(IndexOutOfBoundsException.class, () -> exp.getChild(2));
        assertThrows(IndexOutOfBoundsException.class, () -> exp.getChild(-1));
    }

    @Test
    void sliceArrayEmpty() {
        IndexRangeExpression exp = (IndexRangeExpression) parseExpression("v[:]");
        assertEquals("v", ((ProgramVariable) exp.getBaseExp()).toString());
        assertNull(exp.getStartExp());
        assertNull(exp.getEndExp());

        assertEquals(1, exp.getChildCount());
        assertEquals("v", ((ProgramVariable) exp.getChild(0)).toString());
        assertThrows(IndexOutOfBoundsException.class, () -> exp.getChild(1));
        assertThrows(IndexOutOfBoundsException.class, () -> exp.getChild(-1));
    }

    @Test
    void ternary() {
        TernaryExpression exp = (TernaryExpression) parseExpression("true ? true : false");
        assertTrue(((BoolLiteral) exp.getCondition()).getValue());
        assertTrue(((BoolLiteral) exp.getTrueExpression()).getValue());
        assertFalse(((BoolLiteral) exp.getFalseExpression()).getValue());
    }

    @Test
    void newExp() {
        NewExpression exp = (NewExpression) parseExpression("new bool");
        assertEquals("bool", exp.getType().name().toString());
    }

    @Test
    void structType() {
        DeclarationStatement stm = (DeclarationStatement) parseStatement("Person memory alice;");
        ProgramVariable alice =
            ((StatementVariableDeclaration) stm.getDeclarations().get(0)).getProgramVariable();
        assertEquals("alice", alice.name().toString());
        assertEquals(Memory, alice.getDataLocation());
        assertInstanceOf(StructDeclaration.class, alice.getType());
        Type structType = alice.getType();
        assertEquals("Person", structType.toString());
    }
}
