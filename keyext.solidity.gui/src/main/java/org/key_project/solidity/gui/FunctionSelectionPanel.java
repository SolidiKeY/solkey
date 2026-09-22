/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Font;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import org.key_project.solidity.program.parser.SolidityOutline;
import org.key_project.solidity.proof.init.SolidityProblemSpec;
import org.key_project.solidity.proof.init.SolidityProblemSynthesizer;

import org.jspecify.annotations.Nullable;

/// The contract/function browser of the Solidity open flow: a tree of the contracts a `.sol`
/// declares and the functions each one exposes, beside the Solidity source of whichever function
/// is selected.
///
/// This is the Solidity analogue of KeY-Java's `ContractSelectionPanel`, and it is a [JPanel] for
/// the same reason that one is: a panel can be built without a display, so the selection logic is
/// testable headlessly, while [FunctionSelectionDialog] adds only the window around it.
///
/// A function no obligation can be generated for is shown greyed out, with the reason as its
/// tooltip, and can never become the [#selection] — the browser must not offer a proof that
/// cannot be started.
final class FunctionSelectionPanel extends JPanel {

    private final byte[] source;
    private final JTree tree;
    private final JTextArea sourceText = new JTextArea();
    private final JLabel header = new JLabel(" ");
    private @Nullable Runnable selectionListener;

