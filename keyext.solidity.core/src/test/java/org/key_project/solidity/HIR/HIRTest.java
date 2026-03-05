package org.key_project.solidity.HIR;

import org.junit.jupiter.api.Test;
import org.key_project.logic.Namespace;
import org.key_project.solidity.common.Services;
import org.key_project.solidity.program.ast.Context;
import org.key_project.solidity.program.ast.HirSolidityReader;
import org.key_project.solidity.program.ast.statement.Block;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class HIRTest {

    @Test
    void emptyBlock() throws IOException {
        Services services = new Services();
        HirSolidityReader hir = new HirSolidityReader(services);
        Context ctx = new Context(new Namespace<>());
        Block block = hir.readBlock("{}", ctx);
        assertEquals(0, block.getStatements().size());
    }
}
