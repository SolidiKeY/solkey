/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.gui;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.swing.tree.DefaultMutableTreeNode;

import org.key_project.logic.PosInTerm;
import org.key_project.prover.sequent.PosInOccurrence;
import org.key_project.prover.sequent.SequentFormula;
import org.key_project.solidity.control.KeYEnvironment;
import org.key_project.solidity.proof.Goal;
import org.key_project.solidity.proof.Proof;
import org.key_project.solidity.rule.TacletApp;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Checks the linearized proof-tree view: a linear chain of proof steps is shown as one flat
/// branch (no nesting), and a proof split introduces a sub-branch per child.
public class ProofTreeLinearizationTest {

    static {
        System.setProperty("java.awt.headless", "true");
    }

    private static DefaultMutableTreeNode rootOf(ProofTreePanel panel) {
        return (DefaultMutableTreeNode) panel.getTree().getModel().getRoot();
    }

    /// Children that are individual proof nodes (leaves of the tree).
    private static int leafChildren(DefaultMutableTreeNode branch) {
        int n = 0;
        for (int i = 0; i < branch.getChildCount(); i++) {
            if (!branch.getChildAt(i).getAllowsChildren()) {
                n++;
            }
        }
        return n;
    }

    /// Children that are sub-branches.
    private static int subBranches(DefaultMutableTreeNode branch) {
        int n = 0;
        for (int i = 0; i < branch.getChildCount(); i++) {
            if (branch.getChildAt(i).getAllowsChildren()) {
                n++;
            }
        }
        return n;
    }

    /// Total number of proof-node leaves across the whole linearized tree.
    private static int totalLeaves(DefaultMutableTreeNode branch) {
        int n = 0;
        for (int i = 0; i < branch.getChildCount(); i++) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) branch.getChildAt(i);
            n += child.getAllowsChildren() ? totalLeaves(child) : 1;
        }
        return n;
    }


    private static ProofTreePanel run(KeYEnvironment<?> env, Proof proof) {
        ProofContext context = new ProofContext();
        ProofTreePanel panel = new ProofTreePanel(context);
        context.setProof(env, proof);
        env.getProofControl().startAndWaitForAutoMode(proof);
        context.fireProofChanged();
        return panel;
    }

    @Test
    void linearProofIsOneFlatBranch() throws Exception {
        Path file = Path.of("keyext.solidity.examples/functionBody/archive.key");
        if (!Files.exists(file)) {
            file = Path.of("../keyext.solidity.examples/functionBody/archive.key");
        }
        KeYEnvironment<?> env = KeYEnvironment.load(file);
        Proof proof = env.getLoadedProof();

        DefaultMutableTreeNode root = rootOf(run(env, proof));
        // Every proof node is a flat leaf under the single root branch; no nesting, no
        // sub-branches.
        assertEquals(proof.countNodes(), leafChildren(root),
            "all nodes of a linear proof should be flat leaves of the root branch");
        assertEquals(0, subBranches(root), "a linear proof has no sub-branches");
    }

    @Test
    void hideIntermediateCollapsesChainToEndpoint() throws Exception {
        Path file = Path.of("keyext.solidity.examples/functionBody/archive.key");
        if (!Files.exists(file)) {
            file = Path.of("../keyext.solidity.examples/functionBody/archive.key");
        }
        KeYEnvironment<?> env = KeYEnvironment.load(file);
        Proof proof = env.getLoadedProof();
        ProofTreePanel panel = run(env, proof);

        assertTrue(leafChildren(rootOf(panel)) > 1, "without filters every step is shown");

        panel.setFilters(true, false, false, false); // hide intermediate proof steps
        DefaultMutableTreeNode root = rootOf(panel);
        assertEquals(1, leafChildren(root),
            "hide-intermediate leaves only the branch endpoint");
        assertEquals(0, subBranches(root), "still a single (linear) branch");
    }

    @Test
    void searchSelectsMatchingNode() throws Exception {
        Path file = Path.of("keyext.solidity.examples/functionBody/archive.key");
        if (!Files.exists(file)) {
            file = Path.of("../keyext.solidity.examples/functionBody/archive.key");
        }
        KeYEnvironment<?> env = KeYEnvironment.load(file);
        Proof proof = env.getLoadedProof();
        ProofContext context = new ProofContext();
        ProofTreePanel panel = new ProofTreePanel(context);
        context.setProof(env, proof);
        env.getProofControl().startAndWaitForAutoMode(proof);
        context.fireProofChanged();

        assertTrue(panel.search("selectIntOnStore", true), "the rule name occurs in the proof");
        org.key_project.solidity.proof.Node selected = context.getSelectedNode();
        assertTrue(selected != null
                && selected.name().toLowerCase().contains("selectintonstore"),
            "search should select a matching node");
        assertTrue(!panel.search("no-such-rule-xyz", true), "a missing query finds nothing");
    }

    @Test
    void splitRendersTwoSubBranches() throws Exception {
        File tmp = File.createTempFile("split-", ".key");
        // andRight on two distinct uninterpreted predicates splits ==> p & q into ==> p and ==> q.
        Files.writeString(tmp.toPath(), "\\predicates { p; q; }\n\\problem { p & q }\n");
        try {
            KeYEnvironment<?> env = KeYEnvironment.load(tmp.toPath());
            Proof proof = env.getLoadedProof();
            ProofContext context = new ProofContext();
            ProofTreePanel panel = new ProofTreePanel(context);
            context.setProof(env, proof);

            // Apply andRight at the succedent conjunction (the strategy never schedules it).
            Goal goal = proof.openGoals().head();
            SequentFormula sf = goal.sequent().succedent().getFirst();
            PosInOccurrence pos = new PosInOccurrence(sf, PosInTerm.getTopLevel(), false);
            TacletApp andRight = null;
            for (TacletApp app : env.getProofControl().getFindTaclet(goal, pos)) {
                if (app.taclet().name().toString().equals("andRight")) {
                    andRight = app;
                    break;
                }
            }
            assertNotNull(andRight, "andRight should be applicable at p & q");
            goal.apply(andRight.setPosInOccurrence(pos, env.getServices()));
            context.fireProofChanged();

            DefaultMutableTreeNode root = rootOf(panel);
            assertEquals(2, subBranches(root), "andRight should split into two sub-branches");
            assertEquals(proof.countNodes(), totalLeaves(root),
                "every proof node appears exactly once as a leaf");
            assertEquals(2, proof.openGoals().size(), "two open goals after the split");
        } finally {
            tmp.delete();
        }
    }

}
