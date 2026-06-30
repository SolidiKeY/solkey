/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.visitor;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.key_project.logic.*;
import org.key_project.logic.sort.Sort;
import org.key_project.solidity.common.Services;
import org.key_project.solidity.logic.op.ProgramVariable;
import org.key_project.solidity.logic.sort.SortImpl;
import org.key_project.solidity.program.ast.SolidityProgramElement;
import org.key_project.solidity.program.ast.abstractions.KeYSolidityType;
import org.key_project.solidity.program.ast.declarations.*;
import org.key_project.solidity.program.ast.declarations.FunctionEnums.DataLocation;
import org.key_project.solidity.program.ast.expressions.*;
import org.key_project.solidity.program.ast.expressions.operators.*;
import org.key_project.solidity.program.ast.statement.*;
import org.key_project.util.collection.ImmutableArray;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.key_project.solidity.parser.ParserForTesting.*;
import static org.key_project.solidity.program.ast.abstractions.PrimitiveType.UINT;
import static org.key_project.solidity.program.parser.SolcParserNoServices.getDeclStr;

public class ProgVarReplaceVisitorTest {

    private final Services services;
    private final Map<ProgramVariable, ProgramVariable> map;
    private final KeYSolidityType uintKST;
    private final ProgramVariable replacement;

    public ProgVarReplaceVisitorTest() {
        map = new HashMap<>();
        services = new Services();

        final Sort uint = new SortImpl(new Name("uint"), false);
        uintKST = new KeYSolidityType(UINT, uint);
        services.getNamespaces().sorts().add(uint);
        replacement = new ProgramVariable(new Name("replacement"), uintKST, DataLocation.Default);
    }

    @Test
    void testReplacement() {
        ProgramVariable original =
            new ProgramVariable(new Name("original"), uintKST, DataLocation.Default);
        ProgramVariable replacement =
            new ProgramVariable(new Name("replacement"), uintKST, DataLocation.Default);

        map.put(original, replacement);

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
        assertSame(replacement, ((AssignExpression) getExpression(result, 1)).getLeft());
    }

    @Test
    void testBinaryExpressionReplacesAllOccurrences() {
        Block body = parseBlock("{ int original; original = original + original; }");
        extractAndMap(body);

        Block result = runOnBlock(body);
        assertSame(replacement, getDeclaredVar(result));
        AssignExpression assign = (AssignExpression) getExpression(result, 1);
        assertSame(replacement, assign.getLeft());
        BinaryExpression sum = (BinaryExpression) assign.getRight();
        assertSame(replacement, sum.getLeft());
        assertSame(replacement, sum.getRight());
    }

    @Test
    void testOnlyMappedVariableIsReplaced() {
        Block body = parseBlock("{ int a; int original; a = original; }");
        ProgramVariable original = getDeclaredVar(body, 1);
        addMap(original); // map only 'original', leave 'a' untouched

        Block result = runOnBlock(body);
        noReplacement(result.getStatements().get(0)); // declaration of 'a' unchanged
        assertSame(replacement, getDeclaredVar(result, 1)); // 'original' declaration replaced
        AssignExpression assign = (AssignExpression) getExpression(result, 2);
        assertNotSame(replacement, assign.getLeft()); // lhs 'a' untouched
        assertSame(replacement, assign.getRight()); // rhs 'original' replaced
    }

    @Test
    @Disabled("array types are not yet constructed on demand for fragments — task #10")
    void testArray() {
        Block body = parseBlock("{ int256[10] memory original; original[1] = 1; }");
        extractAndMap(body);

        Block result = runOnBlock(body);
        assertEquals(replacement, getDeclaredVar(result));
        assertEquals(replacement,
            ((IndexExpression) ((AssignExpression) getExpression(result, 1)).getLeft())
                    .getLeftExp());
    }

    @Test
    void testStruct() {
        DeclarationStatement dstm =
            (DeclarationStatement) parseStatement("Person memory original;");
        StatementVariableDeclaration stm =
            (StatementVariableDeclaration) dstm.getDeclarations().get(0);

        ProgramVariable original = stm.getProgramVariable();
        addMap(original);

        StatementVariableDeclaration result = (StatementVariableDeclaration) runOn(stm);
        assertEquals(replacement, result.getProgramVariable());
    }

