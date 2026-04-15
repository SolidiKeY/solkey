/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.visitor;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.key_project.logic.Name;
import org.key_project.logic.SyntaxElement;
import org.key_project.logic.sort.Sort;
import org.key_project.solidity.common.Services;
import org.key_project.solidity.logic.op.ProgramVariable;
import org.key_project.solidity.logic.sort.SortImpl;
import org.key_project.solidity.program.ast.SolidityProgramElement;
import org.key_project.solidity.program.ast.abstractions.KeYSolidityType;
import org.key_project.solidity.program.ast.abstractions.PrimitiveType;
import org.key_project.solidity.program.ast.declarations.ContractDeclaration;
import org.key_project.solidity.program.ast.declarations.StatementVariableDeclaration;
import org.key_project.solidity.program.ast.expressions.Expression;
import org.key_project.solidity.program.ast.expressions.IndexExpression;
import org.key_project.solidity.program.ast.expressions.operators.BinaryOperator;
import org.key_project.solidity.program.ast.expressions.operators.UnaryOperator;
import org.key_project.solidity.program.ast.statement.Block;
import org.key_project.solidity.program.ast.statement.ConditionStatement;
import org.key_project.solidity.program.ast.statement.DeclarationStatement;
import org.key_project.solidity.program.ast.statement.DoWhileStatement;
import org.key_project.solidity.program.ast.statement.ExpressionStatement;
import org.key_project.solidity.program.ast.statement.ForStatement;
import org.key_project.solidity.program.ast.statement.Statement;
import org.key_project.solidity.program.ast.statement.WhileStatement;
import org.key_project.util.collection.ImmutableArray;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.key_project.solidity.parser.ParserForTesting.*;
import static org.key_project.solidity.program.parser.SolcParserNoServices.getDeclStr;

class TestProgVarReplaceVisitor {

    private final Services services;
    private final Map<ProgramVariable, ProgramVariable> map;
    private final KeYSolidityType uintKST;
    private final ProgramVariable replacement;

    public TestProgVarReplaceVisitor() {
        map = new HashMap<>();
        services = new Services();

        final Sort uint = new SortImpl(new Name("uint"), false);
        uintKST = new KeYSolidityType(PrimitiveType.UINT, uint);
        services.getNamespaces().sorts().add(uint);
        replacement = new ProgramVariable(new Name("replacement"), uintKST, null);
    }

    // TODO: why does this test should work?
    @Disabled("Not understanding why should pass")
    @Test
    void testNoReplacement() {
        // parse in statements
        Statement stmnt = null; // <- here actual statement needed
        ProgVarReplaceVisitor replacer = new ProgVarReplaceVisitor(stmnt, map, false, services);
        replacer.start();
        assertEquals(stmnt, replacer.result()); // stmnt.equals(repl.result())
        assertSame(stmnt, replacer.result()); // stmnt == repl.result()
    }


    @Test
    void testReplacement() {
        Expression original = new ProgramVariable(new Name("original"), uintKST, null);
        Expression replacement = new ProgramVariable(new Name("replacement"), uintKST, null);

        map.put((ProgramVariable) original, (ProgramVariable) replacement);

        ProgVarReplaceVisitor replacer = new ProgVarReplaceVisitor(original, map, false, services);
        replacer.start();
        assertEquals(replacement, replacer.result());
    }

    @Test
    void testSimpleInt() {
        Block body = parseBlock("{ int original; original = 5; }");
        DeclarationStatement dstm = (DeclarationStatement) body.getStatements().get(0);
        StatementVariableDeclaration stm =
            (StatementVariableDeclaration) dstm.getDeclarations().get(0);

        ProgramVariable original = stm.getProgramVariable();
        addMap(original);

        ProgramVariable result =
            ((StatementVariableDeclaration) runOn(stm)).getProgramVariable();
        assertSame(replacement, result);
    }

    @Test
    void testWholeBody() {
        Block body = parseBlock("{ int original; original = 5; }");
        extractAndMap(body);

        Block result = runOnBlock(body);
        assertSame(replacement, getDeclaredVar(result));
        assertSame(replacement, ((BinaryOperator) getExpression(result, 1)).getLeft());
    }

    @Test
    void testArray() {
        Block body = parseBlock("{ int256[10] memory original; original[1] = 1; }");
        extractAndMap(body);

        Block result = runOnBlock(body);
        assertEquals(replacement, getDeclaredVar(result));
        assertEquals(replacement, ((IndexExpression) ((BinaryOperator) getExpression(result, 1)).getLeft()).getLeftExp());
    }

