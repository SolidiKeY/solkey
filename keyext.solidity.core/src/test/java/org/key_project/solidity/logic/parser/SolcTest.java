package org.key_project.solidity.logic.parser;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.key_project.solidity.program.ast.declarations.ContractDeclaration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.key_project.solidity.program.parser.SolcParser.getDeclStrJson;

public class SolcTest {

    @Test
    void withPath(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve("contract.sol");
        // language=solidity
        String contract = """
                contract SimpleContract { }""";
        Files.writeString(file, contract);

        ContractDeclaration ctrl = getDeclStrJson(file);
        assertNotNull(ctrl);
    }
}
