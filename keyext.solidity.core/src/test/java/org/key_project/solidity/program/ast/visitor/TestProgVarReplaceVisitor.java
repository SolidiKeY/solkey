/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.visitor;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.key_project.logic.Name;
import org.key_project.logic.sort.Sort;
import org.key_project.solidity.common.Services;
import org.key_project.solidity.logic.op.ProgramVariable;
import org.key_project.solidity.logic.sort.SortImpl;
import org.key_project.solidity.program.ast.abstractions.KeYSolidityType;
import org.key_project.solidity.program.ast.abstractions.PrimitiveType;
import org.key_project.solidity.program.ast.declarations.ArrayDeclaration;
import org.key_project.solidity.program.ast.declarations.ContractDeclaration;
import org.key_project.solidity.program.ast.declarations.StatementVariableDeclaration;
import org.key_project.solidity.program.ast.expressions.Expression;
import org.key_project.solidity.program.ast.statement.Block;
import org.key_project.solidity.program.ast.statement.DeclarationStatement;
import org.key_project.solidity.program.ast.statement.Statement;

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

        ProgramVariable original = stm.programVariable;
        addMap(original);

        ProgVarReplaceVisitor replacer = new ProgVarReplaceVisitor(stm, map, false, services);
        replacer.start();
        ProgramVariable result = ((StatementVariableDeclaration) replacer.result()).programVariable;
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
        DeclarationStatement dstm = (DeclarationStatement) contractDeclaration.getFunctions()
                .getFirst().getBody().getStatements().get(0);
        StatementVariableDeclaration stm =
            (StatementVariableDeclaration) dstm.getDeclarations().get(0);

        ProgramVariable original = stm.programVariable;
        addMap(original);

        Block body = contractDeclaration.getFunctions().getFirst().getBody();
        ProgVarReplaceVisitor replacer = new ProgVarReplaceVisitor(body, map, false, services);
        replacer.start();
        Block result = ((Block) replacer.result());
        ProgramVariable progRes =
            (ProgramVariable) result.getChild(1).getChild(0).getChild(0).getChild(0).getChild(1);
        assertSame(replacement, progRes);
    }

    @Test
    void testArray() throws IOException {
        // language=solidity
        String contract = """
                contract SimpleContract {
                    function f() public pure {
                        int[10] memory original;
                    }
                }""";
        ContractDeclaration contractDeclaration = getDeclStr(contract);
        DeclarationStatement dstm = (DeclarationStatement) contractDeclaration.getFunctions()
                .getFirst().getBody().getStatements().get(0);
        ArrayDeclaration stm = (ArrayDeclaration) dstm.getDeclarations().get(0);

        ProgramVariable original = stm.programVariable;
        addMap(original);

        ProgVarReplaceVisitor replacer = new ProgVarReplaceVisitor(stm, map, false, services);
        replacer.start();
        ArrayDeclaration result = ((ArrayDeclaration) replacer.result());
        ProgramVariable resultPV = result.programVariable;
        assertEquals(replacement, resultPV);
        assertEquals(10, result.length);
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

        ProgramVariable original = stm.programVariable;
        addMap(original);

        ProgVarReplaceVisitor replacer = new ProgVarReplaceVisitor(stm, map, false, services);
        replacer.start();
        StatementVariableDeclaration result = (StatementVariableDeclaration) replacer.result();
        ProgramVariable resultPV = result.programVariable;
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
                    }
                }""";
        ContractDeclaration contractDeclaration = getDeclStr(contract);
        DeclarationStatement dstm = (DeclarationStatement) contractDeclaration.getFunctions()
                .getFirst().getBody().getStatements().get(0);
        StatementVariableDeclaration stm =
            (StatementVariableDeclaration) dstm.getDeclarations().get(0);

        ProgramVariable original = stm.programVariable;
        addMap(original);

        ProgVarReplaceVisitor replacer = new ProgVarReplaceVisitor(stm, map, false, services);
        replacer.start();
        StatementVariableDeclaration result = (StatementVariableDeclaration) replacer.result();
        ProgramVariable resultPV = result.programVariable;
        assertEquals(replacement, resultPV);
    }
}
