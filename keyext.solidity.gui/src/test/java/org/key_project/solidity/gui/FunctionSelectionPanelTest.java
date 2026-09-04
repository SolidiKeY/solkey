/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.gui;

import java.awt.Font;
import java.nio.charset.StandardCharsets;
import java.util.List;
import javax.swing.tree.DefaultMutableTreeNode;

import org.key_project.solidity.program.parser.SolidityOutline;
import org.key_project.solidity.program.parser.SolidityOutline.Span;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Checks the contract/function browser: what it lists, what it refuses to offer as a proof, and
/// what it shows beside the tree. The outline is built in memory, so the test needs neither solc
/// nor a display — only a [javax.swing.JPanel] is constructed, as in [ProofTreeLinearizationTest].
public class FunctionSelectionPanelTest {

    static {
        System.setProperty("java.awt.headless", "true");
    }

    private static final String SOURCE = """
            contract A {
                function ok() public { assert(true); }
                function returnsSomething() public returns (uint r) { r = 1; }
            }
            contract B {
                function pay(address payable a) public { a.transfer(5); }
                function withdraw() public { assert(false); }
            }
            """;

    private static final byte[] BYTES = SOURCE.getBytes(StandardCharsets.UTF_8);

    private static Span spanOf(String snippet) {
        int offset = SOURCE.indexOf(snippet);
        assertTrue(offset >= 0, "snippet must occur in the fixture: " + snippet);
        return new Span(offset, snippet.length());
    }

    private static SolidityOutline outline() {
        var ok = new SolidityOutline.Function("ok", List.of(), 0, "",
            spanOf("function ok() public { assert(true); }"));
        var returnsSomething = new SolidityOutline.Function("returnsSomething", List.of(), 1, "",
            spanOf("function returnsSomething() public returns (uint r) { r = 1; }"));
        var pay = new SolidityOutline.Function("pay",
            List.of(new SolidityOutline.Parameter("a", "address payable")), 0, "",
            spanOf("function pay(address payable a) public { a.transfer(5); }"));
        var withdraw = new SolidityOutline.Function("withdraw", List.of(), 0, "",
            spanOf("function withdraw() public { assert(false); }"));
        return new SolidityOutline(List.of(
            new SolidityOutline.Contract("A", List.of(ok, returnsSomething)),
            new SolidityOutline.Contract("B", List.of(pay, withdraw))));
    }

    private static FunctionSelectionPanel panel() {
        return new FunctionSelectionPanel(outline(), BYTES,
            new Font(Font.MONOSPACED, Font.PLAIN, 12));
    }

    private static DefaultMutableTreeNode contractNode(FunctionSelectionPanel panel, int index) {
        DefaultMutableTreeNode root =
            (DefaultMutableTreeNode) panel.getTree().getModel().getRoot();
        return (DefaultMutableTreeNode) root.getChildAt(index);
    }

    /// Every function is listed, provable or not — the ones that cannot be proved are shown
    /// greyed rather than hidden, so the user is not left wondering where they went.
    @Test
    void listsEveryContractAndFunction() {
        FunctionSelectionPanel panel = panel();
        DefaultMutableTreeNode root =
            (DefaultMutableTreeNode) panel.getTree().getModel().getRoot();

        assertEquals(2, root.getChildCount());
        assertEquals("A", contractNode(panel, 0).getUserObject());
        assertEquals(2, contractNode(panel, 0).getChildCount());
        assertEquals("B", contractNode(panel, 1).getUserObject());
        assertEquals(2, contractNode(panel, 1).getChildCount());
    }

    @Test
    void labelsCarryTheParameterList() {
        var pay = outline().contract("B").orElseThrow().function("pay").orElseThrow();
        assertEquals("pay(address payable a)", FunctionSelectionPanel.label(pay));

        var ok = outline().contract("A").orElseThrow().function("ok").orElseThrow();
        assertEquals("ok()", FunctionSelectionPanel.label(ok));
        assertNull(FunctionSelectionPanel.tooltip(ok));
    }

