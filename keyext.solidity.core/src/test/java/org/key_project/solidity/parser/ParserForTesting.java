/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.parser;

import java.util.List;

import org.key_project.logic.*;
import org.key_project.logic.sort.Sort;
import org.key_project.solidity.common.Services;
import org.key_project.solidity.logic.op.ProgramVariable;
import org.key_project.solidity.logic.sort.SortImpl;
import org.key_project.solidity.parser.SolidityParser.*;
import org.key_project.solidity.program.ast.abstractions.KeYSolidityType;
import org.key_project.solidity.program.ast.declarations.*;
import org.key_project.solidity.program.ast.expressions.Expression;
import org.key_project.solidity.program.ast.statement.*;
import org.key_project.solidity.rule.sv.ProgramSV;

import org.antlr.v4.runtime.*;

import static org.key_project.solidity.program.ast.abstractions.PrimitiveType.UINT;
import static org.key_project.solidity.rule.sv.SchemaVariableFactory.createProgramSV;

public class ParserForTesting {

    static SolidityToKeyConverter stk = solConverter();

    private static SolidityToKeyConverter solConverter() {
        Services services = new Services();

        KeYSolidityType ksType =
            new KeYSolidityType(UINT, new SortImpl(new Name("UINT")));
        ProgramVariable px = new ProgramVariable(new Name("x"), ksType, null);
        ProgramVariable pf = new ProgramVariable(new Name("f"), ksType, null);
        ProgramVariable pv = new ProgramVariable(new Name("v"), ksType, null);

        Namespace<ProgramVariable> localVars = new Namespace<>();
        localVars.add(px);
        localVars.add(pf);
        localVars.add(pv);

        Namespace<ProgramSV> schemaVariables = new Namespace<>();
        ProgramSV sv = createProgramSV(new Name("s#v"), null, false, UINT);
        ProgramSV svv = createProgramSV(new Name("s#vv"), null, false, UINT);
        schemaVariables.add(sv);
        schemaVariables.add(svv);

        Name structName = new Name("Person");
        StructDeclaration structDeclaration = new StructDeclaration(structName, List.of(), -1);
        final Sort sort = structDeclaration.getSort(services);
        KeYSolidityType ksStructType = new KeYSolidityType(structDeclaration, sort);
        services.getSolidityInfo().addType(sort, ksStructType);
        services.getNamespaces().sorts().add(sort);

        Name enumName = new Name("State");
        EnumDeclaration stateEnum = new EnumDeclaration(enumName, List.of(
            new MemberEnumDeclaration(new Name("Begin")),
            new MemberEnumDeclaration(new Name("End"))
        ));
        final Sort enumSort = stateEnum.getSort(services);
        KeYSolidityType ksEnumType = new KeYSolidityType(stateEnum, enumSort);
        services.getSolidityInfo().addType(enumSort, ksEnumType);
        services.getNamespaces().sorts().add(enumSort);

        Namespace<FunctionDeclaration> functions = new Namespace<>();
        FunctionDeclaration f = new FunctionDeclaration(new Name("f"),
                List.of(), null, List.of(), null, null, null, null, List.of(), null);
        functions.add(f);

        return new SolidityToKeyConverter(services, functions, localVars, schemaVariables);
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
