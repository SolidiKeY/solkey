package org.key_project.solidity.HIR;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.key_project.logic.Namespace;
import org.key_project.solidity.common.Services;
import org.key_project.solidity.program.ast.Context;
import org.key_project.solidity.program.ast.HirSolidityReader;
import org.key_project.solidity.program.ast.statement.Block;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class HIRTest {
    @TempDir
    Path tempDir;

    @Test
    void emptyBlock() throws IOException {
        Services services = new Services();
        HirSolidityReader hir = new HirSolidityReader(services);
        Block block = hir. readBlockWithEmptyContext("{}");
        assertEquals(0, block.getStatements().size());
    }

    @Test
    void emptyBlockWithContextFile() throws IOException {
        Services services = new Services();
        HirSolidityReader hir = new HirSolidityReader(services);

        Path file = tempDir.resolve("contract.sol");
        // language=solidity
        String contract = """
                contract SimpleContract { }""";
        Files.writeString(file, contract);
        Context ctx = new Context(new Namespace<>(), file);

        Block block = hir. readBlockWithEmptyContext("{}");
        assertEquals(0, block.getStatements().size());
    }
}
