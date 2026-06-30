/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.visitor;

import org.key_project.logic.Name;
import org.key_project.logic.op.sv.SchemaVariable;
import org.key_project.solidity.program.ast.statement.Statement;
import org.key_project.solidity.rule.sv.ProgramSV;
import org.key_project.util.collection.*;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.key_project.solidity.parser.ParserForTesting.parseStatement;
import static org.key_project.solidity.rule.matching.inst.SVInstantiations.EMPTY_SVINSTANTIATIONS;
import static org.key_project.solidity.rule.sv.SchemaVariableFactory.createProgramSV;
import static org.key_project.solidity.rule.sv.sort.ProgramSVSort.VARIABLE;

public class SchemaVariableVisitorTest {

    @Test
    void onlySchema() {
        ProgramSV x = createProgramSV(new Name("x"), VARIABLE, false);
        ProgramSVCollector visitor =
            new ProgramSVCollector(x, ImmutableSLList.nil(), EMPTY_SVINSTANTIATIONS);

        visitor.start();
        ImmutableList<SchemaVariable> schemaVars = visitor.getSchemaVariables();
        assertEquals(1, schemaVars.size());
        assertEquals(x, schemaVars.get(0));
    }

    @Test
    void statement() {
        Statement stm = parseStatement("s#v;");
        ProgramSVCollector visitor =
            new ProgramSVCollector(stm, ImmutableSLList.nil(), EMPTY_SVINSTANTIATIONS);
        visitor.start();
        ImmutableList<SchemaVariable> schemaVars = visitor.getSchemaVariables();
        assertEquals(1, schemaVars.size());
        assertEquals("v", schemaVars.get(0).toString());
    }

    @Test
    void statementComplex() {
        Statement stm = parseStatement("int a = s#v;");
        ProgramSVCollector visitor =
            new ProgramSVCollector(stm, ImmutableSLList.nil(), EMPTY_SVINSTANTIATIONS);
        visitor.start();
        ImmutableList<SchemaVariable> schemaVars = visitor.getSchemaVariables();
        assertEquals(1, schemaVars.size());
        assertEquals("v", schemaVars.get(0).toString());
    }

    @Test
    void statementTwoSchemas() {
        Statement stm = parseStatement("int a = s#v + s#vv;");
        ProgramSVCollector visitor =
            new ProgramSVCollector(stm, ImmutableSLList.nil(), EMPTY_SVINSTANTIATIONS);
        visitor.start();
        ImmutableList<SchemaVariable> schemaVars = visitor.getSchemaVariables();
        assertEquals(2, schemaVars.size());
        assertEquals("vv", schemaVars.get(0).toString());
        assertEquals("v", schemaVars.get(1).toString());
    }

}