    @Test
    void testStruct() {
        DeclarationStatement dstm = (DeclarationStatement) parseStatement("Person memory original;");
        StatementVariableDeclaration stm =
            (StatementVariableDeclaration) dstm.getDeclarations().get(0);

        ProgramVariable original = stm.getProgramVariable();
        addMap(original);

        StatementVariableDeclaration result = (StatementVariableDeclaration) runOn(stm);
        assertEquals(replacement, result.getProgramVariable());
    }

    @Test
    void testEnum() {
        Block body = parseBlock("{ State s; s = s; }");
        extractAndMap(body);

        Block result = runOnBlock(body);
        assertEquals(replacement, getDeclaredVar(result));
        assertEquals(replacement, ((BinaryOperator) getExpression(result, 1)).getLeft());
    }

    @Test
    void testFor() {
        Block body = parseBlock("{ int original; for(original = 0; original<10; original++){} }");
        extractAndMap(body);

        Block result = runOnBlock(body);
        assertSame(replacement, getDeclaredVar(result));
        Statement forLoop = result.getStatements().get(1);
        assertSame(replacement, ((BinaryOperator) ((ForStatement) forLoop).getInit().getInit()).getLeft());
        assertSame(replacement, ((BinaryOperator) ((ForStatement) forLoop).getCondition()).getLeft());
        assertSame(replacement, ((UnaryOperator) ((ForStatement) forLoop).getUpdate().getUpdate()).getExp());
    }

    @Test
    void testIf() {
        Block body = parseBlock("{ int original; if(original == 2) original = 0; else original = 1; }");
        extractAndMap(body);

        Block result = runOnBlock(body);
        assertSame(replacement, getDeclaredVar(result));
        ConditionStatement ifStmt = (ConditionStatement) result.getStatements().get(1);
        assertSame(replacement, ((BinaryOperator) ifStmt.getCondition()).getLeft());
        assertSame(replacement, ((BinaryOperator) ((ExpressionStatement) ifStmt.getTrueBody()).getExpression()).getLeft());
        assertSame(replacement, ((BinaryOperator) ((ExpressionStatement) ifStmt.getFalseBody()).getExpression()).getLeft());
    }

    @Test
    void testWhile() {
        Block body = parseBlock("{ int original; while(original == 0) original = 1; }");
        extractAndMap(body);

        Block result = runOnBlock(body);
        assertSame(replacement, getDeclaredVar(result));
        WhileStatement whileStmt = (WhileStatement) result.getStatements().get(1);
        assertSame(replacement, ((BinaryOperator) whileStmt.getCondition()).getLeft());
        assertSame(replacement, ((BinaryOperator) ((ExpressionStatement) whileStmt.getBody()).getExpression()).getLeft());
    }

    @Test
    void testUnaryIncrement() {
        Block body = parseBlock("{ int original; original++; }");
        extractAndMap(body);

        Block result = runOnBlock(body);
        assertSame(replacement, getDeclaredVar(result));
        assertSame(replacement, ((UnaryOperator) getExpression(result, 1)).getExp());
    }

    @Test
    void testCompoundAssignment() {
        Block body = parseBlock("{ int original; original += 1; }");
        extractAndMap(body);

        Block result = runOnBlock(body);
        assertSame(replacement, getDeclaredVar(result));
        assertSame(replacement, ((BinaryOperator) getExpression(result, 1)).getLeft());
    }

    @Test
    void testDoWhile() {
        Block body = parseBlock("{ int original; do { original++; } while (original == 0); }");
        extractAndMap(body);

        Block result = runOnBlock(body);
        assertSame(replacement, getDeclaredVar(result));
        DoWhileStatement doWhileStmt = (DoWhileStatement) result.getStatements().get(1);
        assertSame(replacement, ((BinaryOperator) doWhileStmt.getCondition()).getLeft());
        assertSame(replacement, ((UnaryOperator) ((ExpressionStatement) ((Block) doWhileStmt.getBody()).getStatements().get(0)).getExpression()).getExp());
    }

    @Test
    void testOtherFeatures() {
        Block body = parseBlock("{ }");
        runOnBlock(body);
    }

    @Test
    void testSelfReference() {
        Block body = parseBlock("{ f(); }");
        runOnBlock(body);
    }

    @Test
    void testAddress() {
        Block body = parseBlock("{ try false { } catch { } }");
        runOnBlock(body);
    }

