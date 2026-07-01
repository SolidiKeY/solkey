/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.testutil;

import java.nio.file.Files;
import java.nio.file.Path;

import org.key_project.logic.PosInTerm;
import org.key_project.logic.Term;
import org.key_project.prover.sequent.PosInOccurrence;
import org.key_project.prover.sequent.SequentFormula;
import org.key_project.solidity.control.KeYEnvironment;
import org.key_project.solidity.logic.SolidityBlock;
import org.key_project.solidity.logic.op.SModality;
import org.key_project.solidity.program.ast.statement.Block;
import org.key_project.solidity.proof.Goal;
import org.key_project.solidity.proof.Node;
import org.key_project.solidity.proof.Proof;
import org.key_project.solidity.proof.io.ProblemLoaderException;
import org.key_project.solidity.rule.TacletApp;
import org.key_project.util.collection.ImmutableList;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Shared helpers for the taclet example tests: locate the filesystem example problems, load and
/// run them, and the small proof-inspection utilities (apply a named taclet at the top-level
/// succedent formula, render a proof tree, unwrap a modality's program block) that were previously
/// copy-pasted across the individual test classes.
public final class SolidityExampleTests {

    /// Sentinel for "leave the strategy step budget untouched" in [#prove] (distinct from the real
    /// `-1` "no limit" value KeY itself accepts).
    public static final int KEEP = Integer.MIN_VALUE;

    /// Sentinel for "leave the strategy timeout untouched" in [#prove].
    public static final long KEEP_TIMEOUT = Long.MIN_VALUE;

    private SolidityExampleTests() {}

    // --- locating example problems -------------------------------------------------------------

    /// Resolve an example `.key` file under `keyext.solidity.examples/`, tolerating both working
    /// directories the suite may run from (the module dir or the repo root), and assert it exists.
    ///
    /// @param relativePath path relative to `keyext.solidity.examples/`, e.g.
    /// `"functionBody/archive.key"`
    public static Path example(String relativePath) {
        Path p = Path.of("keyext.solidity.examples").resolve(relativePath);
        Path resolved =
            Files.exists(p) ? p : Path.of("../keyext.solidity.examples").resolve(relativePath);
        assertTrue(Files.exists(resolved), "example must exist: " + resolved.toAbsolutePath());
        return resolved;
    }

    /// Resolve an example directory under `keyext.solidity.examples/` (with the same working-dir
    /// fallback as [#example]), for the parameterized suites that enumerate many files in a dir.
    public static Path examplesDir(String subdir) {
        Path p = Path.of("keyext.solidity.examples").resolve(subdir);
        return Files.exists(p) ? p : Path.of("../keyext.solidity.examples").resolve(subdir);
    }

    // --- loading and proving -------------------------------------------------------------------

    public static KeYEnvironment load(Path file) throws ProblemLoaderException {
        return KeYEnvironment.load(file);
    }

    /// Run automode on the environment's loaded proof, optionally overriding the strategy's step
    /// budget and timeout ([#KEEP] leaves a setting untouched), and return the proof.
    public static Proof prove(KeYEnvironment env, int maxSteps, long timeout) {
        Proof proof = env.getLoadedProof();
        var strategySettings = proof.getSettings().getStrategySettings();
        if (maxSteps != KEEP) {
            strategySettings.setMaxSteps(maxSteps);
        }
        if (timeout != KEEP_TIMEOUT) {
            strategySettings.setTimeout(timeout);
        }
        env.getProofControl().startAndWaitForAutoMode(proof);
        return proof;
    }

    /// Load a file and run automode with the default strategy settings.
    public static Proof loadAndProve(Path file) throws ProblemLoaderException {
        return prove(load(file), KEEP, KEEP_TIMEOUT);
    }

    /// Load a file and run automode with an explicit step budget and timeout.
    public static Proof loadAndProve(Path file, int maxSteps, long timeout)
            throws ProblemLoaderException {
        return prove(load(file), maxSteps, timeout);
    }

    // --- proof inspection ----------------------------------------------------------------------

    /// Find the taclet named `tacletName` applicable at the top-level succedent formula of `goal`,
    /// position it there, assert it is applicable and complete, apply it, and return the positioned
    /// app (callers may still inspect its position or the resulting goal).
    public static TacletApp applyNamedTacletAtTop(KeYEnvironment env, Proof proof, Goal goal,
            String tacletName) {
        SequentFormula sf = goal.sequent().succedent().get(0);
        PosInOccurrence pos = new PosInOccurrence(sf, PosInTerm.getTopLevel(), false);
        ImmutableList<TacletApp> apps = env.getProofControl().getFindTaclet(goal, pos);
        TacletApp app = null;
        for (TacletApp a : apps) {
            if (a.taclet().name().toString().equals(tacletName)) {
                app = a;
                break;
            }
        }
        assertNotNull(app, tacletName + " should be applicable at the top-level succedent formula");
        app = app.setPosInOccurrence(pos, proof.getServices());
        assertTrue(app.complete(), tacletName + " should be complete once positioned");
        goal.apply(app);
        return app;
    }

    /// Unwrap the [Block] program of an [SModality] formula (the modality's program block).
    public static Block modalityProgram(Term formula) {
        assertInstanceOf(SModality.class, formula.op(), "succedent formula must be a modality");
        SolidityBlock sb = ((SModality) formula.op()).programBlock();
        return (Block) sb.program();
    }

    /// Canonical preorder rendering of a proof tree: each node's applied rule name (or `*` for an
    /// open leaf) followed by its children in parentheses. Two trees with the same signature have
    /// the same shape and apply the same rules in the same order at every node.
    public static String treeSignature(Node node) {
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
