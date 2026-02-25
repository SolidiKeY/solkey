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
import org.key_project.solidity.program.ast.statement.Block;
import org.key_project.solidity.program.ast.statement.DeclarationStatement;
import org.key_project.solidity.program.ast.statement.Statement;
import org.key_project.util.collection.ImmutableArray;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.key_project.solidity.logic.parser.SolJsonParserTest.getDeclStr;

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
        replacement = new ProgramVariable(new Name("replacement"), uintKST);
    }

    void addMap(ProgramVariable original) {
        map.put(original, replacement);
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
        Expression original = new ProgramVariable(new Name("original"), uintKST); // <-
                                                                                  // here
                                                                                  // actual
                                                                                  // statement
                                                                                  // needed
        Expression replacement = new ProgramVariable(new Name("replacement"), uintKST); // <-
                                                                                        // here
                                                                                        // actual
                                                                                        // statement
                                                                                        // needed

        map.put((ProgramVariable) original, (ProgramVariable) replacement);

        ProgVarReplaceVisitor replacer = new ProgVarReplaceVisitor(original, map, false, services);
        replacer.start();
        assertEquals(replacement, replacer.result());
    }

    @Test
    void testSimpleInt() throws IOException {
        // language=solidity
        String contract = """
                contract SimpleContract {
                    function f() public pure {
                        int original;
                        original = 5;
                    }
                }""";
        ContractDeclaration contractDeclaration = getDeclStr(contract);
        DeclarationStatement dstm = (DeclarationStatement) contractDeclaration.getFunctions()
                .getFirst().getBody().getStatements().get(0);
        StatementVariableDeclaration stm =
            (StatementVariableDeclaration) dstm.getDeclarations().get(0);

        ProgramVariable original = stm.getProgramVariable();
        addMap(original);

        ProgVarReplaceVisitor replacer = new ProgVarReplaceVisitor(stm, map, false, services);
        replacer.start();
        ProgramVariable result =
            ((StatementVariableDeclaration) replacer.result()).getProgramVariable();
        assertSame(replacement, result);
    }

    @Test
    void testWholeBody() throws IOException {
        // language=solidity
        String contract = """
                contract SimpleContract {
                    function f() public pure {
                        int original;
                        original = 5;
                    }
                }""";
        ContractDeclaration contractDeclaration = getDeclStr(contract);
        Block body = contractDeclaration.getFunctions().getFirst().getBody();
        DeclarationStatement dstm = (DeclarationStatement) body.getStatements().get(0);
        StatementVariableDeclaration stm =
            (StatementVariableDeclaration) dstm.getDeclarations().get(0);

        ProgramVariable original = stm.getProgramVariable();
        addMap(original);

        ProgVarReplaceVisitor replacer = new ProgVarReplaceVisitor(body, map, false, services);
        replacer.start();
        Block result = ((Block) replacer.result());
        ProgramVariable progRes =
            (ProgramVariable) result.getChild(1).getChild(0).getChild(0);
        assertSame(replacement, progRes);
    }

    @Test
    void testArray() throws IOException {
        // language=solidity
        String contract = """
                contract SimpleContract {
                    function f() public pure {
                        int256[10] memory original;
                        original[1] = 1;
                    }
                }""";
        ContractDeclaration contractDeclaration = getDeclStr(contract);
        Block body = contractDeclaration.getFunctions().getFirst().getBody();

        ProgramVariable original =
            (ProgramVariable) body.getStatements().get(0).getChild(0).getChild(0);
        addMap(original);
        ProgVarReplaceVisitor replacer = new ProgVarReplaceVisitor(body, map, false, services);
        replacer.start();
        SolidityProgramElement result = replacer.result();

        assertEquals(replacement, result.getChild(0).getChild(0).getChild(0));
        assertEquals(replacement, result.getChild(1).getChild(0).getChild(0).getChild(0));
    }

    @Test
    void testStruct() throws IOException {
        // language=solidity
        String contract = """
                contract SimpleContract {
                    struct Person {
                       int age;
                    }

                    function f() public pure {
                        Person memory alice;
                    }
                }""";
        ContractDeclaration contractDeclaration = getDeclStr(contract);
        DeclarationStatement dstm = (DeclarationStatement) contractDeclaration.getFunctions()
                .getFirst().getBody().getStatements().get(0);
        StatementVariableDeclaration stm =
            (StatementVariableDeclaration) dstm.getDeclarations().get(0);

        ProgramVariable original = stm.getProgramVariable();
        addMap(original);

        ProgVarReplaceVisitor replacer = new ProgVarReplaceVisitor(stm, map, false, services);
        replacer.start();
        StatementVariableDeclaration result = (StatementVariableDeclaration) replacer.result();
        ProgramVariable resultPV = result.getProgramVariable();
        assertEquals(replacement, resultPV);
    }

    @Test
    void testEnum() throws IOException {
        // language=solidity
        String contract = """
                contract SimpleContract {
                    enum State {
                        Begin,
                        End
                    }
                    function f() public {
                        State s = State.Begin;
                        s = State.End;
                    }
                }""";
        ContractDeclaration contractDeclaration = getDeclStr(contract);
        Block body = contractDeclaration.getFunctions().getFirst().getBody();
        DeclarationStatement dstm = (DeclarationStatement) body.getStatements().get(0);
        StatementVariableDeclaration stm =
            (StatementVariableDeclaration) dstm.getDeclarations().get(0);

        ProgramVariable original = stm.getProgramVariable();
        addMap(original);

        ProgVarReplaceVisitor replacer = new ProgVarReplaceVisitor(body, map, false, services);
        replacer.start();
        Block result = (Block) replacer.result();
        assertEquals(replacement, result.getChild(0).getChild(0).getChild(1));
        assertEquals(replacement, result.getChild(1).getChild(0).getChild(0));
    }

    @Test
    void testFor() throws IOException {
        // language=solidity
        String contract = """
                contract SimpleContract {
                    function f() public pure {
                        int original;
                        for(original = 0; original<10; original++){}
                    }
                }""";
        ContractDeclaration contractDeclaration = getDeclStr(contract);
        Block body = contractDeclaration.getFunctions().getFirst().getBody();
        DeclarationStatement dstm = (DeclarationStatement) body.getStatements().get(0);
        StatementVariableDeclaration stm =
            (StatementVariableDeclaration) dstm.getDeclarations().get(0);

        ProgramVariable original = stm.getProgramVariable();
        addMap(original);

        ProgVarReplaceVisitor replacer = new ProgVarReplaceVisitor(body, map, false, services);
        replacer.start();
        Block result = ((Block) replacer.result());
        assertSame(replacement, result.getChild(0).getChild(0).getChild(1));
        Statement forLoop = result.getStatements().get(1);
        assertSame(replacement, forLoop.getChild(0).getChild(0).getChild(0));
        assertSame(replacement, forLoop.getChild(1).getChild(0));
        assertSame(replacement, forLoop.getChild(2).getChild(0).getChild(0));
    }

    @Test
    void testMultipleFeatures() throws IOException {
        // language=solidity
        String contract = """
                contract SimpleContract {
                    function f() public pure {
                        int original;
                        if(original == 2)
                            original = 0;
                        else
                            original = 1;
                        while(original == 0) original = 1;
                        original++;
                        original += 1;
                        do { original++; } while (original == 0);
                    }
                }""";
        ContractDeclaration contractDeclaration = getDeclStr(contract);
        Block body = contractDeclaration.getFunctions().getFirst().getBody();
        DeclarationStatement dstm = (DeclarationStatement) body.getStatements().get(0);
        StatementVariableDeclaration stm =
            (StatementVariableDeclaration) dstm.getDeclarations().get(0);

        ProgramVariable original = stm.getProgramVariable();
        addMap(original);

        ProgVarReplaceVisitor replacer = new ProgVarReplaceVisitor(body, map, false, services);
        replacer.start();
        Block result = ((Block) replacer.result());
        assertFalse(result.toString().contains("original"));
        assertTrue(result.toString().contains("replacement"));
    }

    @Test
    void testOtherFeatures() throws IOException {
        // language=solidity
        String contract = """
                contract SimpleContract {
                    modifier mod1(){
                        _;
                    }
                    modifier mod2(){
                        _;
                    }
                    function f(address target) public mod1 mod2 {
                    }
                }""";
        ContractDeclaration contractDeclaration = getDeclStr(contract);
        Block body = contractDeclaration.getFunctions().getFirst().getBody();

        ProgVarReplaceVisitor replacer = new ProgVarReplaceVisitor(body, map, false, services);
        replacer.start();
    }

    @Test
    void testSelfReference() throws IOException {
        // language=solidity
        String contract = """
                contract SimpleContract {
                    function f() public {
                        f();
                    }
                }""";
        ContractDeclaration contractDeclaration = getDeclStr(contract);
        Block body = contractDeclaration.getFunctions().getFirst().getBody();
        ProgVarReplaceVisitor replacer = new ProgVarReplaceVisitor(body, map, false, services);
        replacer.start();
        Block result = ((Block) replacer.result());
    }

    @Test
    void testAddress() throws IOException {
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
        Block body = contractDeclaration.getFunctions().getFirst().getBody();
        ProgVarReplaceVisitor replacer = new ProgVarReplaceVisitor(body, map, false, services);
        replacer.start();
        Block result = ((Block) replacer.result());
    }

    @Test
    void testNested() throws IOException {
        Block body = getNestedBody();
        DeclarationStatement dstm = (DeclarationStatement) body.getStatements().get(0);
        StatementVariableDeclaration stm =
            (StatementVariableDeclaration) dstm.getDeclarations().get(0);

        ProgramVariable original = stm.getProgramVariable();
        addMap(original);

        ProgVarReplaceVisitor replacer = new ProgVarReplaceVisitor(body, map, false, services);
        replacer.start();
        Block result = ((Block) replacer.result());

        ImmutableArray<Statement> stmRes = result.getStatements();
        assertSame(replacement, stmRes.get(1).getChild(0).getChild(0));
        noReplacement(stmRes.get(2));
        noReplacement(stmRes.get(3));
    }

    @Test
    void testNestedSecond() throws IOException {
        Block body = getNestedBody();

        ProgramVariable original =
            (ProgramVariable) body.getChild(2).getChild(0).getChild(0).getChild(1);
        addMap(original);

        ProgVarReplaceVisitor replacer = new ProgVarReplaceVisitor(body, map, false, services);
        replacer.start();
        Block result = ((Block) replacer.result());

        ImmutableArray<Statement> stmRes = result.getStatements();
        assertSame(replacement, result.getChild(2).getChild(1).getChild(0).getChild(0));
        noReplacement(stmRes.get(0));
        noReplacement(stmRes.get(1));
        noReplacement(stmRes.get(2).getChild(2));
        noReplacement(stmRes.get(3));
    }

    @Test
    void testNestedThird() throws IOException {
        Block body = getNestedBody();

        ProgramVariable original =
            (ProgramVariable) body.getChild(2).getChild(2).getChild(0).getChild(0).getChild(1);
        addMap(original);

        ProgVarReplaceVisitor replacer = new ProgVarReplaceVisitor(body, map, false, services);
        replacer.start();
        Block result = ((Block) replacer.result());

        ImmutableArray<Statement> stmRes = result.getStatements();
        assertSame(replacement, result.getChild(2).getChild(2).getChild(1).getChild(0).getChild(0));
        noReplacement(stmRes.get(0));
        noReplacement(stmRes.get(1));
        noReplacement(stmRes.get(2).getChild(0));
        noReplacement(stmRes.get(2).getChild(1));
        noReplacement(stmRes.get(3));
    }

    @Test
    void testNestedLast() throws IOException {
        Block body = getNestedBody();

        ProgramVariable original =
            (ProgramVariable) body.getChild(3).getChild(0).getChild(0).getChild(1);
        addMap(original);

        ProgVarReplaceVisitor replacer = new ProgVarReplaceVisitor(body, map, false, services);
        replacer.start();
        Block result = ((Block) replacer.result());

        ImmutableArray<Statement> stmRes = result.getStatements();
        assertSame(replacement, stmRes.get(3).getChild(1).getChild(0).getChild(0));
        noReplacement(stmRes.get(0));
        noReplacement(stmRes.get(1));
        noReplacement(stmRes.get(2));
    }

    public Block getNestedBody() throws IOException {
        // language=solidity
        String contract = """
                contract SimpleContract {
                    function f() public pure {
                        int original;
                        original = 1;
                        {
                            int original;
                            original = 2;
                            {
                                int original;
                                original = 3;
                            }
                        }
                        {
                            int original;
                            original = 4;
                        }
                    }
                }""";

        ContractDeclaration contractDeclaration = getDeclStr(contract);
        return contractDeclaration.getFunctions().getFirst().getBody();
    }

    void noReplacement(SyntaxElement st) {
        assertFalse(st.toString().contains("replacement"));
    }
}
