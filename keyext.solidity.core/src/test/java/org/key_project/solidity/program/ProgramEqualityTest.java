/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program;

import java.io.IOException;

import org.key_project.logic.Name;
import org.key_project.logic.Namespace;
import org.key_project.solidity.common.Services;
import org.key_project.solidity.logic.SolidityBlock;
import org.key_project.solidity.logic.op.ProgramVariable;
import org.key_project.solidity.logic.sort.SortImpl;
import org.key_project.solidity.program.ast.abstractions.KeYSolidityType;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.key_project.solidity.program.ast.abstractions.PrimitiveType.UINT;

/// Two programs that are parsed independently but are structurally identical (and share the same
/// program variables) must compare equal, so the prover recognises identical modalities.
public class ProgramEqualityTest {

    private final Services services = new Services();
    private final SolidityReader reader = new SolidityReader(services);

    private SolidityBlock read(Namespace<ProgramVariable> vars, String src) throws IOException {
        return reader.readBlockWithProgramVariables(vars, src);
    }

    @Test
    void structurallyEqualBlocksAreEqual() throws IOException {
        Namespace<ProgramVariable> vars = new Namespace<>();
        KeYSolidityType ty = new KeYSolidityType(UINT, new SortImpl(new Name("UINT")));
        vars.add(new ProgramVariable(new Name("x"), ty, null));

        SolidityBlock a = read(vars, "{ x = 1; }");
        SolidityBlock b = read(vars, "{ x = 1; }");

        assertEquals(a, b, "structurally equal blocks should be equal");
        assertEquals(a.hashCode(), b.hashCode(), "equal blocks must have equal hash codes");
    }

    @Test
    void structurallyEqualBinaryExpressionsAreEqual() throws IOException {
        Namespace<ProgramVariable> vars = new Namespace<>();
        KeYSolidityType ty = new KeYSolidityType(UINT, new SortImpl(new Name("UINT")));
        vars.add(new ProgramVariable(new Name("x"), ty, null));

        SolidityBlock a = read(vars, "{ x = x + 1; }");
        SolidityBlock b = read(vars, "{ x = x + 1; }");

        assertEquals(a, b, "blocks with equal binary expressions should be equal");
        assertEquals(a.hashCode(), b.hashCode(), "equal blocks must have equal hash codes");
    }
}
