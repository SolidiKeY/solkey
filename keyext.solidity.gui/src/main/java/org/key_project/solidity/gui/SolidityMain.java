/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.gui;

import java.io.File;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import com.formdev.flatlaf.FlatLightLaf;

/// Entry point of KeYther, the minimal Solidity prover GUI.
///
/// An optional argument is the file to open on startup — a `.sol` source (which asks which of its
/// functions to verify), a `.key` problem or a `.proof`.
public final class SolidityMain {

    private SolidityMain() {}

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            // A modern, flat cross-platform look (away from the native Aqua/Metal styling).
            try {
                UIManager.put("Component.focusWidth", 1);
                UIManager.put("TabbedPane.tabType", "card");
                UIManager.put("ScrollBar.showButtons", false);
                FlatLightLaf.setup();
            } catch (Exception ex) {
                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                } catch (Exception ignored) {
                    // fall back to the default look and feel
                }
            }
            MainWindow window = new MainWindow();
            window.setVisible(true);
            if (args.length > 0) {
                window.open(new File(args[0]));
            }
        });
    }
}
