/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.pp;

import java.nio.file.Path;

import org.key_project.logic.Name;
import org.key_project.logic.Term;
import org.key_project.logic.op.Function;
import org.key_project.prover.rules.Taclet;
import org.key_project.solidity.common.Services;
import org.key_project.solidity.control.KeYEnvironment;
import org.key_project.solidity.proof.Node;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Checks that integer literals encoded as `Z(d(..(#)))` are printed in decimal (like the
/// KeY-Java logic printer) and that the position table relates the whole number to the `Z` term,
/// without exposing the digit subterms for selection.
public class NumLiteralPrintingTest {

    private static final String EXAMPLE = "org/key_project/solidity/examples/problem1.key";

    /// Has a `\sameAsTerm` varcond taclet (`fieldWriteThenRead`) and registers the field constant
    /// `FieldBox$value` via `FieldBox.sol`.
    private static final String FIELD_EXAMPLE =
        "org/key_project/solidity/examples/fieldAccessTest.key";

    private static Path resource(String name) throws Exception {
        return Path.of(NumLiteralPrintingTest.class.getClassLoader().getResource(name).toURI());
    }

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

    @Test
    void varcondPrintsInKeySyntax() throws Exception {
        KeYEnvironment env = KeYEnvironment.load(resource(FIELD_EXAMPLE));
        Taclet taclet = env.getInitConfig().lookupActiveTaclet(new Name("fieldWriteThenRead"));
        assertNotNull(taclet, "taclet fieldWriteThenRead should be loaded");

        var lp = new LogicPrinter(new NotationInfo(), env.getServices(), PosTableLayouter.pure());
        // The 1-arg overload prints the SV declarations too (declareSchemaVars=true); this must not
        // throw even though the taclet declares a modal-operator SV (#mod).
        lp.printTaclet(taclet);
        String out = lp.result();

        // The varcond renders exactly as written in the .key file ...
        assertTrue(out.contains("\\sameAsTerm(fr, path)"),
            () -> "expected \\sameAsTerm(fr, path) in: " + out);
        // ... not via the verbose SchemaVariable toString forms.
        assertFalse(out.contains("((modal operator))"), () -> "verbose SV toString leaked: " + out);
        assertFalse(out.contains(" term "), () -> "verbose SV toString leaked: " + out);
        // The modal-operator schema variable declaration is rendered (no ClassCastException).
        assertTrue(out.contains("\\modalOperator {"),
            () -> "expected \\modalOperator declaration in: " + out);
        assertTrue(out.contains("#mod"), () -> "expected #mod declaration in: " + out);
    }

    @Test
    void unambiguousFieldPrintsShortFormViaToggle() throws Exception {
        KeYEnvironment env = KeYEnvironment.load(resource(FIELD_EXAMPLE));
        Services services = env.getServices();
        Function field = services.getNamespaces().functions().lookup(new Name("FieldBox$value"));
        assertNotNull(field, "field constant FieldBox$value should be registered");
        Term term = services.getTermBuilder().func(field);

        // Default: the unambiguous simple field name is printed.
        var shortPrinter = new LogicPrinter(new NotationInfo(), services, PosTableLayouter.pure());
        shortPrinter.printTerm(term);
        assertEquals("value", shortPrinter.result().trim());

        // Toggle off: the full qualified name is printed.
        NotationInfo full = new NotationInfo();
        full.setHideFieldPrefix(false);
        var fullPrinter = new LogicPrinter(full, services, PosTableLayouter.pure());
        fullPrinter.printTerm(term);
        assertEquals("FieldBox$value", fullPrinter.result().trim());
    }
}
