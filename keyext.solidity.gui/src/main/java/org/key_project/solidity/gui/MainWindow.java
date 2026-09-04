/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Image;
import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.prefs.Preferences;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JSplitPane;
import javax.swing.JToolBar;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.key_project.solidity.control.KeYEnvironment;
import org.key_project.solidity.pp.NotationInfo;
import org.key_project.solidity.program.parser.SolidityOutline;
import org.key_project.solidity.proof.Node;
import org.key_project.solidity.proof.Proof;
import org.key_project.solidity.proof.Statistics;
import org.key_project.solidity.proof.io.LoadErrors;
import org.key_project.solidity.proof.io.ProofSaver;

import org.jspecify.annotations.Nullable;

/// The single-window MVP over one [ProofContext]: a menu/toolbar (load/save/run), a status bar with
/// a zoom slider, and an IDE-like docked layout. The editor (sequent tabs) is the centre; the proof
/// tree and the open goals — the two navigation lists — are stacked in the left column; the live
/// strategy settings are the right tool window; the node info is the full-width bottom strip. The
/// tool windows are resizable/collapsible via the one-touch split dividers. No multi-proof task
/// management, no KeY-Java specific tooling.
public final class MainWindow extends JFrame {

    private static final int MAX_RECENT = 12;
    private static final String RECENT_KEY = "recentFiles";
    private static final int BASE_FONT_SIZE = 13; // font size at 100% zoom
    private static final String TITLE = "KeYther";

    private final ProofContext context = new ProofContext();
    private final Preferences prefs = Preferences.userNodeForPackage(MainWindow.class);
    private final JMenu recentMenu = new JMenu("Recent files");
    private final JLabel statusLabel = new JLabel("No proof loaded");
    private final StrategyView strategyView = new StrategyView(context);
    private final ProofTreePanel treePanel = new ProofTreePanel(context);
    private final GoalsView goalsView = new GoalsView(context);
    private final NodeInfoView infoView = new NodeInfoView(context);
    private final EditorArea editor = new EditorArea(context, this::runAutoMode);
    private int zoomPercent = 100;
    private boolean busy;
    private boolean proofClosedAnnounced;

    public MainWindow() {
        super(TITLE);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        applyPrettyPrinting(prefs.getBoolean("pp.pretty", true));
        setJMenuBar(buildMenuBar());

        treePanel.setHoverListener(infoView::preview);
        treePanel.setOpenNodeListener(editor::openNode);
        treePanel.setPruneListener(this::prune);

        Icon logo = icon("key-color-icon-square.png", 0); // unscaled, for the window icon
        if (logo instanceof ImageIcon img) {
            setIconImage(img.getImage());
        }

        // IDE-like docked layout. The editor (sequent tabs) is the centre, with its own header.
        // The left column stacks the two *navigation lists* — the proof tree and the open goals —
        // because both want vertical room (a proof can have many goals). The node info is short but
        // wide content (a taclet line is wide), so it gets the full-width bottom strip. The
        // strategy
        // settings are an always-visible right tool window. All dividers are one-touch collapsible.
        JSplitPane leftColumn = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
            toolWindow("Proof tree", treePanel), toolWindow("Open goals", goalsView));
        leftColumn.setResizeWeight(0.6);
        leftColumn.setDividerLocation(260); // give the goals list real height by default
        leftColumn.setOneTouchExpandable(true);

