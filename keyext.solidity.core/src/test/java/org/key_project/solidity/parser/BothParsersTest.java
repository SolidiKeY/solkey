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
import org.key_project.solidity.program.ast.statement.DeclarationStatement;
import org.key_project.solidity.program.ast.statement.Statement;
import org.key_project.solidity.program.parser.SolJSONParser;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.key_project.solidity.parser.ParserForTesting.parse;
import static org.key_project.solidity.program.parser.SolcWrapper.getDeclStrJsonParser;

public class BothParsersTest {

    Services services;
    static SolidityToKeyConverter stk;;
    SolJSONParser jsonParser;

    public BothParsersTest() throws IOException {
        services = new Services();
        stk = new SolidityToKeyConverter(services, new Namespace<>(), new Namespace<>());
        jsonParser = new SolJSONParser(services);
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
        getDeclStrJsonParser(jsonParser, contract);
    }

    static public Statement parseStatement(String s) {
        SolidityParser parser = parse(s);
        StatementContext stmCtx = parser.statement();
        return (Statement) stk.visitStatement(stmCtx);
    }

    @Test
    void usingVariable() {
        DeclarationStatement stm = (DeclarationStatement) parseStatement("Person alice;");
        ProgramVariable programVariable = (ProgramVariable) stm.getChild(0).getChild(1);
        Sort sort = programVariable.getType().getSort(services);
        Assertions.assertEquals("Person", sort.toString());
    }

}
