/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.taclets;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import org.key_project.solidity.proof.Proof;
import org.key_project.solidity.proof.io.ProofSaver;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.key_project.solidity.testutil.SolidityExampleTests.assertSameProofTree;
import static org.key_project.solidity.testutil.SolidityExampleTests.example;
import static org.key_project.solidity.testutil.SolidityExampleTests.loadAndProve;
import static org.key_project.solidity.testutil.SolidityExampleTests.replay;

/// Round-trip: prove a closing example, save the proof, then reload it and check the saved proof
/// replays back to a closed proof (exercises {@code KeYUserProblemFile.readProof} /
/// {@link org.key_project.solidity.parser.ProofReplayer}).
public class ProofSaveLoadTest {

    @Test
    void saveThenReloadClosedProof() throws Exception {
        // 1) load + prove
        Proof proof = loadAndProve(example("fieldAccess/fieldAccess.key"));
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
        Proof reloaded = replay(out.toPath());
        assertTrue(reloaded.closed(), "the reloaded proof should be closed after replay");

        // 4) structural equivalence: same node count and identical tree of applied rule names
        assertTrue(proof.countNodes() > 1,
            "the proof should be non-trivial so the structural check is meaningful");
        assertSameProofTree("fieldAccess", proof, reloaded);
    }
}
