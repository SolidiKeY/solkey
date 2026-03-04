/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.visitor;

import org.junit.jupiter.api.Test;
import org.key_project.logic.Name;
import org.key_project.logic.sort.Sort;
import org.key_project.solidity.common.Services;
import org.key_project.solidity.logic.op.ProgramVariable;
import org.key_project.solidity.logic.sort.SortImpl;
import org.key_project.solidity.program.ast.abstractions.KeYSolidityType;
import org.key_project.solidity.program.ast.abstractions.PrimitiveType;
import org.key_project.solidity.program.ast.expressions.Expression;

import java.util.LinkedHashSet;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProgramVariableVisitorTest {

    final Sort uint = new SortImpl(new Name("uint"), false);
    KeYSolidityType uintKST = new KeYSolidityType(PrimitiveType.UINT, uint);
    Services services = new Services();

    @Test
    void onlyProgVar() {
        Expression x = new ProgramVariable(new Name("x"), uintKST);
        ProgramVariableCollector visitor = new ProgramVariableCollector(x, services);

        visitor.start();

        LinkedHashSet<ProgramVariable> variables = visitor.result();
        assertEquals(1, variables.size());
        assertEquals(x, variables.getFirst());
    }
}
