/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.control;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.key_project.logic.Name;
import org.key_project.logic.op.sv.SchemaVariable;
import org.key_project.prover.rules.RuleApp;
import org.key_project.prover.sequent.PosInOccurrence;
import org.key_project.solidity.common.Profile;
import org.key_project.solidity.pp.IdentitySequentPrintFilter;
import org.key_project.solidity.pp.InitialPositionTable;
import org.key_project.solidity.pp.LogicPrinter;
import org.key_project.solidity.pp.NotationInfo;
import org.key_project.solidity.pp.PosInSequent;
import org.key_project.solidity.pp.PosTableLayouter;
import org.key_project.solidity.pp.Range;
import org.key_project.solidity.proof.Goal;
import org.key_project.solidity.proof.Node;
import org.key_project.solidity.proof.Proof;
import org.key_project.solidity.proof.init.SolidityProblemSpec;
import org.key_project.solidity.proof.io.AbstractProblemLoader.ReplayResult;
import org.key_project.solidity.proof.io.IntermediateProofReplayer;
import org.key_project.solidity.proof.io.OutputStreamProofSaver;
import org.key_project.solidity.proof.io.ProblemLoaderException;
import org.key_project.solidity.rule.NoPosTacletApp;
import org.key_project.solidity.rule.TacletApp;
import org.key_project.solidity.rule.sv.VariableSV;
import org.key_project.solidity.rule.taclets.SolFindTaclet;
import org.key_project.solidity.settings.StrategySettings;
import org.key_project.solidity.strategy.StrategyFactory;
import org.key_project.solidity.strategy.StrategyProperties;
import org.key_project.solidity.util.ProofStarter;
import org.key_project.util.collection.ImmutableSLList;

import org.jspecify.annotations.Nullable;

public final class ProofSession {

    public static final Map<String, List<String>> STRATEGY_CHOICES = strategyChoices();

    public record Limits(int maxSteps, long timeout, Map<String, String> strategy) {
        public static Limits defaults() {
            return new Limits(10000, -1, Map.of());
        }
    }

    public record TreeEntry(int serial, int parent, String name, @Nullable String branchLabel,
            String state) {
    }

    public record NodeView(int serial, String sequent, @Nullable String rule,
            @Nullable String ruleName, @Nullable String taclet, boolean openGoal, int children,
            @Nullable String branchLabel) {
    }

    public record RuleOption(int index, String name, String displayName, boolean sequentWide,
            List<String> missing) {
    }

    public record RulesAt(int start, int end, @Nullable String term, List<RuleOption> rules) {
    }

    public record Summary(boolean closed, int openGoals, int nodes, int branches,
            long autoModeMillis) {
    }

    private final Proof proof;
    private final @Nullable ReplayResult replay;
    private final Map<Integer, InitialPositionTable> tables = new HashMap<>();
    private final Map<Integer, IdentitySequentPrintFilter> filters = new HashMap<>();
    private List<TacletApp> lastRules = List.of();
    private @Nullable PosInOccurrence lastPosition;
    private int lastRulesSerial = -1;

    private ProofSession(Proof proof, @Nullable ReplayResult replay) {
        this.proof = proof;
        this.replay = replay;
    }

    public static ProofSession start(Path solFile, SolidityProblemSpec spec, Limits limits)
            throws ProblemLoaderException {
        String malformed = TacletChoices.malformed(spec.choices());
        if (malformed != null) {
            throw new IllegalArgumentException(malformed);
        }
        KeYEnvironment<?> env = KeYEnvironment.load(solFile, spec);
        String unknown = TacletChoices.unknown(spec.choices(), env.getInitConfig().choiceNS());
        if (unknown != null) {
            throw new IllegalArgumentException(unknown);
        }
        ProofSession session = new ProofSession(env.getLoadedProof(), null);
        session.configure(limits);
        session.runAuto(null);
        return session;
    }

    public static ProofSession load(Path keyOrProof, Limits limits, boolean prove)
            throws ProblemLoaderException {
        KeYEnvironment<?> env = KeYEnvironment.load(keyOrProof);
        ProofSession session = new ProofSession(env.getLoadedProof(), env.getReplayResult());
        session.configure(limits);
        if (prove && !session.proof.closed()) {
            session.runAuto(null);
        }
        return session;
    }

