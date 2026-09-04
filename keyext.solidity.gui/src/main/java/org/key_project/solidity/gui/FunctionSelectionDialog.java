/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Font;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

import org.key_project.solidity.program.parser.SolidityOutline;
import org.key_project.solidity.proof.init.SolidityProblemSpec;

import org.jspecify.annotations.Nullable;

/// Asks which function of a `.sol` file to verify, the way KeY-Java's `ProofManagementDialog` asks
/// which contract to prove: browse what the file offers, pick one, start the proof.
///
/// The browsing itself lives in [FunctionSelectionPanel]; this adds the window, the buttons and
/// the memory of what was chosen last for a file, so reopening it lands on the same function.
///
/// The obligation is built from the file on disk when the proof is started, not from the snippet
/// shown here — editing the `.sol` while this dialog is open therefore proves the newer text.
final class FunctionSelectionDialog extends JDialog {

    /// The last function chosen per file, so reopening a `.sol` preselects it. Session-only, like
    /// `ProofManagementDialog.previouslySelectedContracts`.
    private static final Map<Path, SolidityProblemSpec> LAST_SELECTION = new HashMap<>();

    private final FunctionSelectionPanel panel;
    private final JButton start = new JButton("Start Proof");
    private @Nullable SolidityProblemSpec result;

    private FunctionSelectionDialog(@Nullable Window owner, Path file, SolidityOutline outline,
            byte[] source, Font monospace) {
        super(owner, "Verify a function of " + file.getFileName(),
            ModalityType.APPLICATION_MODAL);
        this.panel = new FunctionSelectionPanel(outline, source, monospace);

        SolidityProblemSpec previous = LAST_SELECTION.get(file.toAbsolutePath());
        if (previous != null && previous.contract() != null && previous.function() != null) {
            panel.select(previous.contract(), previous.function());
        }

        start.addActionListener(e -> onStart());
        panel.setSelectionListener(this::updateStartButton);
        panel.setActivationListener(this::onStart);

        JButton cancel = new JButton("Cancel");
        cancel.addActionListener(e -> dispose());

        JPanel buttons = new JPanel();
        buttons.setLayout(new BoxLayout(buttons, BoxLayout.LINE_AXIS));
        buttons.setBorder(BorderFactory.createEmptyBorder(8, 12, 12, 12));
        buttons.add(Box.createHorizontalGlue());
        buttons.add(cancel);
        buttons.add(Box.createHorizontalStrut(6));
        buttons.add(start);

        add(panel, BorderLayout.CENTER);
        add(buttons, BorderLayout.SOUTH);
        getRootPane().setDefaultButton(start);
        getRootPane().registerKeyboardAction(e -> dispose(),
            KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
            JComponent.WHEN_IN_FOCUSED_WINDOW);
        updateStartButton();
        setSize(900, 560);
        setLocationRelativeTo(owner);
    }

    /// Opens the browser for `file` and returns the obligation to load, or empty when the user
    /// cancelled.
    static Optional<SolidityProblemSpec> select(Component parent, Path file,
            SolidityOutline outline, byte[] source, Font monospace) {
        Window owner = SwingUtilities.getWindowAncestor(parent);
        FunctionSelectionDialog dialog =
            new FunctionSelectionDialog(owner, file, outline, source, monospace);
        dialog.setVisible(true);
        SolidityProblemSpec chosen = dialog.result;
        if (chosen != null) {
            LAST_SELECTION.put(file.toAbsolutePath(), chosen);
        }
        return Optional.ofNullable(chosen);
    }

    private void updateStartButton() {
        start.setEnabled(panel.selection().isPresent());
    }

    private void onStart() {
        panel.selection().ifPresent(spec -> {
            result = spec;
            dispose();
        });
    }
}
