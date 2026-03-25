/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.HIR;

import java.io.IOException;

import org.key_project.logic.Name;
import org.key_project.logic.Namespace;
import org.key_project.solidity.common.Services;
import org.key_project.solidity.program.SoliditySchemaReader;
import org.key_project.solidity.program.ast.Context;
import org.key_project.solidity.program.ast.statement.Block;
import org.key_project.solidity.rule.sv.ProgramSV;

import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.key_project.solidity.rule.sv.SchemaVariableFactory.createProgramSV;

public class SchemaReaderTest {

    @Test
    void schemaStatement() throws IOException {
        Namespace<@NonNull ProgramSV> ns = new Namespace<>();
        ProgramSV sv = createProgramSV(new Name("s#v"), null, false);
        ns.add(sv);

        Services services = new Services();
        SoliditySchemaReader scr = new SoliditySchemaReader(services, null);
        scr.setSVNamespace(ns);
        Context ctx = new Context(new Namespace<>());

        Block block = (Block) scr.readBlock("{ s#v; }", ctx).program();
        assertEquals(sv, block.getStatements().get(0).getChild(0));
    }
}