        // Editor (grows) on the left, strategy tool window pinned to the right.
        JSplitPane centreRight = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, editor,
            toolWindow("Strategy", strategyView));
        centreRight.setResizeWeight(1.0);
        centreRight.setOneTouchExpandable(true);

        JSplitPane editorSplit =
            new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftColumn, centreRight);
        editorSplit.setDividerLocation(300);
        editorSplit.setOneTouchExpandable(true);

        JSplitPane mainSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, editorSplit,
            toolWindow("Node info", infoView));
        mainSplit.setResizeWeight(1.0);
        mainSplit.setOneTouchExpandable(true);

        add(buildToolBar(), BorderLayout.NORTH);
        add(mainSplit, BorderLayout.CENTER);
        add(buildStatusBar(), BorderLayout.SOUTH);
        applyAllFonts();

        // Sensible initial divider positions once the frame has a size (fractions need a realised
        // component). The strategy pane sits ~230px from the right; node info is a short bottom
        // band.
        SwingUtilities.invokeLater(() -> {
            centreRight.setDividerLocation(centreRight.getWidth() - 230);
            mainSplit.setDividerLocation(mainSplit.getHeight() - 150);
        });

        // Keep the status bar in sync with the proof state, and announce a proof that closes.
        context.addListener(new ProofContext.Listener() {
            @Override
            public void proofLoaded() {
                Proof loaded = context.getProof();
                // A proof that arrives closed (a replayed .proof) is not news: only a proof that
                // closes here is announced.
                proofClosedAnnounced = loaded != null && loaded.closed();
                updateStatus();
            }

            @Override
            public void proofChanged() {
                updateStatus();
                announceProofClosed();
            }
        });

        rebuildRecentMenu();
        setSize(1100, 700);
        setLocationRelativeTo(null);
    }

    /// Reflects the live proof state (what is being proved, node count, remaining goals, closed)
    /// in the status bar and the window title.
    private void updateStatus() {
        Proof proof = context.getProof();
        if (proof == null) {
            statusLabel.setText("No proof loaded");
            setTitle(TITLE);
            return;
        }
        int nodes = proof.countNodes();
        int open = proof.openGoals().size();
        String state = proof.closed() ? "proof closed ✓"
                : open + (open == 1 ? " open goal" : " open goals");
        String subject = describe(proof);
        statusLabel.setText(subject + "   ·   " + nodes + (nodes == 1 ? " node" : " nodes")
            + "   ·   " + state);
        setTitle(TITLE + " — " + subject);
    }

    /// What a proof is about: for an obligation synthesized from a Solidity source, the file and
    /// the `Contract.function` it was generated for; otherwise the problem's own name.
    private static String describe(Proof proof) {
        Path source = proof.getSoliditySource();
        return source == null ? proof.name().toString()
                : source.getFileName() + "  ·  " + proof.name();
    }

    private JMenuBar buildMenuBar() {
        JMenuBar bar = new JMenuBar();

        JMenu file = new JMenu("File");
        file.add(menuItem("Open Solidity source or problem ...", this::openProof));
        file.add(menuItem("Reopen most recent", this::openMostRecent));
        file.add(recentMenu);
        file.add(menuItem("Save proof ...", this::saveProof));
        file.addSeparator();
        file.add(menuItem("Preferences ...", this::openPreferences));
        file.add(menuItem("Exit", this::dispose));

        JMenu view = new JMenu("View");
        JCheckBoxMenuItem pretty = new JCheckBoxMenuItem("Use pretty syntax",
            prefs.getBoolean("pp.pretty", true));
        pretty.addActionListener(e -> setPrettyPrinting(pretty.isSelected()));
        view.add(pretty);
        view.add(menuItem("Font ...", this::openFontPreferences));

        JMenu proof = new JMenu("Proof");
        proof.add(menuItem("Run auto mode", this::runAutoMode));
        proof.add(menuItem("Prune proof at selected node", this::pruneAtSelected));

        bar.add(file);
        bar.add(view);
        bar.add(proof);
        return bar;
    }

    /// Sets the global pretty-printing default (pretty notation + abbreviated, unambiguous field
    /// names), persists it, and re-renders the views so the change is visible immediately.
    ///
    /// @param on whether pretty printing is enabled
    private void setPrettyPrinting(boolean on) {
        prefs.putBoolean("pp.pretty", on);
        applyPrettyPrinting(on);
        goalsView.invalidatePrinter(); // its reused printer captured the previous setting
        context.fireProofChanged();
    }

    /// Applies the pretty-printing default to the notation used by every freshly created printer.
    /// When off, terms print in raw form, including fully qualified `contract$struct$…$field`
    /// names.
    ///
    /// @param on whether pretty printing is enabled
    private void applyPrettyPrinting(boolean on) {
        NotationInfo.DEFAULT_PRETTY_SYNTAX = on;
        NotationInfo.DEFAULT_HIDE_FIELD_PREFIX = on;
    }

    /// Opens the preferences dialog focused on the font settings.
    private void openFontPreferences() {
        new PreferencesDialog(this, prefs, this::applyAllFonts, true).setVisible(true);
    }

    private JToolBar buildToolBar() {
        JToolBar bar = new JToolBar();
        bar.setFloatable(false);
        // A thin band between the menu bar and the content, so the toolbar reads as its own strip.
        bar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 1, 0, Theme.hairline()),
            BorderFactory.createEmptyBorder(3, 6, 3, 6)));
        bar.add(toolButton("Open", "Load a .sol source, a .key problem or a .proof",
            icon("open.png", 18), this::openProof));
        bar.add(toolButton("Reopen", "Reopen the most recently opened file",
            icon("openMostRecent.png", 18), this::openMostRecent));
        bar.add(toolButton("Save", "Save the current proof", icon("saveFile.png", 18),
            this::saveProof));
        bar.addSeparator();
        bar.add(toolButton("Run prover", "Run auto mode on the current proof",
            icon("autoModeStart.png", 18), this::runAutoMode));
        bar.add(toolButton("Prune", "Prune the proof at the selected node",
            icon("pruneProof.png", 18), this::pruneAtSelected));
        return bar;
    }

    /// Loads a bundled KeY icon by name, scaled to `size` px (or unscaled when `size <= 0`);
    /// returns
    /// `null` when the resource is missing.
    private @Nullable Icon icon(String name, int size) {
        URL url = MainWindow.class.getResource("/org/key_project/solidity/gui/icons/" + name);
        if (url == null) {
            return null;
        }
        ImageIcon raw = new ImageIcon(url);
        if (size <= 0) {
            return raw;
        }
        return new ImageIcon(raw.getImage().getScaledInstance(size, size, Image.SCALE_SMOOTH));
    }

    /// Wraps a section in an IDE-like tool window: a thin title bar above the content.
    private JComponent toolWindow(String title, JComponent content) {
        JPanel panel = new JPanel(new BorderLayout());

        JLabel titleBar = new JLabel(title.toUpperCase());
        titleBar.setHorizontalAlignment(SwingConstants.LEFT);
        titleBar.setFont(titleBar.getFont().deriveFont(Font.BOLD, 11f));
        titleBar.setForeground(Theme.mutedText());
        titleBar.setOpaque(true);
        titleBar.setBackground(Theme.surface());
        Border line = BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.hairline());
        Border pad = BorderFactory.createEmptyBorder(3, 8, 3, 8);
        titleBar.setBorder(BorderFactory.createCompoundBorder(line, pad));

        panel.add(titleBar, BorderLayout.NORTH);
        panel.add(content, BorderLayout.CENTER);
        return panel;
    }

    private JComponent buildStatusBar() {
        JPanel bar = new JPanel(new BorderLayout());
        // A hairline above the status bar separates it from the panes.
        bar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.hairline()),
            BorderFactory.createEmptyBorder(3, 8, 3, 8)));

        JPanel zoom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        JSlider slider = new JSlider(50, 200, zoomPercent);
        slider.setPreferredSize(new Dimension(150, slider.getPreferredSize().height));
        slider.setToolTipText("Zoom (percentage of the configured font size)");
        JLabel value = new JLabel(zoomPercent + "%");
        slider.addChangeListener(e -> {
            zoomPercent = slider.getValue();
            value.setText(zoomPercent + "%");
            applyAllFonts();
        });
        zoom.add(new JLabel("Zoom"));
        zoom.add(slider);
        zoom.add(value);

        bar.add(statusLabel, BorderLayout.WEST);
        bar.add(zoom, BorderLayout.EAST);
        return bar;
    }

    /// Applies the configured fonts (global, with optional per-pane overrides, scaled by the zoom
    /// percentage) to every pane.
    private void applyAllFonts() {
        treePanel.applyFont(fontFor("tree"));
        editor.applyFont(fontFor("sequent"));
        infoView.applyFont(fontFor("info"));
        goalsView.applyFont(fontFor("goals"));
    }

    /// The effective font for a pane: its per-pane override family/size if set, otherwise the
    /// global
    /// family/size, scaled by the current zoom percentage.
    private Font fontFor(String pane) {
        String globalFamily = prefs.get("font.family", Font.MONOSPACED);
        int globalSize = prefs.getInt("font.size", BASE_FONT_SIZE);
        String family = prefs.get("font." + pane + ".family", globalFamily);
        int base = prefs.getInt("font." + pane + ".size", globalSize);
        int size = Math.max(6, Math.round(base * zoomPercent / 100f));
        return new Font(family, Font.PLAIN, size);
    }

    private void openPreferences() {
        new PreferencesDialog(this, prefs, this::applyAllFonts).setVisible(true);
    }

    private JMenuItem menuItem(String label, Runnable action) {
        JMenuItem item = new JMenuItem(label);
        item.addActionListener(e -> action.run());
        return item;
    }

    private JButton toolButton(String label, String tip, @Nullable Icon icon, Runnable action) {
        JButton button = new JButton(label);
        if (icon != null) {
            button.setIcon(icon);
        }
        button.setToolTipText(tip);
        button.setFocusable(false);
        button.addActionListener(e -> action.run());
        return button;
    }

    // ── Recent files ────────────────────────────────────────────────────────

    private List<String> recentFiles() {
        String stored = prefs.get(RECENT_KEY, "");
        return stored.isEmpty() ? new ArrayList<>()
                : new ArrayList<>(Arrays.asList(stored.split("\n")));
    }

    private void addRecentFile(File file) {
        String path = file.getAbsolutePath();
        List<String> recent = recentFiles();
        recent.remove(path);
        recent.add(0, path);
        while (recent.size() > MAX_RECENT) {
            recent.remove(recent.size() - 1);
        }
        prefs.put(RECENT_KEY, String.join("\n", recent));
        rebuildRecentMenu();
    }

    private void rebuildRecentMenu() {
        recentMenu.removeAll();
        List<String> recent = recentFiles();
        if (recent.isEmpty()) {
            JMenuItem none = new JMenuItem("(none)");
            none.setEnabled(false);
            recentMenu.add(none);
            return;
        }
        for (String path : recent) {
            File f = new File(path);
            JMenuItem item = new JMenuItem(f.getName());
            item.setToolTipText(path);
            item.addActionListener(e -> openProof(f));
            recentMenu.add(item);
        }
        recentMenu.addSeparator();
        recentMenu.add(menuItem("Clear recent files", () -> {
            prefs.remove(RECENT_KEY);
            rebuildRecentMenu();
        }));
    }

    private @Nullable File lastDirectory() {
        List<String> recent = recentFiles();
        return recent.isEmpty() ? null : new File(recent.get(0)).getParentFile();
    }

    // ── Actions ─────────────────────────────────────────────────────────────

    private void openProof() {
        JFileChooser chooser = new JFileChooser(lastDirectory());
        chooser.addChoosableFileFilter(
            new FileNameExtensionFilter("Solidity source (*.sol)", "sol"));
        chooser.addChoosableFileFilter(
            new FileNameExtensionFilter("KeY problem or proof (*.key, *.proof)", "key", "proof"));
        chooser.setFileFilter(new FileNameExtensionFilter(
            "Solidity source, KeY problem or proof (*.sol, *.key, *.proof)", "sol", "key",
            "proof"));
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            openProof(chooser.getSelectedFile());
        }
    }

    /// Opens `file`, as the Open action does.
    public void open(File file) {
        openProof(file);
    }

    /// Opens a file by what it is: a Solidity source asks which of its functions to verify, a
    /// `.key` problem or `.proof` loads directly.
    private void openProof(File file) {
        if (busy) {
            showError("Another file is still loading.");
            return;
        }
        if (!file.isFile()) {
            showError("No such file: " + file);
            return;
        }
        if (file.getName().endsWith(".sol")) {
            openSolidity(file);
        } else {
            loadEnvironment(file, file.getName(), () -> KeYEnvironment.load(file.toPath()));
        }
    }

    /// Reads what `file` declares, asks which function to verify, and loads that function's
    /// obligation. Both steps run off the EDT: solc is forked to read the outline, and building
    /// the obligation parses the taclet base.
    private void openSolidity(File file) {
        Path path = file.toPath();
        inBackground("solidity-outline", "Reading " + file.getName() + " ...",
            () -> new SolidityFile(SolidityOutline.of(path), Files.readAllBytes(path)),
            read -> FunctionSelectionDialog
                    .select(this, path, read.outline(), read.source(), fontFor("sequent"))
                    .ifPresent(spec -> loadEnvironment(file,
                        file.getName() + "  ·  " + spec.contract() + "." + spec.function(),
                        () -> KeYEnvironment.load(path, spec.contract(), spec.function()))),
            "Could not read " + file);
    }

    /// Loads an environment off the EDT and installs its proof.
    private void loadEnvironment(File file, String subtitle,
            Callable<KeYEnvironment<?>> loader) {
        inBackground("solidity-load", "Loading " + subtitle + " ...", loader, env -> {
            Proof proof = env.getLoadedProof();
            if (proof == null) {
                showError("No proof was loaded from " + file);
                return;
            }
            context.setProof(env, proof);
            addRecentFile(file);
        }, "Could not load " + file);
    }

    /// Runs `work` on a background thread and hands the result to `onSuccess` on the EDT, with the
    /// window marked busy in between so a second load cannot start on top of the first.
    ///
    /// `work` may throw anything: solc reports a rejected source as an unchecked exception, so
    /// narrowing this to [java.io.IOException] would let real failures escape onto the thread.
    private <T> void inBackground(String threadName, String status, Callable<T> work,
            Consumer<T> onSuccess, String errorContext) {
        setBusy(true, status);
        Thread worker = new Thread(() -> {
            final T value;
            try {
                value = work.call();
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    setBusy(false, null);
                    showError(errorContext + ":\n" + LoadErrors.describe(ex));
                });
                return;
            }
            SwingUtilities.invokeLater(() -> {
                setBusy(false, null);
                onSuccess.accept(value);
            });
        }, threadName);
        worker.setDaemon(true);
        worker.start();
    }

    /// Marks the window busy: the strategy controls are locked and further loads are refused
    /// until the running one finishes.
    private void setBusy(boolean running, @Nullable String status) {
        busy = running;
        strategyView.setRunning(running);
        if (status != null) {
            statusLabel.setText(status);
        } else {
            updateStatus();
        }
    }

    /// What a background read of a `.sol` produced: its outline and the bytes its source spans
    /// index into.
    private record SolidityFile(SolidityOutline outline, byte[] source) {
    }

    private void saveProof() {
        Proof proof = context.getProof();
        if (proof == null) {
            showError("No proof is open.");
            return;
        }
        JFileChooser chooser = new JFileChooser(lastDirectory());
        chooser.setFileFilter(new FileNameExtensionFilter("KeY proof (*.proof)", "proof"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File file = chooser.getSelectedFile();
        if (!file.getName().endsWith(".proof")) {
            file = new File(file.getParentFile(), file.getName() + ".proof");
        }
        try {
            ProofSaver.saveToFile(file, proof);
            addRecentFile(file);
        } catch (Exception ex) {
            showError("Could not save proof:\n" + ex.getMessage());
        }
    }

    /// Re-opens the file at the top of the recent-files list.
    private void openMostRecent() {
        List<String> recent = recentFiles();
        if (recent.isEmpty()) {
            showError("No recently opened files.");
            return;
        }
        openProof(new File(recent.get(0)));
    }

    /// Prunes the proof at the currently selected node (the node becomes an open goal again).
    private void pruneAtSelected() {
        Node node = context.getSelectedNode();
        if (node == null) {
            showError("No node is selected.");
            return;
        }
        if (node.childrenCount() == 0) {
            showError("Nothing to prune: the selected node is a leaf.");
            return;
        }
        prune(node);
    }

    /// Prunes the proof at `node`, dropping its subtree, then refreshes the views.
    private void prune(Node node) {
        Proof proof = context.getProof();
        if (proof == null) {
            return;
        }
        try {
            if (proof.pruneProof(node) == null) {
                showError("Nothing was pruned: node " + node.getSerialNr()
                    + " is an open goal or lies in a closed branch.");
                return;
            }
            context.fireProofChanged();
            context.setSelectedNode(node);
        } catch (Exception ex) {
            showError("Could not prune the proof:\n" + LoadErrors.describe(ex));
        }
    }

    private void runAutoMode() {
        Proof proof = context.getProof();
        KeYEnvironment<?> env = context.getEnvironment();
        if (proof == null || env == null) {
            showError("No proof is open.");
            return;
        }
        if (busy) {
            showError("A file is still loading.");
            return;
        }
        // Locking the window also locks the strategy controls, so a live edit cannot mutate the
        // settings mid-run, and no load can replace the proof under the running strategy.
        inBackground("solidity-auto-mode", "Running the prover ...", () -> {
            env.getProofControl().startAndWaitForAutoMode(proof);
            return proof;
        }, done -> context.fireProofChanged(), "Auto mode failed");
    }

    /// Tells the user when the proof closes, the way KeY-Java does when the prover finishes, and
    /// re-arms once the proof is reopened by pruning.
    private void announceProofClosed() {
        Proof proof = context.getProof();
        if (proof == null || !proof.closed()) {
            proofClosedAnnounced = false;
            return;
        }
        if (proofClosedAnnounced) {
            return;
        }
        proofClosedAnnounced = true;
        StringBuilder message = new StringBuilder(
            "<html><body style='text-align: center'><b>Proved.</b><br><br><table>");
        for (Statistics.Entry entry : proof.getStatistics().getSummary()) {
            message.append("<tr><td align='left'>").append(entry.label())
                    .append("</td><td align='right'>").append(entry.value())
                    .append("</td></tr>");
        }
        message.append("</table></body></html>");
        JOptionPane.showMessageDialog(this, new JLabel(message.toString()), "Proof closed",
            JOptionPane.INFORMATION_MESSAGE);
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }
}
