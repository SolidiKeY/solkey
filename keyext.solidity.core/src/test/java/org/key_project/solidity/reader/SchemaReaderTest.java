/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.reader;

import java.io.IOException;

import org.key_project.logic.*;
import org.key_project.logic.op.sv.SchemaVariable;
import org.key_project.solidity.common.Services;
import org.key_project.solidity.program.SoliditySchemaReader;
import org.key_project.solidity.program.ast.Context;
import org.key_project.solidity.program.ast.expressions.NewExpression;
import org.key_project.solidity.program.ast.statement.*;
import org.key_project.solidity.rule.sv.ProgramSV;
import org.key_project.solidity.rule.sv.sort.ProgramSVSort;

import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.key_project.solidity.rule.sv.SchemaVariableFactory.createProgramSV;

public class SchemaReaderTest {

    @Test
    void schemaStatement() throws IOException {
        Namespace<@NonNull SchemaVariable> ns = new Namespace<>();
        ProgramSV sv = createProgramSV(new Name("v"), null, false);
        ns.add(sv);

        Services services = new Services();
        SoliditySchemaReader scr = new SoliditySchemaReader(services, null);
        scr.setSVNamespace(ns);
        Context ctx = new Context(new Namespace<>());

        Block block = (Block) scr.readBlock("{ s#v; }", ctx).program();
        assertEquals(sv, ((ExpressionStatement) block.getStatements().get(0)).getExpression());
    }

    @Test
    void schemaTypeInNewExpression() throws IOException {
        Namespace<@NonNull SchemaVariable> ns = new Namespace<>();
        ProgramSV t = createProgramSV(new Name("t"), ProgramSVSort.TYPE, false);
        ns.add(t);

        Services services = new Services();
        SoliditySchemaReader scr = new SoliditySchemaReader(services, null);
        scr.setSVNamespace(ns);
        Context ctx = new Context(new Namespace<>());

        Block block = (Block) scr.readBlock("{ new s#t; }", ctx).program();
        NewExpression newExp =
            (NewExpression) ((ExpressionStatement) block.getStatements().get(0)).getExpression();
        assertSame(t, newExp.getType(), "the new-expression type is the Type schema variable");
    }
}