    @Test
    @Disabled("user-defined type (enum) not resolvable in a contract-less fragment — task #10")
    void testEnum() {
        Block body = parseBlock("{ State s; s = s; }");
        extractAndMap(body);

        Block result = runOnBlock(body);
        assertEquals(replacement, getDeclaredVar(result));
        assertEquals(replacement, ((AssignExpression) getExpression(result, 1)).getLeft());
    }

    @Test
    void testFor() {
        Block body = parseBlock("{ int original; for(original = 0; original<10; original++){} }");
        extractAndMap(body);

        Block result = runOnBlock(body);
        assertSame(replacement, getDeclaredVar(result));
        Statement forLoop = result.getStatements().get(1);
        assertSame(replacement,
            ((AssignExpression) Objects.requireNonNull(((ForStatement) forLoop).getInit())
                    .getInit()).getLeft());
        assertSame(replacement,
            ((BinaryExpression) Objects.requireNonNull(((ForStatement) forLoop).getCondition()))
                    .getLeft());
        assertSame(replacement,
            ((UnaryExpression) Objects.requireNonNull(((ForStatement) forLoop).getUpdate())
                    .getUpdate()).getExp());
    }

    @Test
    void testIf() {
        Block body =
            parseBlock("{ int original; if(original == 2) original = 0; else original = 1; }");
        extractAndMap(body);

        Block result = runOnBlock(body);
        assertSame(replacement, getDeclaredVar(result));
        ConditionStatement ifStmt = (ConditionStatement) result.getStatements().get(1);
        assertSame(replacement, ((BinaryExpression) ifStmt.getCondition()).getLeft());
        assertSame(replacement,
            ((AssignExpression) ((ExpressionStatement) ifStmt.getThenBody()).getExpression())
                    .getLeft());
        assertSame(replacement,
            ((AssignExpression) ((ExpressionStatement) Objects.requireNonNull(ifStmt.getElseBody()))
                    .getExpression()).getLeft());
    }

    @Test
    void testWhile() {
        Block body = parseBlock("{ int original; while(original == 0) original = 1; }");
        extractAndMap(body);

        Block result = runOnBlock(body);
        assertSame(replacement, getDeclaredVar(result));
        WhileStatement whileStmt = (WhileStatement) result.getStatements().get(1);
        assertSame(replacement,
            ((BinaryExpression) Objects.requireNonNull(whileStmt.getCondition())).getLeft());
        assertSame(replacement,
            ((AssignExpression) ((ExpressionStatement) whileStmt.getBody()).getExpression())
                    .getLeft());
    }

    @Test
    void testUnaryIncrement() {
        Block body = parseBlock("{ int original; original++; }");
        extractAndMap(body);

        Block result = runOnBlock(body);
        assertSame(replacement, getDeclaredVar(result));
        assertSame(replacement, ((UnaryExpression) getExpression(result, 1)).getExp());
    }

    @Test
    void testCompoundAssignment() {
        Block body = parseBlock("{ int original; original += 1; }");
        extractAndMap(body);

        Block result = runOnBlock(body);
        assertSame(replacement, getDeclaredVar(result));
        assertSame(replacement, ((AssignExpression) getExpression(result, 1)).getLeft());
    }

