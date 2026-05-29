package org.key_project.solidity.taclets;

import java.io.File;
import java.io.IOException;
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

import static org.junit.jupiter.api.Assertions.assertEquals;

public class RulesTest {

    private static final String EXAMPLES_RESOURCE = "org/key_project/solidity/examples";

    @ParameterizedTest(name = "{0}")
    @MethodSource("exampleFiles")
    public void exampleLoads(String exampleName, Path exampleFile) {
        assertEquals(0, CLI.execute(exampleFile.toAbsolutePath().toString()),
            () -> exampleName + " should be verified");
    }

    static Stream<Arguments> exampleFiles() throws Exception {
        URL resource = RulesTest.class.getClassLoader().getResource(EXAMPLES_RESOURCE);
        File examplesDirectory = Path.of(resource.toURI()).toFile();

        List<Path> exampleFiles = Files.list(examplesDirectory.toPath())
            .filter(Files::isRegularFile)
            .filter(path -> path.getFileName().toString().endsWith(".key") && hasProofObligation(path))
            .sorted(Comparator.comparing(path -> path.getFileName().toString()))
            .toList();

        return exampleFiles.stream()
                .map(path -> Arguments.of(path.getFileName().toString(), path));
    }

    private static boolean hasProofObligation(Path exampleFile) {
        try (Stream<String> lines = Files.lines(exampleFile)) {
            return lines.map(String::stripLeading)
                    .anyMatch(line -> line.contains("\\problem"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
