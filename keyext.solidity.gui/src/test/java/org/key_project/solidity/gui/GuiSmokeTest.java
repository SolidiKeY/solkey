/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.gui;

import java.nio.file.Files;
import java.nio.file.Path;

import org.key_project.solidity.control.KeYEnvironment;
import org.key_project.solidity.proof.Node;
import org.key_project.solidity.proof.Proof;
import org.key_project.solidity.proof.io.OutputStreamProofSaver;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Headless smoke test: load an example proof, wire it through the [ProofContext] and the three
/// views, run auto mode and check the model/views update without error. Top-level windows are not
/// created (they need a display), but the panels are exercised.
public class GuiSmokeTest {

    static {
        System.setProperty("java.awt.headless", "true");
    }

    private static Path example() {
        return example("functionBody/archive.key");
    }

    private static Path example(String relativePath) {
        Path p = Path.of("keyext.solidity.examples").resolve(relativePath);
        return Files.exists(p) ? p
                : Path.of("../keyext.solidity.examples").resolve(relativePath);
    }

    @Test
    void loadsWiresAndRefreshesViews() throws Exception {
        Path file = example();
        assertTrue(Files.exists(file), "example must exist: " + file.toAbsolutePath());

        KeYEnvironment<?> env = KeYEnvironment.load(file);
        Proof proof = env.getLoadedProof();
        assertNotNull(proof);

        ProofContext context = new ProofContext();
        // Construct the views (registers them as listeners); panels are headless-safe.
        ProofTreePanel tree = new ProofTreePanel(context);
        GoalsView goals = new GoalsView(context);
        SequentView sequent = new SequentView(context);
        assertNotNull(tree);
        assertNotNull(goals);
        assertNotNull(sequent);

        context.setProof(env, proof);
        assertTrue(context.getSelectedNode() == proof.root(), "root should be selected on load");
        assertTrue(!proof.openGoals().isEmpty(), "freshly loaded proof should have an open goal");

        // The selected node renders to a non-empty sequent (inner/leaf agnostic).
        Node root = proof.root();
        String rendered =
            OutputStreamProofSaver.printSequent(root.sequent(), root.proof().getServices());
        assertTrue(rendered.contains("=") || rendered.contains("\\<"),
            "root sequent should render: " + rendered);

        // Run auto mode and refresh; the example closes, so no open goals remain.
        env.getProofControl().startAndWaitForAutoMode(proof);
        context.fireProofChanged();
        assertTrue(proof.closed(), "archive example should close under auto mode");
        assertTrue(proof.openGoals().isEmpty(), "closed proof has no open goals");
    }

    @Test
    void loadsAChosenSolidityFunction() throws Exception {
        Path file = example("TestSuite.sol");
        assertTrue(Files.exists(file), "example must exist: " + file.toAbsolutePath());

        KeYEnvironment<?> env = KeYEnvironment.load(file, "TestSuite", "testSimpleAssert");
        Proof proof = env.getLoadedProof();
        assertNotNull(proof);
        assertEquals("TestSuite.testSimpleAssert", proof.name().toString());
        assertNotNull(proof.getSoliditySource());

        ProofContext context = new ProofContext();
        context.setProof(env, proof);
        assertTrue(context.getSelectedNode() == proof.root());

        env.getProofControl().startAndWaitForAutoMode(proof);
        context.fireProofChanged();
        assertTrue(proof.closed(), "testSimpleAssert should close under auto mode");
    }

    /// Naming a function no obligation can be generated for is refused with the reason. This is
    /// what a gutter click on an unsupported function has to produce: `MainWindow` shows this
    /// message instead of falling back to the picker.
    @Test
    void refusesAFunctionThatCannotBeProved() {
        Path file = example("net/PiggyBankNet.sol");
        assertTrue(Files.exists(file), "example must exist: " + file.toAbsolutePath());

        Exception e = assertThrows(Exception.class,
            () -> KeYEnvironment.load(file, "PiggyBankNet", "payTo"));

        assertTrue(describe(e).contains("cannot be proved"), describe(e));
        assertTrue(describe(e).contains("address payable"), describe(e));
    }

    /// The reason may be wrapped by the loader, so match against the whole cause chain.
    private static String describe(Throwable t) {
        StringBuilder sb = new StringBuilder();
        for (Throwable c = t; c != null; c = c.getCause()) {
            sb.append(c).append('\n');
        }
        return sb.toString();
    }
}
