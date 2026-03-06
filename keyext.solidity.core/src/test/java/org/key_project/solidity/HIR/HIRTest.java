package org.key_project.solidity.HIR;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.key_project.logic.Name;
import org.key_project.logic.Namespace;
import org.key_project.solidity.common.Services;
import org.key_project.solidity.logic.op.ProgramVariable;
import org.key_project.solidity.program.ast.Context;
import org.key_project.solidity.program.ast.HirSolidityReader;
import org.key_project.solidity.program.ast.statement.Block;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class HIRTest {
    @TempDir
    Path tempDir;

    Services services = new Services();
    HirSolidityReader hir = new HirSolidityReader(services);

    @Test
    void emptyBlock() throws IOException {
        Block block = hir.readBlockWithEmptyContext("{}");
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

        Block block = hir. readBlock("{}", ctx);
        assertEquals(0, block.getStatements().size());
    }

    @Test
    void referenceSameProgVar() throws IOException {
        Namespace<ProgramVariable> varNS = new Namespace<>();
        ProgramVariable x = new ProgramVariable(new Name("x"), null, null);
        varNS.add(x);
        Block block = hir.readBlockWithProgramVariables(varNS, "{ x = 1; }");
        ProgramVariable xTest = (ProgramVariable) block.getStatements().get(0).getChild(0).getChild(0);
        assertEquals(x, xTest);
    }
}