    @Test
    void testDoWhile() {
        Block body = parseBlock("{ int original; do { original++; } while (original == 0); }");
        extractAndMap(body);

        Block result = runOnBlock(body);
        assertSame(replacement, getDeclaredVar(result));
        DoWhileStatement doWhileStmt = (DoWhileStatement) result.getStatements().get(1);
        assertSame(replacement,
            ((BinaryExpression) Objects.requireNonNull(doWhileStmt.getCondition())).getLeft());
        assertSame(replacement,
            ((UnaryExpression) ((ExpressionStatement) ((Block) doWhileStmt.getBody())
                    .getStatements()
                    .get(0)).getExpression()).getExp());
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
    void testReturn() {
        Block body = parseBlock("{ int original; return original; }");
        extractAndMap(body);
        Block result = runOnBlock(body);
        assertSame(replacement, getDeclaredVar(result));
        ReturnStatement ret = (ReturnStatement) result.getStatements().get(1);
        assertSame(replacement, Objects.requireNonNull(ret.getReturnExp()));
    }

    @Test
    void testTernary() {
        Block body = parseBlock("{ int original; original == 0 ? original : 1; }");
        extractAndMap(body);
        Block result = runOnBlock(body);
        assertSame(replacement, getDeclaredVar(result));
        TernaryExpression ternary = (TernaryExpression) getExpression(result, 1);
        assertSame(replacement, ((BinaryExpression) ternary.getCondition()).getLeft());
        assertSame(replacement, ternary.getTrueExpression());
    }

    @Test
    @Disabled("array types are not yet constructed on demand for fragments — task #10")
    void testSliceAccess() {
        Block body = parseBlock("{ int[] memory arr; int original; arr[original:5]; }");
        ProgramVariable original = getDeclaredVar(body, 1);
        addMap(original);
        Block result = runOnBlock(body);
        IndexRangeExpression slice = (IndexRangeExpression) getExpression(result, 2);
        assertSame(replacement, Objects.requireNonNull(slice.getStartExp()));
    }

    @Test
    void testBreak() {
        Block body = parseBlock("{ int original; while(original == 0) { break; } }");
        extractAndMap(body);
        Block result = runOnBlock(body);
        assertSame(replacement, getDeclaredVar(result));
        WhileStatement whileStmt = (WhileStatement) result.getStatements().get(1);
        assertSame(replacement,
            ((BinaryExpression) Objects.requireNonNull(whileStmt.getCondition())).getLeft());
    }

    @Test
    void testContinue() {
        Block body = parseBlock("{ int original; while(original == 0) { continue; } }");
        extractAndMap(body);
        Block result = runOnBlock(body);
        assertSame(replacement, getDeclaredVar(result));
        WhileStatement whileStmt = (WhileStatement) result.getStatements().get(1);
        assertSame(replacement,
            ((BinaryExpression) Objects.requireNonNull(whileStmt.getCondition())).getLeft());
    }

    @Test
    void testFunctionCallWithArgs() {
        Block body = parseBlock("{ int original; f(original); }");
        extractAndMap(body);
        Block result = runOnBlock(body);
        assertSame(replacement, getDeclaredVar(result));
        FunctionCallExpression call = (FunctionCallExpression) getExpression(result, 1);
        assertSame(replacement, call.getArgument(0));
    }

    @Test
    @Disabled("array types are not yet constructed on demand for fragments — task #10")
    void testNewExpression() {
        Block body = parseBlock("{ int original; int[] memory arr = new int[](original); }");
        extractAndMap(body);

        Block result = runOnBlock(body);
        assertSame(replacement, getDeclaredVar(result));
        DeclarationStatement arrDecl = (DeclarationStatement) result.getStatements().get(1);
        FunctionCallExpression newCall =
            (FunctionCallExpression) Objects.requireNonNull(arrDecl.getInitialValue());
        assertSame(replacement, newCall.getArgument(0));
        assertInstanceOf(NewExpression.class, newCall.getFunctionExp());
    }

    @Test
    void testAddress() {
        Block body = parseBlock("{ try false { } catch { } }");
        runOnBlock(body);
    }

    @Test
    @Disabled("try with external call (SimpleContract(target).g()) not yet supported — task #10")
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

        ContractDeclaration contractDeclaration = getDeclStr(contract, services);
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
        assertSame(replacement, ((AssignExpression) getExpression(result, 1)).getLeft());
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
        assertSame(replacement,
            ((AssignExpression) getExpression((Block) result.getStatements().get(2), 1)).getLeft());
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
        assertSame(replacement, ((AssignExpression) getExpression(innerResult, 1)).getLeft());
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
        assertSame(replacement,
            ((AssignExpression) getExpression((Block) stmRes.get(3), 1)).getLeft());
        noReplacement(stmRes.get(0));
        noReplacement(stmRes.get(1));
        noReplacement(stmRes.get(2));
    }

    void addMap(ProgramVariable original) {
        map.put(original, replacement);
    }

    void extractAndMap(Block body) {
        ProgramVariable original = getDeclaredVar(body, 0);
        addMap(original);
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
        return ((StatementVariableDeclaration) ((DeclarationStatement) block.getStatements()
                .get(statementIndex)).getDeclarations().get(0)).getProgramVariable();
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
