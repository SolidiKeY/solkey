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
import org.key_project.solidity.program.ast.declarations.ContractDeclaration;
import org.key_project.solidity.program.ast.declarations.StateVariableDeclaration;
import org.key_project.solidity.program.ast.expressions.SoliditiyExpression;
import org.key_project.solidity.program.ast.statement.DeclarationStatement;
import org.key_project.solidity.program.ast.statement.Statement;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.key_project.solidity.logic.parser.SolJsonParserTest.getDeclStr;

class TestProgVarReplaceVisitor {

    @Test
    void testNoReplacement() {
        Map<ProgramVariable, ProgramVariable> map = new HashMap<>();
        Services services = new Services();
        // parse in statements
        Statement stmnt = null; // <- here actual statement needed
        ProgVarReplaceVisitor replacer = new ProgVarReplaceVisitor(stmnt, map, false, services);
        replacer.start();
        assertEquals(stmnt, replacer.result()); // stmnt.equals(repl.result())
        assertSame(stmnt, replacer.result()); // stmnt == repl.result()
    }


    @Test
    void testReplacement() {
        Map<ProgramVariable, ProgramVariable> map = new HashMap<>();
        Services services = new Services();
        // parse in statements
        final Sort uint = new SortImpl(new Name("uint"), false);
        KeYSolidityType uintKST = new KeYSolidityType(PrimitiveType.UINT, uint);
        services.getNamespaces().sorts().add(uint);
        SoliditiyExpression original = new ProgramVariable(new Name("original"), uintKST); // <-
                                                                                           // here
                                                                                           // actual
                                                                                           // statement
                                                                                           // needed
        SoliditiyExpression replacement = new ProgramVariable(new Name("replacement"), uintKST); // <-
                                                                                                 // here
                                                                                                 // actual
                                                                                                 // statement
                                                                                                 // needed

        map.put((ProgramVariable) original, (ProgramVariable) replacement);

        ProgVarReplaceVisitor replacer = new ProgVarReplaceVisitor(original, map, false, services);
        replacer.start();
        assertEquals(replacement, replacer.result()); // stmnt.equals(repl.result())
    }

    @Test
    void testContractReplacement() throws IOException {
        Map<ProgramVariable, ProgramVariable> map = new HashMap<>();
        Services services = new Services();
        // parse in statements
        final Sort uint = new SortImpl(new Name("uint"), false);
        KeYSolidityType uintKST = new KeYSolidityType(PrimitiveType.UINT, uint);
        services.getNamespaces().sorts().add(uint);

        // language=solidity
        String contract = """
                contract SimpleContract {
                   uint256 balance;
                }""";
        ContractDeclaration contractDeclaration = getDeclStr(contract);

        StateVariableDeclaration original =
            contractDeclaration.getFieldDeclarations().toList().getFirst();
        StateVariableDeclaration replacement = new StateVariableDeclaration(new Name("replacement"),
            null, null, null);
        // map.put(original, replacement);
        //
        // ProgVarReplaceVisitor replacer = new ProgVarReplaceVisitor(original, map, false,
        // services);
        // replacer.start();
        // assertEquals(replacement, replacer.result()); // stmnt.equals(repl.result())
    }

    @Test
    void testFunction() throws IOException {
        Map<ProgramVariable, ProgramVariable> map = new HashMap<>();
        Services services = new Services();
        // parse in statements
        final Sort uint = new SortImpl(new Name("uint"), false);
        KeYSolidityType uintKST = new KeYSolidityType(PrimitiveType.UINT, uint);
        services.getNamespaces().sorts().add(uint);

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
        StateVariableDeclaration original =
            (StateVariableDeclaration) dstm.getDeclarations().getFirst();
        StateVariableDeclaration replacement =
            new StateVariableDeclaration(new Name("replacement"), null, null);

        // map.put(original, replacement);
        //
        // ProgVarReplaceVisitor replacer = new ProgVarReplaceVisitor(original, map, false,
        // services);
        // replacer.start();
        // assertEquals(replacement, replacer.result()); // stmnt.equals(repl.result())
    }
}
