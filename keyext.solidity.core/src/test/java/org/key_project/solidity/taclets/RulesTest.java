/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.taclets;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import org.key_project.solidity.control.KeYEnvironment;
import org.key_project.solidity.proof.Proof;
import org.key_project.solidity.proof.io.OutputStreamProofSaver;
import org.key_project.solidity.proof.io.ProblemLoaderException;
import org.key_project.solidity.proof.io.ProofSaver;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class RulesTest {

    private static final String EXAMPLES_RESOURCE = "org/key_project/solidity/examples";

    private static Proof prove(File f, long timeout, int maxSteps) throws ProblemLoaderException {
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
        Proof proof = prove(exampleFile.toFile(), -1, 10000);

// For debugging to inspect the saved proof
//        if (!proof.closed()) {
//            try {
//                String filename = exampleFile.getFileName().toString() + ".proof";
//                ProofSaver.saveToFile(new File(filename), proof);
//            } catch (IOException e) {
//                throw new RuntimeException(e);
//            }
//        }

        assertEquals(0, proof.closed(),
            () -> exampleName + " should be verified, but the following goals are open " +
                proof.getOpenGoals().stream()
                        .map(g -> OutputStreamProofSaver.printSequent(g.sequent(),
                            g.getOverlayServices()))
                        .toList()
                + "\n" + proof.getStatistics());


    }

    static Stream<Arguments> exampleFiles() throws Exception {
        URL resource = RulesTest.class.getClassLoader().getResource(EXAMPLES_RESOURCE);
        File examplesDirectory = Path.of(resource.toURI()).toFile();

        List<Path> exampleFiles = Files.list(examplesDirectory.toPath())
                .filter(Files::isRegularFile)
                .filter(path -> path.getFileName().toString().endsWith(".key")
                        && hasProofObligation(path))
                .sorted(Comparator.comparing(path -> path.getFileName().toString()))
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
}
