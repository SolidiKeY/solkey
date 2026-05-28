package org.key_project.solidity.taclets;

import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.key_project.solidity.CLI;
import org.key_project.solidity.parser.ParsingFacade;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RulesTest {

    private static final String EXAMPLES_RESOURCE = "org/key_project/solidity/examples";

    @ParameterizedTest(name = "{0}")
    @MethodSource("exampleFiles")
    public void exampleLoads(String exampleName, Path exampleFile) throws Exception {
        if (hasProofObligation(exampleFile)) {
            assertEquals(0, CLI.execute("--no-prove", exampleFile.toAbsolutePath().toString()),
                () -> exampleName + " should load through CLI");
        } else {
            assertFalse(ParsingFacade.parseFiles(exampleFile.toUri().toURL()).isEmpty(),
                () -> exampleName + " should parse");
        }
    }

    static Stream<Arguments> exampleFiles() throws Exception {
        URL resource = RulesTest.class.getClassLoader().getResource(EXAMPLES_RESOURCE);
        assertNotNull(resource, EXAMPLES_RESOURCE + " test resources must be available");

        File examplesDirectory = Path.of(resource.toURI()).toFile();
        assertTrue(examplesDirectory.isDirectory(),
            EXAMPLES_RESOURCE + " test resource must resolve to a directory");

        List<Path> exampleFiles;
        try (Stream<Path> files = Files.list(examplesDirectory.toPath())) {
            exampleFiles = files
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".key"))
                    .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                    .toList();
        }

        assertFalse(exampleFiles.isEmpty(), EXAMPLES_RESOURCE + " must contain .key examples");
        return exampleFiles.stream()
                .map(path -> Arguments.of(path.getFileName().toString(), path));
    }

    private static boolean hasProofObligation(Path exampleFile) throws Exception {
        try (Stream<String> lines = Files.lines(exampleFile)) {
            return lines.map(String::stripLeading)
                    .anyMatch(line -> line.startsWith("\\problem")
                            || line.startsWith("\\chooseContract")
                            || line.startsWith("\\proofObligation"));
        }
    }
}
