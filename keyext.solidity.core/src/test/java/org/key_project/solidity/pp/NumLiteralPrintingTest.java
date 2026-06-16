/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.pp;

import java.nio.file.Path;

import org.key_project.solidity.control.KeYEnvironment;
import org.key_project.solidity.proof.Node;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Checks that integer literals encoded as `Z(d(..(#)))` are printed in decimal (like the
/// KeY-Java logic printer) and that the position table relates the whole number to the `Z` term,
/// without exposing the digit subterms for selection.
public class NumLiteralPrintingTest {

    private static final String EXAMPLE = "org/key_project/solidity/examples/problem1.key";

    private static String render(Node node, PosTableLayouter layouter, KeYEnvironment env) {
        var lp = new LogicPrinter(new NotationInfo(), env.getServices(), layouter);
        lp.printSequent(node.sequent());
        return lp.result();
    }

    @Test
    void numeralPrintsInDecimal() throws Exception {
        Path file = Path.of(
            NumLiteralPrintingTest.class.getClassLoader().getResource(EXAMPLE).toURI());
        KeYEnvironment env = KeYEnvironment.load(file);
        Node root = env.getLoadedProof().root();

        var layouter = PosTableLayouter.positionTable(80);
        String out = render(root, layouter, env);

        // The numeral 10 is rendered in decimal, not as Z(0(1(#))).
        assertTrue(out.contains("10"), () -> "expected decimal numeral in: " + out);
        assertFalse(out.contains("Z("), () -> "numeral should not be printed as Z(...): " + out);

        // The position table must relate the whole "10" to the Z term: selecting any digit yields
        // the full numeral range, so the digit subterms inside Z cannot be selected individually.
        PositionTable pt = layouter.getInitialPositionTable();
        int ten = out.lastIndexOf("10");
        for (int idx : new int[] { ten, ten + 1 }) {
            Range r = pt.rangeForIndex(idx, out.length());
            assertEquals("10", out.substring(r.start(), r.end()),
                () -> "selecting a digit should select the whole numeral, not a sub-digit");
        }
    }
}