    public Proof proof() {
        return proof;
    }

    public List<String> replayErrors() {
        if (replay == null || !replay.hasErrors()) {
            return List.of();
        }
        return replay.getErrorList().stream().map(String::valueOf).toList();
    }

    public void configure(Limits limits) {
        StrategySettings settings = proof.getSettings().getStrategySettings();
        settings.setMaxSteps(limits.maxSteps());
        settings.setTimeout(limits.timeout());
        if (limits.strategy().isEmpty()) {
            return;
        }
        StrategyProperties properties = settings.getActiveStrategyProperties();
        for (Map.Entry<String, String> entry : limits.strategy().entrySet()) {
            List<String> values = STRATEGY_CHOICES.get(entry.getKey());
            if (values == null || !values.contains(entry.getValue())) {
                throw new IllegalArgumentException("no strategy setting " + entry.getKey() + "="
                    + entry.getValue() + "; known: " + STRATEGY_CHOICES);
            }
            properties.setProperty(entry.getKey(), entry.getValue());
        }
        settings.setActiveStrategyProperties(properties);
        Profile profile = proof.getServices().getProfile();
        Name name = settings.getStrategy();
        StrategyFactory factory = name != null && profile.supportsStrategyFactory(name)
                ? profile.getStrategyFactory(name)
                : profile.getDefaultStrategyFactory();
        proof.setActiveStrategy(factory.create(proof, properties));
    }

    public Summary summary() {
        return new Summary(proof.closed(), proof.openGoals().size(), proof.countNodes(),
            leaves(), proof.getAutoModeTime());
    }

    public List<String> openGoalTexts(int max) {
        List<String> texts = new ArrayList<>();
        for (Goal goal : proof.openGoals()) {
            if (texts.size() >= max) {
                break;
            }
            texts.add(
                OutputStreamProofSaver.printSequent(goal.sequent(), goal.getOverlayServices()));
        }
        return texts;
    }

    public Summary runAuto(@Nullable Integer serial) {
        ProofStarter starter = new ProofStarter(null, false);
        starter.init(proof);
        if (serial == null) {
            starter.start();
        } else {
            starter.start(ImmutableSLList.<Goal>nil().prepend(requireGoal(serial)));
        }
        invalidate();
        return summary();
    }

    public List<TreeEntry> tree() {
        List<TreeEntry> entries = new ArrayList<>();
        Deque<Node> stack = new ArrayDeque<>();
        stack.push(proof.root());
        while (!stack.isEmpty()) {
            Node node = stack.pop();
            Node parent = node.parent();
            String state = node.leaf() ? (node.isClosed() ? "closed" : "open") : "inner";
            entries.add(
                new TreeEntry(node.getSerialNr(), parent == null ? -1 : parent.getSerialNr(),
                    node.name(), node.getNodeInfo().getBranchLabel(), state));
            for (int i = node.childrenCount() - 1; i >= 0; i--) {
                stack.push(node.child(i));
            }
        }
        return entries;
    }

    public NodeView node(int serial, boolean prettySyntax) {
        Node node = requireNode(serial);
        boolean previous = NotationInfo.DEFAULT_PRETTY_SYNTAX;
        NotationInfo.DEFAULT_PRETTY_SYNTAX = prettySyntax;
        try {
            PosTableLayouter layouter = PosTableLayouter.positionTable(100);
            LogicPrinter printer =
                new LogicPrinter(new NotationInfo(), proof.getServices(), layouter);
            printer.printSequent(node.sequent());
            tables.put(serial, layouter.getInitialPositionTable());
            IdentitySequentPrintFilter filter = new IdentitySequentPrintFilter();
            filter.setSequent(node.sequent());
            filters.put(serial, filter);
            RuleApp app = node.getAppliedRuleApp();
            String rule = app == null ? null : app.rule().displayName();
            String ruleName = app == null ? null : app.rule().name().toString();
            String taclet = app instanceof TacletApp tacletApp ? tacletText(tacletApp) : null;
            return new NodeView(serial, printer.result(), rule, ruleName, taclet,
                goalOf(node) != null, node.childrenCount(), node.getNodeInfo().getBranchLabel());
        } finally {
            NotationInfo.DEFAULT_PRETTY_SYNTAX = previous;
        }
    }