    @Test
    void testAddressComplex() throws IOException {
        // language=solidity
        String contract = """
                contract SimpleContract {
                    function f(address target) public {
                        try SimpleContract(target).g() { }
                        catch { }
                    }
                    function g() external pure {
                    }
                }""";

        ContractDeclaration contractDeclaration = getDeclStr(contract);
        Block body = parseBlock("{ try false { } catch { } }");
        runOnBlock(body);
    }

    @Test
    void testNested() {
        Block body = getNestedBody();
        extractAndMap(body);

        Block result = runOnBlock(body);
        ImmutableArray<Statement> stmRes = result.getStatements();
        assertSame(replacement, getDeclaredVar(result));
        assertSame(replacement, ((BinaryOperator) getExpression(result, 1)).getLeft());
        noReplacement(stmRes.get(2));
        noReplacement(stmRes.get(3));
    }

    @Test
    void testNestedSecond() {
        Block body = getNestedBody();
        Block innerBlock = (Block) body.getStatements().get(2);

        ProgramVariable original = getDeclaredVar(innerBlock);
        addMap(original);

        Block result = runOnBlock(body);
        ImmutableArray<Statement> stmRes = result.getStatements();
        assertSame(replacement, getDeclaredVar((Block) result.getStatements().get(2)));
        assertSame(replacement, ((BinaryOperator) getExpression((Block) result.getStatements().get(2), 1)).getLeft());
        noReplacement(stmRes.get(0));
        noReplacement(stmRes.get(1));
        noReplacement(((Block) stmRes.get(2)).getStatements().get(2));
        noReplacement(stmRes.get(3));
    }

    @Test
    void testNestedThird() {
        Block body = getNestedBody();
        Block innerBlock = (Block) ((Block) body.getStatements().get(2)).getStatements().get(2);

        ProgramVariable original = getDeclaredVar(innerBlock);
        addMap(original);

        Block result = runOnBlock(body);
        ImmutableArray<Statement> stmRes = result.getStatements();
        Block innerResult = (Block) ((Block) result.getStatements().get(2)).getStatements().get(2);
        assertSame(replacement, getDeclaredVar(innerResult));
        assertSame(replacement, ((BinaryOperator) getExpression(innerResult, 1)).getLeft());
        noReplacement(stmRes.get(0));
        noReplacement(stmRes.get(1));
        noReplacement(((Block) stmRes.get(2)).getStatements().get(0));
        noReplacement(((Block) stmRes.get(2)).getStatements().get(1));
        noReplacement(stmRes.get(3));
    }

    @Test
    void testNestedLast() {
        Block body = getNestedBody();
        Block innerBlock = (Block) body.getStatements().get(3);

        ProgramVariable original = getDeclaredVar(innerBlock);
        addMap(original);

        Block result = runOnBlock(body);
        ImmutableArray<Statement> stmRes = result.getStatements();
        assertSame(replacement, getDeclaredVar((Block) stmRes.get(3)));
        assertSame(replacement, ((BinaryOperator) getExpression((Block) stmRes.get(3), 1)).getLeft());
        noReplacement(stmRes.get(0));
        noReplacement(stmRes.get(1));
        noReplacement(stmRes.get(2));
    }

    void addMap(ProgramVariable original) {
        map.put(original, replacement);
    }

    ProgramVariable extractAndMap(Block body) {
        ProgramVariable original = getDeclaredVar(body, 0);
        addMap(original);
        return original;
    }

    Block runOnBlock(Block body) {
        ProgVarReplaceVisitor replacer = new ProgVarReplaceVisitor(body, map, false, services);
        replacer.start();
        return (Block) replacer.result();
    }

    SolidityProgramElement runOn(SolidityProgramElement element) {
        ProgVarReplaceVisitor replacer = new ProgVarReplaceVisitor(element, map, false, services);
        replacer.start();
        return replacer.result();
    }

    ProgramVariable getDeclaredVar(Block block) {
        return getDeclaredVar(block, 0);
    }

    ProgramVariable getDeclaredVar(Block block, int statementIndex) {
        return ((StatementVariableDeclaration) ((DeclarationStatement) block.getStatements().get(statementIndex)).getDeclarations().get(0)).getProgramVariable();
    }

    Expression getExpression(Block block, int index) {
        return ((ExpressionStatement) block.getStatements().get(index)).getExpression();
    }

    public Block getNestedBody() {
        return parseBlock("""
            { int original;
              original = 1;
              { int original;
                original = 2;
                { int original;
                  original = 3;
                }
              }
              { int original;
                original = 4;
              }
            }""");
    }

    void noReplacement(SyntaxElement st) {
        assertFalse(st.toString().contains("replacement"));
    }
}
