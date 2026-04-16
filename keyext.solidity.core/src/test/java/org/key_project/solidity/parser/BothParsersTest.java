/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.parser;

import java.io.IOException;

import org.key_project.logic.Namespace;
import org.key_project.logic.sort.Sort;
import org.key_project.solidity.common.Services;
import org.key_project.solidity.logic.op.ProgramVariable;
import org.key_project.solidity.parser.SolidityParser.*;
import org.key_project.solidity.program.ast.abstractions.ArrayType;
import org.key_project.solidity.program.ast.abstractions.DynamicArrayType;
import org.key_project.solidity.program.ast.abstractions.TupleType;
import org.key_project.solidity.program.ast.abstractions.Type;
import org.key_project.solidity.program.ast.declarations.ContractDeclaration;
import org.key_project.solidity.program.ast.declarations.StateVariableDeclaration;
import org.key_project.solidity.program.ast.declarations.StatementVariableDeclaration;
import org.key_project.solidity.program.ast.statement.DeclarationStatement;
import org.key_project.solidity.program.ast.statement.ReturnStatement;
import org.key_project.solidity.program.ast.statement.Statement;
import org.key_project.solidity.program.parser.SolcParser;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.key_project.solidity.parser.ParserForTesting.parse;

public class BothParsersTest {

    Services services;
    static SolidityToKeyConverter stk;;
    SolcParser solcParser;

    public BothParsersTest() throws IOException {
        services = new Services();
        stk = new SolidityToKeyConverter(services, new Namespace<>(), new Namespace<>(), new Namespace<>());
        solcParser = new SolcParser(services);
        addContractToService();
    }

    void addContractToService() throws IOException {
        // language=solidity
        String contract = """
                contract SimpleContract {
                    struct Person {
                       uint256 age;
                    }
                    Person alice;
                }""";
        solcParser.getDeclStrJsonParser(contract);
    }

    static public Statement parseStatement(String s) {
        SolidityParser parser = parse(s);
        StatementContext stmCtx = parser.statement();
        return (Statement) stk.visitStatement(stmCtx);
    }

    @Test
    void usingVariable() {
        DeclarationStatement stm = (DeclarationStatement) parseStatement("Person alice;");
        ProgramVariable programVariable = ((StatementVariableDeclaration) stm.getDeclarations().get(0)).getProgramVariable();
        Sort sort = programVariable.getType().getSort(services);
        assertEquals("Person", sort.toString());
    }

    @Test
    void sameTupleType() throws IOException {
        // language=solidity
        String contract = """
                contract AnotherContract {
                    function f() public returns (bool, bool) {
                        return (false, false);
                    }
                }""";
        ContractDeclaration contractDec = solcParser.getDeclStrJsonParser(contract);
        TupleType contractTupleType = contractDec.getFunctions().getFirst().getType();

        ReturnStatement returnStm = (ReturnStatement) parseStatement("return (false, false);");
        TupleType parsedTupleType = (TupleType) returnStm.getReturnExp().getType();

        assertSame(contractTupleType, parsedTupleType);
    }

    @Test
    void sameBoolType() throws IOException {
        // language=solidity
        String contract = """
                contract BoolContract {
                    function f() public returns (bool) {}
                }""";
        ContractDeclaration contractDec = solcParser.getDeclStrJsonParser(contract);
        Type contractBoolType = contractDec.getFunctions().getFirst().getReturnParameters().get(0).getType();

        DeclarationStatement stm = (DeclarationStatement) parseStatement("bool x;");
        Type parsedBoolType = ((StatementVariableDeclaration) stm.getDeclarations().get(0)).getProgramVariable().getType();

        assertSame(contractBoolType, parsedBoolType);
    }

    @Test
    void sameStaticArrayType() throws IOException {
        // language=solidity
        String contract = """
                contract StaticArrayContract {
                    bool[2] x;
                }""";
        ContractDeclaration contractDec = solcParser.getDeclStrJsonParser(contract);
        StateVariableDeclaration field = contractDec.getFieldDeclarations().get(0);
        ArrayType contractArrayType = (ArrayType) field.getProgramVariable().getType();

        DeclarationStatement stm = (DeclarationStatement) parseStatement("bool[2] x;");
        ArrayType parsedArrayType = (ArrayType) ((StatementVariableDeclaration) stm.getDeclarations().get(0)).getProgramVariable().getType();

        assertSame(contractArrayType, parsedArrayType);
    }

    @Test
    void sameDynamicArrayType() throws IOException {
        // language=solidity
        String contract = """
                contract DynamicArrayContract {
                    bool[] x;
                }""";
        ContractDeclaration contractDec = solcParser.getDeclStrJsonParser(contract);
        StateVariableDeclaration field = contractDec.getFieldDeclarations().get(0);
        DynamicArrayType contractArrayType = (DynamicArrayType) field.getProgramVariable().getType();

        DeclarationStatement stm = (DeclarationStatement) parseStatement("bool[] x;");
        DynamicArrayType parsedArrayType = (DynamicArrayType) ((StatementVariableDeclaration) stm.getDeclarations().get(0)).getProgramVariable().getType();

        assertSame(contractArrayType, parsedArrayType);
    }

}