    FunctionSelectionPanel(SolidityOutline outline, byte[] source, Font monospace) {
        super(new BorderLayout());
        this.source = source;
        this.tree = new JTree(new DefaultTreeModel(rootOf(outline)));

        tree.setRootVisible(false);
        tree.setShowsRootHandles(true);
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        tree.setCellRenderer(new EntryRenderer());
        tree.addTreeSelectionListener(e -> onSelectionChanged());
        for (int row = 0; row < tree.getRowCount(); row++) {
            tree.expandRow(row);
        }

        sourceText.setEditable(false);
        sourceText.setFont(monospace);
        sourceText.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));

        header.setFont(header.getFont().deriveFont(Font.BOLD));
        header.setForeground(Theme.mutedText());
        header.setOpaque(true);
        header.setBackground(Theme.surface());
        header.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.hairline()),
            BorderFactory.createEmptyBorder(3, 8, 3, 8)));

        JPanel detail = new JPanel(new BorderLayout());
        detail.add(header, BorderLayout.NORTH);
        detail.add(new JScrollPane(sourceText), BorderLayout.CENTER);

        JSplitPane split =
            new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, new JScrollPane(tree), detail);
        split.setDividerLocation(280);
        add(split, BorderLayout.CENTER);

        selectFirstProvable();
    }

    /// The chosen obligation, or empty while the selection is a contract or a function no
    /// obligation can be generated for.
    Optional<SolidityProblemSpec> selection() {
        Entry entry = selectedEntry();
        if (entry == null || !entry.function().isProvable()) {
            return Optional.empty();
        }
        return Optional.of(SolidityProblemSpec.of(entry.contract(), entry.function().name()));
    }

    /// Selects `function` of `contract` when the file still declares it; leaves the selection
    /// untouched otherwise.
    void select(String contract, String function) {
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) tree.getModel().getRoot();
        for (int c = 0; c < root.getChildCount(); c++) {
            DefaultMutableTreeNode contractNode = (DefaultMutableTreeNode) root.getChildAt(c);
            for (int f = 0; f < contractNode.getChildCount(); f++) {
                DefaultMutableTreeNode node =
                    (DefaultMutableTreeNode) contractNode.getChildAt(f);
                Entry entry = (Entry) node.getUserObject();
                if (entry.contract().equals(contract)
                        && entry.function().name().equals(function)) {
                    tree.setSelectionPath(new TreePath(node.getPath()));
                    return;
                }
            }
        }
    }

    /// Notified whenever the selection changes, so the dialog can follow it with its button.
    void setSelectionListener(Runnable listener) {
        this.selectionListener = listener;
    }

    /// Runs `action` when a provable function is double-clicked.
    void setActivationListener(Runnable action) {
        tree.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2 && tree.getRowForLocation(e.getX(), e.getY()) != -1
                        && selection().isPresent()) {
                    action.run();
                }
            }
        });
    }

    /// The Solidity source currently shown beside the tree.
    String sourceText() {
        return sourceText.getText();
    }

    /// The detail pane's caption: what is selected and, for a function, where it starts and which
    /// modality its obligation will use.
    String headerText() {
        return header.getText();
    }

    /// Package-private access to the underlying tree, for tests.
    JTree getTree() {
        return tree;
    }

    /// A function's label: its name and parameter list, as it would be called.
    static String label(SolidityOutline.Function function) {
        return function.name() + "(" + function.parameters().stream()
                .map(p -> p.type() + " " + p.name()).collect(Collectors.joining(", "))
            + ")";
    }

    /// Why the function cannot be proved, or `null` when it can.
    static @Nullable String tooltip(SolidityOutline.Function function) {
        return function.unsupportedReason().orElse(null);
    }

    private static DefaultMutableTreeNode rootOf(SolidityOutline outline) {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("contracts");
        for (SolidityOutline.Contract contract : outline.contracts()) {
            DefaultMutableTreeNode contractNode = new DefaultMutableTreeNode(contract.name());
            for (SolidityOutline.Function function : contract.functions()) {
                contractNode.add(new DefaultMutableTreeNode(
                    new Entry(contract.name(), function), false));
            }
            root.add(contractNode);
        }
        return root;
    }

    private void selectFirstProvable() {
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) tree.getModel().getRoot();
        for (int c = 0; c < root.getChildCount(); c++) {
            DefaultMutableTreeNode contractNode = (DefaultMutableTreeNode) root.getChildAt(c);
            for (int f = 0; f < contractNode.getChildCount(); f++) {
                DefaultMutableTreeNode node =
                    (DefaultMutableTreeNode) contractNode.getChildAt(f);
                if (((Entry) node.getUserObject()).function().isProvable()) {
                    tree.setSelectionPath(new TreePath(node.getPath()));
                    return;
                }
            }
        }
        onSelectionChanged();
    }

    private @Nullable Entry selectedEntry() {
        TreePath path = tree.getSelectionPath();
        if (path == null
                || !(path.getLastPathComponent() instanceof DefaultMutableTreeNode node)
                || !(node.getUserObject() instanceof Entry entry)) {
            return null;
        }
        return entry;
    }

    private void onSelectionChanged() {
        Entry entry = selectedEntry();
        if (entry == null) {
            header.setText("Select a function to verify");
            header.setToolTipText(null);
            sourceText.setText("");
        } else {
            SolidityOutline.Function function = entry.function();
            String text = function.source().textIn(source);
            header.setText(entry.contract() + "." + function.name() + "  ·  line "
                + function.source().lineIn(source) + "  ·  " + modalityOf(function)
                + function.unsupportedReason().map(r -> "  ·  not provable: " + r).orElse(""));
            header.setToolTipText(header.getText());
            sourceText.setText(text.isEmpty() ? "(source not available)" : text);
            sourceText.setCaretPosition(0);
        }
        if (selectionListener != null) {
            selectionListener.run();
        }
    }

    /// Which modality the synthesized obligation will use — the natspec directive that decides it
    /// is part of what the user is choosing.
    private static String modalityOf(SolidityOutline.Function function) {
        return function.documentation().contains(SolidityProblemSynthesizer.BOX_DIRECTIVE)
                ? "box modality"
                : "diamond modality";
    }

    /// One selectable function of one contract.
    private record Entry(String contract, SolidityOutline.Function function) {
        @Override
        public String toString() {
            return label(function);
        }
    }

    /// Greys out the functions no obligation can be generated for and explains why on hover.
    private static final class EntryRenderer extends DefaultTreeCellRenderer {

        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected,
                boolean expanded, boolean leaf, int row, boolean hasFocus) {
            super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row,
                hasFocus);
            setToolTipText(null);
            if (value instanceof DefaultMutableTreeNode node
                    && node.getUserObject() instanceof Entry entry
                    && !entry.function().isProvable()) {
                setForeground(Theme.mutedText());
                setToolTipText(tooltip(entry.function()));
            }
            return this;
        }
    }
}
