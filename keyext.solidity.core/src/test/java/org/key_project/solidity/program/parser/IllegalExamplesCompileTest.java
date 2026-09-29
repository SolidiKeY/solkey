/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.parser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.key_project.solidity.testutil.SolidityExampleTests;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class IllegalExamplesCompileTest {

    private static final String MARKER = "/// solc:";

    @ParameterizedTest(name = "{0}")
    @MethodSource("contracts")
    void doesNotCompile(String name, Path file) throws IOException {
        String source = Files.readString(file, UTF_8);
        String expected = expectedDiagnostic(name, source);

        var e = assertThrows(RuntimeException.class, () -> SolcWrapper.getBinJson(file),
            () -> name + " compiles, but " + MARKER + " claims solc rejects it with: " + expected);

        assertTrue(e.getMessage().contains(expected),
            () -> name + ": solc rejected it, but not with the expected diagnostic.\nexpected: "
                + expected + "\nactual:\n" + e.getMessage());
    }

    private static String expectedDiagnostic(String name, String source) {
        Optional<String> marker = source.lines()
                .map(String::strip)
                .filter(line -> line.startsWith(MARKER))
                .map(line -> line.substring(MARKER.length()).strip())
                .findFirst();
        assertTrue(marker.isPresent(),
            () -> name + " must declare the diagnostic it expects on a `" + MARKER + " ...` line");
        assertFalse(marker.get().isEmpty(), () -> name + " has an empty `" + MARKER + "` line");
        return marker.get();
    }

    static Stream<Arguments> contracts() throws IOException {
        Path dir = SolidityExampleTests.examplesDir("illegal/nocompile");
        assertTrue(Files.isDirectory(dir), () -> "missing directory: " + dir.toAbsolutePath());
        try (Stream<Path> files = Files.list(dir)) {
            List<Arguments> contracts =
                files.filter(p -> p.getFileName().toString().endsWith(".sol"))
                        .sorted()
                        .map(p -> Arguments.of(p.getFileName().toString(), p))
                        .toList();
            assertFalse(contracts.isEmpty(), () -> "no contracts in " + dir.toAbsolutePath());
            return contracts.stream();
        }
    }
}
