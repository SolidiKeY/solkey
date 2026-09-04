/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.gui;

import java.io.File;
import java.io.PrintStream;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import org.key_project.solidity.proof.init.SolidityProblemSpec;

import com.formdev.flatlaf.FlatLightLaf;
import org.jspecify.annotations.Nullable;
import picocli.CommandLine;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

/// Entry point of KeYther, the minimal Solidity prover GUI.
///
/// An optional argument is the file to open on startup — a `.sol` source (which asks which of its
/// functions to verify), a `.key` problem or a `.proof`. `--function` names one function of a
/// `.sol` to go straight to, skipping the picker; the flags are spelled as
/// `org.key_project.solidity.CLI` spells them.
public final class SolidityMain {

    @Parameters(index = "0", arity = "0..1", paramLabel = "FILE",
        description = "the .sol, .key or .proof to open")
    @Nullable
    File file;

    @Option(names = { "-f", "--function" },
        description = "for a .sol FILE: go straight to this function's proof, without the picker")
    @Nullable
    String function;

    @Option(names = { "-c", "--contract" },
        description = "for a .sol FILE: the contract declaring --function, if it declares several")
    @Nullable
    String contract;

    @Option(names = { "-h", "--help" }, usageHelp = true, description = "display this help message")
    boolean usageHelpRequested;

    /// What a command line asks for. `spec` is null when the picker decides which function to
    /// prove, which is also the only case in which a non-`.sol` file is allowed.
    record Invocation(@Nullable File file, @Nullable SolidityProblemSpec spec) {
    }

    /// Parses `args` into what to open. Returns null when the line only asked for `--help`, which
    /// has then already been written to `out`.
    ///
    /// @throws CommandLine.ParameterException on a line the GUI cannot act on — this runs before
    /// the window appears, so a bad invocation fails on the console instead of opening an empty
    /// window
    static @Nullable Invocation parse(String[] args, PrintStream out) {
        SolidityMain cli = new SolidityMain();
        CommandLine cmd = new CommandLine(cli);
        cmd.parseArgs(args);
        if (cmd.isUsageHelpRequested()) {
            cmd.usage(out);
            return null;
        }
        if (cli.file == null) {
            if (cli.function != null || cli.contract != null) {
                throw new CommandLine.ParameterException(cmd,
                    "--function and --contract need a FILE");
            }
            return new Invocation(null, null);
        }
        if (cli.contract != null && cli.function == null) {
            // Without --function the picker infers the contract itself, so -c alone does nothing.
            throw new CommandLine.ParameterException(cmd, "--contract needs --function");
        }
        if (cli.function == null) {
            return new Invocation(cli.file, null);
        }
        if (!cli.file.getName().endsWith(".sol")) {
            throw new CommandLine.ParameterException(cmd,
                "--function and --contract apply to .sol files only");
        }
        return new Invocation(cli.file, new SolidityProblemSpec(cli.contract, cli.function));
    }

    public static void main(String[] args) {
        final Invocation invocation;
        try {
            invocation = parse(args, System.out);
        } catch (CommandLine.ParameterException e) {
            System.err.println(e.getMessage());
            e.getCommandLine().usage(System.err);
            System.exit(2);
            return;
        }
        if (invocation == null) {
            return;
        }
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
            File requested = invocation.file();
            if (requested != null) {
                SolidityProblemSpec spec = invocation.spec();
                if (spec == null) {
                    window.open(requested);
                } else {
                    window.open(requested, spec);
                }
            }
        });
    }
}