    public RulesAt rulesAt(int serial, int offset) {
        Goal goal = requireGoal(serial);
        if (!tables.containsKey(serial)) {
            node(serial, true);
        }
        PosInOccurrence position = null;
        int start = -1;
        int end = -1;
        String term = null;
        if (offset >= 0) {
            PosInSequent pis = tables.get(serial).getPosInSequent(offset, filters.get(serial));
            position = pis == null ? null : pis.getPosInOccurrence();
            Range range = tables.get(serial).rangeForIndex(offset);
            if (range != null) {
                start = range.start();
                end = range.end();
            }
            if (position != null) {
                term = String.valueOf(position.subTerm());
            }
        }
        List<TacletApp> termApps = new ArrayList<>();
        List<TacletApp> sequentApps = new ArrayList<>();
        java.util.Set<String> seen = new java.util.HashSet<>();
        if (position != null) {
            for (TacletApp app : goal.ruleAppIndex().getTacletAppAt(position,
                goal.getOverlayServices())) {
                if (app.taclet() instanceof SolFindTaclet) {
                    termApps.add(app);
                } else if (seen.add(app.rule().name().toString())) {
                    sequentApps.add(app);
                }
            }
        }
        for (NoPosTacletApp app : goal.ruleAppIndex()
                .getNoFindTaclet(goal.getOverlayServices())) {
            if (seen.add(app.rule().name().toString())) {
                sequentApps.add(app);
            }
        }
        Comparator<TacletApp> bySpecificity =
            Comparator.comparingInt(ProofSession::specificity).reversed()
                    .thenComparing(app -> app.rule().displayName());
        termApps.sort(bySpecificity);
        sequentApps.sort(bySpecificity);
        List<TacletApp> all = new ArrayList<>(termApps);
        all.addAll(sequentApps);
        lastRules = all;
        lastPosition = position;
        lastRulesSerial = serial;
        List<RuleOption> options = new ArrayList<>();
        for (int i = 0; i < all.size(); i++) {
            TacletApp app = all.get(i);
            List<String> missing = new ArrayList<>();
            TacletApp placed = place(app, i < termApps.size() ? position : null, goal);
            for (SchemaVariable sv : placed.uninstantiatedVars()) {
                missing.add(sv.name().toString());
            }
            options.add(new RuleOption(i, app.rule().name().toString(), app.rule().displayName(),
                i >= termApps.size(), missing));
        }
        return new RulesAt(start, end, term, options);
    }

    public Summary apply(int serial, int index, Map<String, String> instantiations)
            throws Exception {
        if (serial != lastRulesSerial || index < 0 || index >= lastRules.size()) {
            throw new IllegalArgumentException("list the rules at node " + serial + " first");
        }
        Goal goal = requireGoal(serial);
        TacletApp toApply = place(lastRules.get(index), lastPosition, goal);
        if (!toApply.complete()) {
            toApply = complete(toApply, goal, instantiations);
        }
        goal.apply(toApply);
        invalidate();
        return summary();
    }

    public Summary prune(int serial) {
        Node node = requireNode(serial);
        proof.pruneProof(node);
        invalidate();
        return summary();
    }

