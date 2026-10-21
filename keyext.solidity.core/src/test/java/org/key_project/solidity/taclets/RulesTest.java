/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.taclets;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.key_project.solidity.proof.Proof;
import org.key_project.solidity.proof.io.ProblemLoaderException;
import org.key_project.solidity.proof.io.ProofSaver;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.key_project.solidity.testutil.SolidityExampleTests.appliedRuleNames;
import static org.key_project.solidity.testutil.SolidityExampleTests.assertSameProofTree;
import static org.key_project.solidity.testutil.SolidityExampleTests.describeOpenGoals;
import static org.key_project.solidity.testutil.SolidityExampleTests.keyFiles;
import static org.key_project.solidity.testutil.SolidityExampleTests.loadAndProve;
import static org.key_project.solidity.testutil.SolidityExampleTests.replay;
import static org.key_project.solidity.testutil.SolidityExampleTests.resource;

@Tag("solidityExamples")
public class RulesTest {

    private static final String EXAMPLES_RESOURCE = "org/key_project/solidity/examples";

    /// Examples that exercise features which are not implemented yet: they load but do not
    /// close. They are still run, but an open proof is reported as aborted (a warning) instead
    /// of failing the suite, so genuine regressions in the other examples stay visible. Once an
    /// example here starts closing, remove it from this set.
    private static final Set<String> KNOWN_UNSUPPORTED = Set.of();

    private static final String UNSOUND_RESOURCE = EXAMPLES_RESOURCE + "/unsound";

    @ParameterizedTest(name = "{0}")
    @MethodSource("exampleFiles")
    public void exampleLoads(String exampleName, Path exampleFile) throws ProblemLoaderException {
        Proof proof = loadAndProve(exampleFile, 10000, -1);

        // For debugging to inspect the saved proof
        // if (!proof.closed()) {
        // try {
        // String filename = exampleFile.getFileName().toString() + ".proof";
        // ProofSaver.saveToFile(new File(filename), proof);
        // } catch (IOException e) {
        // throw new RuntimeException(e);
        // }
        // }

        Supplier<String> openGoals = () -> describeOpenGoals(exampleName, proof);

        if (KNOWN_UNSUPPORTED.contains(exampleName)) {
            // Run it, but report a still-open proof as aborted (warning) rather than failed.
            // If it ever closes, the assumption passes and the test goes green on its own.
            Assumptions.assumeTrue(proof.closed(),
                () -> "known-unsupported example (expected open goals): " + openGoals.get());
            return;
        }

        assertTrue(proof.closed(), openGoals);
    }

    /// Saves the proved example and reloads it, checking that the replay reproduces a
    /// structurally equivalent proof (same closed-ness, node count and tree of applied rules).
    /// Gives the proof save/load machinery coverage over the whole example set.
    @ParameterizedTest(name = "{0}")
    @MethodSource("exampleFiles")
    public void exampleSavesAndReloads(String exampleName, Path exampleFile) throws Exception {
        Proof original = loadAndProve(exampleFile, 10000, -1);

        // save as a sibling of the example so any relative \include / \programSource resolves
        File out = exampleFile.resolveSibling(exampleFile.getFileName() + ".roundtrip.proof")
                .toFile();
        try {
            ProofSaver.saveToFile(out, original);

            assertSameProofTree(exampleName, original, replay(out.toPath()));
        } finally {
            out.delete();
        }
    }

    static Stream<Arguments> exampleFiles() throws Exception {
        List<Path> exampleFiles = keyFiles(resource(EXAMPLES_RESOURCE)).stream()
                .filter(RulesTest::hasProofObligation)
                .toList();
        return selectRequestedExample(exampleFiles).stream()
                .map(path -> Arguments.of(path.getFileName().toString(), path));
    }

    private static List<Path> selectRequestedExample(List<Path> exampleFiles) {
        String selectedIndex = System.getProperty(
            "org.key_project.solidity.taclets.RulesTest.exampleIndex");
        if (selectedIndex == null) {
            return exampleFiles;
        }

        int index = Integer.parseInt(selectedIndex);
        if (index < 1 || index > exampleFiles.size()) {
            throw new IllegalArgumentException(
                "Requested exampleLoads[" + index + "], but only " + exampleFiles.size()
                    + " examples exist");
        }
        return List.of(exampleFiles.get(index - 1));
    }

    private static boolean hasProofObligation(Path exampleFile) {
        try (Stream<String> lines = Files.lines(exampleFile)) {
            return lines.map(String::stripLeading)
                    .anyMatch(line -> line.contains("\\problem"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("unsoundFiles")
    public void unsoundWfProblemStaysOpen(String exampleName, Path exampleFile)
            throws ProblemLoaderException {
        Proof proof = loadAndProve(exampleFile, 10000, -1);
        assertFalse(proof.closed(), exampleName + " proves a false shape-invariant claim");
    }

    static Stream<Arguments> unsoundFiles() throws Exception {
        return keyFiles(resource(UNSOUND_RESOURCE)).stream()
                .map(path -> Arguments.of(path.getFileName().toString(), path));
    }

    /// shapeInvMapElem assumes conformance at every Prim key, so shapeInvMapRight must unfold at a
    /// fresh
    /// Prim key; unsound/shapeInvMapRightIntKeysOnly.key is the counterexample to an int one.
    @Test
    public void shapeInvMapRightUnfoldsAtAPrimKey() throws ProblemLoaderException {
        Proof proof =
            loadAndProve(resource(EXAMPLES_RESOURCE + "/unfold/shapeInvMapRightPrimKey.key"), 1,
                -1);
        assertEquals(Set.of("shapeInvMapRight"), appliedRuleNames(proof));
        var unfolded = proof.openGoals().head().sequent().succedent().getFirst().formula();
        var key = unfolded.sub(1).sub(1).sub(0);
        assertEquals("Prim", key.sort().name().toString(), unfolded.toString());
    }
}