    @Test
    void tooltipsExplainWhyAFunctionCannotBeProved() {
        var contract = outline().contract("A").orElseThrow();
        String tooltip = FunctionSelectionPanel
                .tooltip(contract.function("returnsSomething").orElseThrow());
        assertNotNull(tooltip);
        assertTrue(tooltip.contains("returns a value"), tooltip);

        String payTooltip = FunctionSelectionPanel
                .tooltip(outline().contract("B").orElseThrow().function("pay").orElseThrow());
        assertNotNull(payTooltip);
        assertTrue(payTooltip.contains("address payable"), payTooltip);
    }

    /// The first provable function is preselected, so the dialog opens ready to start.
    @Test
    void preselectsTheFirstProvableFunction() {
        FunctionSelectionPanel panel = panel();
        assertEquals("A", panel.selection().orElseThrow().contract());
        assertEquals("ok", panel.selection().orElseThrow().function());
    }

    @Test
    void selectingAFunctionShowsItsSource() {
        FunctionSelectionPanel panel = panel();
        panel.select("B", "withdraw");

        assertEquals("B", panel.selection().orElseThrow().contract());
        assertEquals("withdraw", panel.selection().orElseThrow().function());
        assertEquals("function withdraw() public { assert(false); }", panel.sourceText());
        assertTrue(panel.headerText().contains("B.withdraw"), panel.headerText());
        assertTrue(panel.headerText().contains("line 7"), panel.headerText());
    }

    /// A function that cannot be proved still shows its source when clicked; it just cannot be
    /// started.
    @Test
    void aNonProvableFunctionStillShowsItsSource() {
        FunctionSelectionPanel panel = panel();
        panel.select("B", "pay");

        assertTrue(panel.selection().isEmpty());
        assertEquals("function pay(address payable a) public { a.transfer(5); }",
            panel.sourceText());
        assertTrue(panel.headerText().contains("B.pay"), panel.headerText());
    }

    /// The invariant that keeps the dialog from ever starting an impossible proof.
    @Test
    void aFunctionThatCannotBeProvedIsNeverTheSelection() {
        FunctionSelectionPanel panel = panel();
        panel.select("A", "returnsSomething");

        assertTrue(panel.selection().isEmpty());
        assertTrue(panel.sourceText().contains("returnsSomething"), panel.sourceText());
        assertTrue(panel.headerText().contains("not provable"), panel.headerText());
    }

    @Test
    void aMissingSourceRangeShowsAPlaceholder() {
        var orphan = new SolidityOutline.Function("orphan", List.of(), 0, "", Span.NONE);
        FunctionSelectionPanel panel = new FunctionSelectionPanel(
            new SolidityOutline(List.of(new SolidityOutline.Contract("C", List.of(orphan)))),
            BYTES, new Font(Font.MONOSPACED, Font.PLAIN, 12));

        assertEquals("orphan", panel.selection().orElseThrow().function());
        assertEquals("(source not available)", panel.sourceText());
    }

    /// The box directive decides whether leading `require`s are assumptions or obligations, so the
    /// browser says which modality the obligation will use.
    @Test
    void theHeaderNamesTheModality() {
        var boxed = new SolidityOutline.Function("boxed", List.of(), 0,
            "@custom:key box", spanOf("function ok() public { assert(true); }"));
        FunctionSelectionPanel panel = new FunctionSelectionPanel(
            new SolidityOutline(List.of(new SolidityOutline.Contract("C", List.of(boxed)))),
            BYTES, new Font(Font.MONOSPACED, Font.PLAIN, 12));
        assertTrue(panel.headerText().contains("box modality"), panel.headerText());

        FunctionSelectionPanel plain = panel();
        assertTrue(plain.headerText().contains("diamond modality"), plain.headerText());
        assertFalse(plain.headerText().contains("not provable"), plain.headerText());
    }
}
