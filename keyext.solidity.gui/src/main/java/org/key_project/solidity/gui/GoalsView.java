/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.gui;

import java.awt.BorderLayout;
import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;

import org.key_project.solidity.common.Services;
import org.key_project.solidity.pp.LogicPrinter;
import org.key_project.solidity.pp.NotationInfo;
import org.key_project.solidity.pp.PosTableLayouter;
import org.key_project.solidity.proof.Goal;
import org.key_project.solidity.proof.Node;
import org.key_project.solidity.proof.Proof;

import org.jspecify.annotations.Nullable;

/// Lists the open goals of the current proof. Selecting a goal selects its node in the context (and
/// thus in the proof tree and sequent view).
public final class GoalsView extends JPanel implements ProofContext.Listener {

    private final ProofContext context;
    private final DefaultListModel<Goal> model = new DefaultListModel<>();
    private final JList<Goal> list = new JList<>(model);
    private boolean syncing;

    /// Printer reused across the (frequent) cell-renderer repaints; rebuilt only when the proof's
    /// [Services] changes. The cell renderer runs on the EDT only, so single-threaded reuse is
    /// safe.
    private @Nullable LogicPrinter labelPrinter;
    private @Nullable Services labelPrinterServices;

    public GoalsView(ProofContext context) {
        super(new BorderLayout());
        this.context = context;
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setCellRenderer((jl, goal, idx, sel, focus) -> {
            var label = new javax.swing.JLabel(describe(goal));
            label.setToolTipText(label.getText());
            label.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 4, 1, 4));
            label.setOpaque(true);
            if (sel) {
                label.setBackground(jl.getSelectionBackground());
                label.setForeground(jl.getSelectionForeground());
            }
            return label;
        });
        list.addListSelectionListener(e -> {
            if (syncing || e.getValueIsAdjusting()) {
                return;
            }
            Goal g = list.getSelectedValue();
            if (g != null) {
                context.setSelectedNode(g.getNode());
            }
        });
        add(new JScrollPane(list), BorderLayout.CENTER);
        context.addListener(this);
    }

    /// Sets the goals list font (family + size).
    public void applyFont(java.awt.Font font) {
        list.setFont(font);
    }

    /// Drops the cached label printer so the next render rebuilds it; call when a global rendering
    /// setting (e.g. pretty printing) changed, since the reused printer captured the old setting.
    public void invalidatePrinter() {
        labelPrinter = null;
        labelPrinterServices = null;
        list.repaint();
    }

    @Override
    public void proofLoaded() {
        refresh();
    }

    @Override
    public void proofChanged() {
        refresh();
    }

    private void refresh() {
        model.clear();
        Proof proof = context.getProof();
        if (proof != null) {
            for (Goal g : proof.openGoals()) {
                model.addElement(g);
            }
        }
    }

    /// A one-line label for a goal: the node number plus an abbreviated, whitespace-collapsed
    /// rendering of its sequent, so goals can be told apart at a glance. Reuses a single printer
    /// across repaints instead of allocating one per cell.
    private String describe(Goal goal) {
        Node node = goal.getNode();
        Services services = node.proof().getServices();
        if (labelPrinter == null || labelPrinterServices != services) {
            labelPrinter = new LogicPrinter(new NotationInfo(), services, PosTableLayouter.pure());
            labelPrinterServices = services;
        } else {
            labelPrinter.reset();
        }
        labelPrinter.printSequent(node.sequent());
        String sequent = labelPrinter.result().replaceAll("\\s+", " ").trim();
        if (sequent.length() > 90) {
            sequent = sequent.substring(0, 89) + "…";
        }
        return node.getSerialNr() + ":  " + sequent;
    }
}
