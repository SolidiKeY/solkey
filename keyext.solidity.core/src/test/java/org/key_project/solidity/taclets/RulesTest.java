package org.key_project.solidity.taclets;

import java.io.File;
import java.net.URL;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.key_project.solidity.CLI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RulesTest {

    @Test
    public void problem2RunsThroughCli() throws Exception {
        URL resource = RulesTest.class.getClassLoader()
                .getResource("org/key_project/solidity/examples/problem2.key");
        assertNotNull(resource, "problem2.key test resource must be available");

        File problemFile = Path.of(resource.toURI()).toFile();
        assertTrue(problemFile.isFile(), "problem2.key test resource must resolve to a file");

        assertEquals(0, CLI.execute(problemFile.getAbsolutePath()));
    }
}
