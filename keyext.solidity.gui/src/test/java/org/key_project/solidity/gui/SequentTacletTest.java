/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.gui;

import java.io.File;
import java.nio.file.Files;
import java.util.List;

import org.key_project.prover.sequent.PosInOccurrence;
import org.key_project.solidity.control.KeYEnvironment;
import org.key_project.solidity.pp.PosInSequent;
import org.key_project.solidity.pp.Range;
import org.key_project.solidity.proof.Goal;
import org.key_project.solidity.proof.Proof;
import org.key_project.solidity.rule.TacletApp;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Exercises the sequent view's term-at-position lookup and taclet popup: clicking the succedent
/// conjunction offers andRight and applying it splits the proof.
public class SequentTacletTest {

    static {
        System.setProperty("java.awt.headless", "true");
    }

    @Test
    void offersAndAppliesAndRightAtConjunction() throws Exception {
        File tmp = File.createTempFile("seq-", ".key");
        Files.writeString(tmp.toPath(), "\\predicates { p; q; }\n\\problem { p & q }\n");
        try {
            KeYEnvironment<?> env = KeYEnvironment.load(tmp.toPath());
            Proof proof = env.getLoadedProof();
            ProofContext context = new ProofContext();
            SequentView view = new SequentView(context);
            context.setProof(env, proof);

            String rendered = view.renderedText();
            int offset = rendered.indexOf('&');
            assertTrue(offset >= 0, "the conjunction should be rendered: " + rendered);

            PosInSequent pis = view.posAt(offset);
            assertNotNull(pis, "the position table should map the click to a term");
            PosInOccurrence occ = pis.getPosInOccurrence();

            Goal goal = proof.openGoals().head();
            List<TacletApp> apps = view.applicableTaclets(goal, occ);
            TacletApp andRight = apps.stream()
                    .filter(a -> a.taclet().name().toString().equals("andRight"))
                    .findFirst().orElse(null);
            assertNotNull(andRight, "andRight should be offered at the conjunction; offered: "
                + apps.stream().map(a -> a.taclet().name().toString()).toList());

            view.applyTaclet(andRight, goal, occ);
            assertEquals(3, proof.countNodes(), "andRight splits root into two children");
            assertEquals(2, proof.openGoals().size(), "two open goals after the split");
        } finally {
            tmp.delete();
        }
    }

    @Test
    void clicksResolveToSubtermsWithBounds() throws Exception {
        File tmp = File.createTempFile("seq-", ".key");
        Files.writeString(tmp.toPath(), "\\predicates { p; q; }\n\\problem { p & q }\n");
        try {
            KeYEnvironment<?> env = KeYEnvironment.load(tmp.toPath());
            Proof proof = env.getLoadedProof();
            ProofContext context = new ProofContext();
            SequentView view = new SequentView(context);
            context.setProof(env, proof);
            String s = view.renderedText();

            // The conjunction operator resolves to the whole "p & q" formula (not sequent level),
            // with bounds spanning that formula — the range used to highlight it.
            PosInSequent conj = view.posAt(s.indexOf('&'));
            assertNotNull(conj);
            assertFalse(conj.isSequent(), "clicking a formula must not resolve to the sequent");
            assertNotNull(conj.getPosInOccurrence());
            Range cb = conj.getBounds();
            assertEquals("p & q", s.substring(cb.start(), cb.end()).trim());

            // The atom resolves to the smaller subterm "p".
            PosInSequent atom = view.posAt(s.indexOf('p'));
            assertNotNull(atom);
            assertFalse(atom.isSequent());
            assertTrue(atom.getBounds().length() < cb.length(),
                "a subterm highlights a smaller range than its enclosing formula");
        } finally {
            tmp.delete();
        }
    }
}
