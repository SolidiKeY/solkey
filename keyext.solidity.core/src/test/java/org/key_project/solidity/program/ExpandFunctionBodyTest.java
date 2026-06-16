/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program;

import java.io.IOException;
import java.util.List;

import org.key_project.logic.Name;
import org.key_project.logic.Namespace;
import org.key_project.solidity.common.Services;
import org.key_project.solidity.logic.op.ProgramVariable;
import org.key_project.solidity.logic.sort.SortImpl;
import org.key_project.solidity.program.ast.SolidityProgramElement;
import org.key_project.solidity.program.ast.abstractions.KeYSolidityType;
import org.key_project.solidity.program.ast.declarations.FunctionDeclaration;
import org.key_project.solidity.program.ast.declarations.FunctionEnums.StateMutability;
import org.key_project.solidity.program.ast.declarations.FunctionEnums.Visibility;
import org.key_project.solidity.program.ast.declarations.StatementVariableDeclaration;
import org.key_project.solidity.program.ast.expressions.Expression;
import org.key_project.solidity.program.ast.statement.Block;
import org.key_project.solidity.program.ast.statement.DeclarationStatement;
import org.key_project.solidity.program.ast.statement.ExpressionStatement;
import org.key_project.solidity.program.ast.statement.FunctionBodyStatement;
import org.key_project.solidity.program.ast.statement.Statement;
import org.key_project.solidity.rule.metaconstruct.ExpandFunctionBody;
import org.key_project.solidity.rule.sv.SchemaVariableFactory;
import org.key_project.solidity.rule.sv.sort.ProgramSVSort;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.key_project.solidity.program.ast.abstractions.PrimitiveType.UINT;

/// Exercises [ExpandFunctionBody]: inlining the body of a [FunctionBodyStatement] must declare
/// a fresh local variable per formal parameter (initialised with the actual argument) and rewrite
/// the body so that parameter references become object-identical to those fresh declarations.
public class ExpandFunctionBodyTest {

    private final Services services = new Services();
    private final SolidityReader reader = new SolidityReader(services);

    private ProgramVariable var(String name) {
        KeYSolidityType ty = new KeYSolidityType(UINT, new SortImpl(new Name("UINT")));
        return new ProgramVariable(new Name(name), ty, null);
    }

    @Test
    void expandsBodyWithFreshObjectIdenticalParameter() throws IOException {
        // formal parameter p, used in the body; actual argument a
        ProgramVariable p = var("p");
        ProgramVariable a = var("a");

        // parse the body so that its reference to "p" is the very same object as the formal
        Namespace<ProgramVariable> bodyVars = new Namespace<>();
        bodyVars.add(p);
        Block body = (Block) reader.readBlockWithProgramVariables(bodyVars, "{ p; }").program();

        // sanity: the body really references the formal parameter object
        Expression bodyRef =
            ((ExpressionStatement) body.getStatements().get(0)).getExpression();
        assertSame(p, bodyRef, "body should reference the formal parameter object");

        FunctionDeclaration fn = new FunctionDeclaration(new Name("f"), List.of(),
            p.getKeYSolidityType(), List.of(p), body, "function",
            Visibility.Public, StateMutability.nonpayable, List.of(), "");

        FunctionBodyStatement fbs =
            new FunctionBodyStatement(null, fn, List.<Expression>of(a));

        // dummy transformer; the body argument is irrelevant for transform()
        ExpandFunctionBody transformer = new ExpandFunctionBody(
            SchemaVariableFactory.createProgramSV(new Name("fbs"),
                ProgramSVSort.FUNCTION_BODY, false));

        SolidityProgramElement[] result = transformer.transform(fbs, services, null);

        // expect: one parameter declaration followed by the (rewritten) body block
        assertEquals(2, result.length, "one param decl + body block");
        assertTrue(result[0] instanceof DeclarationStatement, "first element is a declaration");
        assertTrue(result[1] instanceof Block, "second element is the body block");

        DeclarationStatement decl = (DeclarationStatement) result[0];
        ProgramVariable declared =
            ((StatementVariableDeclaration) decl.getDeclarations().get(0)).getProgramVariable();

        // the declared variable is fresh (not the formal) but keeps its name ...
        assertNotSame(p, declared, "declared variable must be fresh");
        assertEquals(p.name(), declared.name(), "fresh variable keeps the parameter name");
        // ... and is initialised with the actual argument
        assertSame(a, decl.getInitialValue(), "parameter initialised with the actual argument");

        // the rewritten body references the fresh declared variable (object identity!)
        Block newBody = (Block) result[1];
        Statement first = newBody.getStatements().get(0);
        Expression newRef = ((ExpressionStatement) first).getExpression();
        assertSame(declared, newRef,
            "inlined body must reference the freshly declared variable object");
    }
}
