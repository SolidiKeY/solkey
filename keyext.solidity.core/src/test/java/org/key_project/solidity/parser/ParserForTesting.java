/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.parser;

import org.key_project.logic.Name;
import org.key_project.logic.Namespace;
import org.key_project.logic.sort.Sort;
import org.key_project.solidity.common.Services;
import org.key_project.solidity.logic.op.ProgramVariable;
import org.key_project.solidity.logic.sort.SortImpl;
import org.key_project.solidity.parser.SolidityParser.*;
import org.key_project.solidity.program.ast.abstractions.KeYSolidityType;
import org.key_project.solidity.program.ast.abstractions.PrimitiveType;
import org.key_project.solidity.program.ast.abstractions.StructType;
import org.key_project.solidity.program.ast.expressions.Expression;
import org.key_project.solidity.program.ast.statement.Block;
import org.key_project.solidity.program.ast.statement.Statement;
import org.key_project.solidity.rule.sv.ProgramSV;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CodePointCharStream;
import org.antlr.v4.runtime.CommonTokenStream;

import static org.key_project.solidity.rule.sv.SchemaVariableFactory.createProgramSV;

public class ParserForTesting {

    static SolidityToKeyConverter stk = solConverter();

    private static SolidityToKeyConverter solConverter() {
        Services services = new Services();

        KeYSolidityType ksType = new KeYSolidityType(PrimitiveType.UINT, new SortImpl(new Name("UINT")));
        ProgramVariable px = new ProgramVariable(new Name("x"), ksType, null);
        ProgramVariable pf = new ProgramVariable(new Name("f"), ksType, null);
        ProgramVariable pv = new ProgramVariable(new Name("v"), ksType, null);

        Namespace<ProgramVariable> localVars = new Namespace<>();
        localVars.add(px);
        localVars.add(pf);
        localVars.add(pv);

        Namespace<ProgramSV> schemaVariables = new Namespace<>();
        ProgramSV sv = createProgramSV(new Name("s#v"), null, false);
        ProgramSV svv = createProgramSV(new Name("s#vv"), null, false);
        schemaVariables.add(sv);
        schemaVariables.add(svv);

        Name contractName = new Name("Contract");
        Name structName = new Name("Person");
        StructType structType = new StructType(contractName, structName);
        final Sort sort = structType.getSort(services);
        KeYSolidityType ksStructType = new KeYSolidityType(structType, sort);
        services.getSolidityInfo().addType(sort, ksStructType);
        services.getNamespaces().sorts().add(sort);


        return new SolidityToKeyConverter(services, localVars, schemaVariables);
    }

    static public SolidityParser parse(String s) {
        CodePointCharStream input = CharStreams.fromString(s);

        SolidityLexer lexer = new SolidityLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);

        return new SolidityParser(tokens);
    }

    static public BlockContext parseBlockContext(String s) {
        SolidityParser parser = parse(s);
        return parser.block();
    }

    static public Block parseBlock(String s) {
        BlockContext bc = parseBlockContext(s);
        return (Block) stk.visitBlock(bc);
    }

    static public Expression parseExpression(String s) {
        SolidityParser parser = parse(s);
        ExpressionContext expCtx = parser.expression();
        return stk.visitExpression(expCtx);
    }

    static public Statement parseStatement(String s) {
        SolidityParser parser = parse(s);
        StatementContext stmCtx = parser.statement();
        return (Statement) stk.visitStatement(stmCtx);
    }
}
