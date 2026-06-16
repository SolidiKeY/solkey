/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.taclets;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import org.key_project.solidity.control.KeYEnvironment;
import org.key_project.solidity.proof.Node;
import org.key_project.solidity.proof.Proof;
import org.key_project.solidity.proof.io.AbstractProblemLoader.ReplayResult;
import org.key_project.solidity.proof.io.ProofSaver;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Round-trip: prove a closing example, save the proof, then reload it and check the saved proof
/// replays back to a closed proof (exercises {@code KeYUserProblemFile.readProof} /
/// {@link org.key_project.solidity.parser.ProofReplayer}).
public class ProofSaveLoadTest {

    private static Path example() {
        Path p = Path.of("examples/fieldAccess/fieldAccess.key");
        return Files.exists(p) ? p
                : Path.of("keyext.solidity.core").resolve("examples/fieldAccess/fieldAccess.key");
    }

    @Test
    void saveThenReloadClosedProof() throws Exception {
        // 1) load + prove
        KeYEnvironment env = KeYEnvironment.load(example());
        Proof proof = env.getLoadedProof();
        env.getProofControl().startAndWaitForAutoMode(proof);
        assertTrue(proof.closed(), "precondition: example should close");

        // 2) save into an unrelated temp dir WITHOUT the .sol source next to it. The example uses
        // a relative \programSource, so the saved proof must rewrite it relative to the proof's
        // (new) directory — still resolving back to the original source, without an absolute path.
        Path dir = Files.createTempDirectory("solkey-roundtrip");
        File out = dir.resolve("fieldAccess.proof").toFile();
        ProofSaver.saveToFile(out, proof);
        String saved = Files.readString(out.toPath());
        assertFalse(saved.contains("\\programSource \"/"),
            "a relative source should not be rewritten to an absolute path, was:\n" + saved);
        assertTrue(saved.contains("\\programSource \"") && saved.contains("Bank.sol"),
            "saved proof should still reference Bank.sol, was:\n" + saved);
        assertTrue(out.length() > 0, "a non-empty proof file should be written");

        // 3) reload and replay
        KeYEnvironment env2 = KeYEnvironment.load(out.toPath());
        Proof reloaded = env2.getLoadedProof();
        assertNotNull(reloaded, "reloaded proof must not be null");

        ReplayResult replay = env2.getReplayResult();
        if (replay != null) {
            assertFalse(replay.hasErrors(),
                "proof replay should report no errors, got: " + replay.getErrorList());
        }
        assertTrue(reloaded.closed(), "the reloaded proof should be closed after replay");

        // 4) structural equivalence: same node count and identical tree of applied rule names
        assertTrue(proof.countNodes() > 1,
            "the proof should be non-trivial so the structural check is meaningful");
        assertEquals(proof.countNodes(), reloaded.countNodes(),
            "reloaded proof should have the same number of nodes");
        assertEquals(treeSignature(proof.root()), treeSignature(reloaded.root()),
            "reloaded proof tree (shape + applied rules) should match the original");
    }

    /// A canonical, preorder rendering of a proof tree: each node contributes the name of its
    /// applied rule (or `*` for an open/leaf node) followed by its children in parentheses. Two
    /// trees with the same signature have the same shape and apply the same rules in the same
    /// order at every node.
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
}
