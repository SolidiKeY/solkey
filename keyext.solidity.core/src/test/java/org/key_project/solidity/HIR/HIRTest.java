/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.HIR;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.key_project.logic.Name;
import org.key_project.logic.Namespace;
import org.key_project.solidity.common.Services;
import org.key_project.solidity.logic.op.ProgramVariable;
import org.key_project.solidity.program.ast.Context;
import org.key_project.solidity.program.ast.HirSolidityReader;
import org.key_project.solidity.program.ast.statement.Block;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class HIRTest {
    @TempDir
    Path tempDir;

    Services services = new Services();
    HirSolidityReader hir = new HirSolidityReader(services);

    @Test
    void emptyBlock() throws IOException {
        Block block = (Block) hir.readBlockWithEmptyContext("{}").program();
        assertEquals(0, block.getStatements().size());
    }

    @Test
    void emptyBlockWithContextFile() throws IOException {
        Path file = tempDir.resolve("contract.sol");
        // language=solidity
        String contract = """
                contract SimpleContract { }""";
        Files.writeString(file, contract);
        Context ctx = new Context(new Namespace<>(), file);

        Block block = (Block) hir.readBlock("{}", ctx).program();
        assertEquals(0, block.getStatements().size());
    }

    @Test
    void referenceSameProgVar() throws IOException {
        Namespace<ProgramVariable> varNS = new Namespace<>();
        ProgramVariable x = new ProgramVariable(new Name("x"), null, null);
        varNS.add(x);
        Block block = (Block) hir.readBlockWithProgramVariables(varNS, "{ x = 1; }").program();
        ProgramVariable xTest =
            (ProgramVariable) block.getStatements().get(0).getChild(0).getChild(0);
        assertEquals(x, xTest);
    }
}
