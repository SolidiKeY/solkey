/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.taclets;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.key_project.solidity.control.KeYEnvironment;
import org.key_project.solidity.proof.Node;
import org.key_project.solidity.proof.Proof;
import org.key_project.solidity.proof.io.OutputStreamProofSaver;
import org.key_project.solidity.proof.io.ProblemLoaderException;
import org.key_project.solidity.proof.io.ProofSaver;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RulesTest {

    private static final String EXAMPLES_RESOURCE = "org/key_project/solidity/examples";

    /// Examples that exercise features which are not implemented yet: they load but do not
    /// close. They are still run, but an open proof is reported as aborted (a warning) instead
    /// of failing the suite, so genuine regressions in the other examples stay visible. Once an
    /// example here starts closing, remove it from this set.
    private static final Set<String> KNOWN_UNSUPPORTED = Set.of();

    private static Proof prove(Path f, long timeout, int maxSteps) throws ProblemLoaderException {
        var env = KeYEnvironment.load(f);
        var loadedProof = env.getLoadedProof();
        var stratSettings = loadedProof.getSettings().getStrategySettings();
        stratSettings.setTimeout(timeout);
        stratSettings.setMaxSteps(maxSteps);
        env.getProofControl().startAndWaitForAutoMode(loadedProof);
        return loadedProof;
    }


    @ParameterizedTest(name = "{0}")
    @MethodSource("exampleFiles")
    public void exampleLoads(String exampleName, Path exampleFile) throws ProblemLoaderException {
        Proof proof = prove(exampleFile, -1, 10000);

        // For debugging to inspect the saved proof
        // if (!proof.closed()) {
        // try {
        // String filename = exampleFile.getFileName().toString() + ".proof";
        // ProofSaver.saveToFile(new File(filename), proof);
        // } catch (IOException e) {
        // throw new RuntimeException(e);
        // }
        // }

        Supplier<String> openGoals = () -> exampleName
            + " should be verified, but the following goals are open "
            + proof.getOpenGoals().stream()
                    .map(g -> OutputStreamProofSaver.printSequent(g.sequent(),
                        g.getOverlayServices()))
                    .toList()
            + "\n" + proof.getStatistics();

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
        Proof original = prove(exampleFile, -1, 10000);

        // save as a sibling of the example so any relative \include / \programSource resolves
        File out = exampleFile.resolveSibling(exampleFile.getFileName() + ".roundtrip.proof")
                .toFile();
        try {
            ProofSaver.saveToFile(out, original);

            Proof reloaded = KeYEnvironment.load(out.toPath()).getLoadedProof();
            assertEquals(original.closed(), reloaded.closed(),
                () -> exampleName + ": reloaded proof closed-ness differs");
            assertEquals(original.countNodes(), reloaded.countNodes(),
                () -> exampleName + ": reloaded proof has a different number of nodes");
            assertEquals(treeSignature(original.root()), treeSignature(reloaded.root()),
                () -> exampleName + ": reloaded proof tree differs from the original");
        } finally {
            out.delete();
        }
    }

    /// Canonical preorder rendering of a proof tree: each node's applied rule name (or `*` for an
    /// open leaf) followed by its children in parentheses.
    private static String treeSignature(Node node) {
        StringBuilder sb = new StringBuilder();
        var app = node.getAppliedRuleApp();
        sb.append(app == null ? "*" : app.rule().name().toString());
        if (node.childrenCount() > 0) {
            sb.append('(');
            for (int i = 0; i < node.childrenCount(); i++) {
                if (i > 0) {
                    sb.append(',');
                }
                sb.append(treeSignature(node.child(i)));
            }
            sb.append(')');
        }
        return sb.toString();
    }

    static Stream<Arguments> exampleFiles() throws Exception {
        URL resource = RulesTest.class.getClassLoader().getResource(EXAMPLES_RESOURCE);

        if (resource == null) {
            throw new FileNotFoundException(
                "Could not find resource with examples: " + EXAMPLES_RESOURCE);
        }

        try (var examples = Files.list(Path.of(resource.toURI()))) {
            List<Path> exampleFiles = examples
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".key")
                            && hasProofObligation(path))
                    .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                    .toList();

            return selectRequestedExample(exampleFiles).stream()
                    .map(path -> Arguments.of(path.getFileName().toString(), path));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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
}