    public String save() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        new OutputStreamProofSaver(proof).save(out);
        return out.toString(StandardCharsets.UTF_8);
    }

    private TacletApp complete(TacletApp app, Goal goal, Map<String, String> instantiations)
            throws Exception {
        List<SchemaVariable> open = new ArrayList<>();
        for (SchemaVariable sv : app.uninstantiatedVars()) {
            open.add(sv);
        }
        TacletApp current = app;
        for (SchemaVariable sv : open) {
            if (sv instanceof VariableSV vsv) {
                current = IntermediateProofReplayer.parseSV1(current, vsv,
                    required(instantiations, sv), proof.getServices());
            }
        }
        for (SchemaVariable sv : open) {
            if (!(sv instanceof VariableSV)) {
                current = IntermediateProofReplayer.parseSV2(current, sv,
                    required(instantiations, sv), goal);
            }
        }
        if (!current.complete()) {
            throw new IllegalArgumentException(
                app.rule().displayName() + " is still not completely instantiated");
        }
        return current;
    }

    private static String required(Map<String, String> instantiations, SchemaVariable sv) {
        String value = instantiations.get(sv.name().toString());
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("no value for " + sv.name());
        }
        return value.trim();
    }

    private String tacletText(TacletApp app) {
        try {
            LogicPrinter printer = new LogicPrinter(new NotationInfo(), proof.getServices(),
                PosTableLayouter.pure(100));
            printer.printTaclet(app.taclet(), app.instantiations(), true, false);
            return printer.result();
        } catch (RuntimeException e) {
            return app.taclet().toString();
        }
    }

    private void invalidate() {
        tables.clear();
        filters.clear();
        lastRules = List.of();
        lastPosition = null;
        lastRulesSerial = -1;
    }

    private Node requireNode(int serial) {
        Deque<Node> stack = new ArrayDeque<>();
        stack.push(proof.root());
        while (!stack.isEmpty()) {
            Node node = stack.pop();
            if (node.getSerialNr() == serial) {
                return node;
            }
            for (int i = 0; i < node.childrenCount(); i++) {
                stack.push(node.child(i));
            }
        }
        throw new IllegalArgumentException("no node " + serial);
    }

    private int leaves() {
        int leaves = 0;
        Deque<Node> stack = new ArrayDeque<>();
        stack.push(proof.root());
        while (!stack.isEmpty()) {
            Node node = stack.pop();
            if (node.leaf()) {
                leaves++;
            }
            for (int i = 0; i < node.childrenCount(); i++) {
                stack.push(node.child(i));
            }
        }
        return leaves;
    }

    private @Nullable Goal goalOf(Node node) {
        for (Goal goal : proof.openGoals()) {
            if (goal.getNode() == node) {
                return goal;
            }
        }
        return null;
    }

    private Goal requireGoal(int serial) {
        Goal goal = goalOf(requireNode(serial));
        if (goal == null) {
            throw new IllegalArgumentException("node " + serial + " is not an open goal");
        }
        return goal;
    }

    private static TacletApp place(TacletApp app, @Nullable PosInOccurrence position, Goal goal) {
        if (position == null || app.posInOccurrence() != null
                || !(app.taclet() instanceof SolFindTaclet)) {
            return app;
        }
        return app.setPosInOccurrence(position, goal.getOverlayServices());
    }

    private static int specificity(TacletApp app) {
        return app.taclet() instanceof SolFindTaclet find ? find.find().depth() : 0;
    }

    private static Map<String, List<String>> strategyChoices() {
        Map<String, List<String>> choices = new LinkedHashMap<>();
        choices.put(StrategyProperties.STOPMODE_OPTIONS_KEY,
            List.of(StrategyProperties.STOPMODE_DEFAULT, StrategyProperties.STOPMODE_NONCLOSE));
        choices.put(StrategyProperties.SPLITTING_OPTIONS_KEY,
            List.of(StrategyProperties.SPLITTING_NORMAL, StrategyProperties.SPLITTING_DELAYED,
                StrategyProperties.SPLITTING_OFF));
        choices.put(StrategyProperties.FUNCTION_OPTIONS_KEY,
            List.of(StrategyProperties.FUNCTION_EXPAND, StrategyProperties.FUNCTION_CONTRACT,
                StrategyProperties.FUNCTION_NONE));
        choices.put(StrategyProperties.NON_LIN_ARITH_OPTIONS_KEY,
            List.of(StrategyProperties.NON_LIN_ARITH_NONE, StrategyProperties.NON_LIN_ARITH_DEF_OPS,
                StrategyProperties.NON_LIN_ARITH_COMPLETION));
        return java.util.Collections.unmodifiableMap(choices);
    }
}
