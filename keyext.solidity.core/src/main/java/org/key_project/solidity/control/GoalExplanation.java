/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.control;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.key_project.logic.PosInTerm;
import org.key_project.logic.Term;
import org.key_project.logic.op.Function;
import org.key_project.logic.op.Operator;
import org.key_project.logic.op.sv.SchemaVariable;
import org.key_project.prover.rules.RuleApp;
import org.key_project.prover.rules.Taclet;
import org.key_project.prover.sequent.PosInOccurrence;
import org.key_project.prover.sequent.SequentFormula;
import org.key_project.prover.strategy.costbased.TopRuleAppCost;
import org.key_project.solidity.logic.op.SModality;
import org.key_project.solidity.program.ast.SolidityProgramElement;
import org.key_project.solidity.proof.Goal;
import org.key_project.solidity.proof.Node;
import org.key_project.solidity.proof.NodeInfo;
import org.key_project.solidity.proof.Proof;
import org.key_project.solidity.proof.io.OutputStreamProofSaver;
import org.key_project.solidity.rule.TacletApp;
import org.key_project.solidity.rule.taclets.SolFindTaclet;

public final class GoalExplanation {

    public enum Kind {
        PROGRAM, NO_RULE, REFUSED
    }

    public record Stuck(Kind kind, String term, String operator, int argument,
            String argumentOperator, List<String> rulesForOperator, List<String> refused,
            int occurrences) {
    }

    public record Explanation(int serial, List<String> lastRules, List<Stuck> stuck) {
    }

    private static final int LAST_RULES = 8;
    private static final int MAX_STUCK = 5;
    private static final int MAX_RULE_NAMES = 12;
    private static final int TERM_CHARS = 200;

    private final Proof proof;
    private final Map<String, Map<Integer, Set<String>>> dispatch = new LinkedHashMap<>();
    private final Map<String, Set<String>> rulesByOperator = new LinkedHashMap<>();
    private final Set<String> constructors = new TreeSet<>();

    private GoalExplanation(Proof proof) {
        this.proof = proof;
        for (Taclet taclet : proof.getInitConfig().activatedTaclets()) {
            if (!(taclet instanceof SolFindTaclet find)) {
                continue;
            }
            Term pattern = find.find();
            if (pattern.op() instanceof SchemaVariable) {
                continue;
            }
            String op = name(pattern.op());
            rulesByOperator.computeIfAbsent(op, k -> new TreeSet<>())
                    .add(taclet.name().toString());
            for (int i = 0; i < pattern.arity(); i++) {
                Operator argument = pattern.sub(i).op();
                if (argument instanceof Function f && f.arity() > 0) {
                    dispatch.computeIfAbsent(op, k -> new LinkedHashMap<>())
                            .computeIfAbsent(i, k -> new TreeSet<>()).add(name(argument));
                    constructors.add(name(argument));
                }
            }
        }
    }

    public static List<Explanation> explain(Proof proof, int maxGoals) {
        GoalExplanation explainer = new GoalExplanation(proof);
        List<Explanation> explanations = new ArrayList<>();
        for (Goal goal : proof.openGoals()) {
            if (explanations.size() >= maxGoals) {
                break;
            }
            explanations.add(explainer.explain(goal));
        }
        return explanations;
    }

    private Explanation explain(Goal goal) {
        Map<String, Stuck> found = new LinkedHashMap<>();
        for (boolean antecedent : new boolean[] { false, true }) {
            var formulas = antecedent ? goal.sequent().antecedent() : goal.sequent().succedent();
            for (SequentFormula formula : formulas) {
                visit(goal, new PosInOccurrence(formula, PosInTerm.getTopLevel(), antecedent),
                    found);
            }
        }
        List<Stuck> stuck = new ArrayList<>(found.values());
        stuck.sort(java.util.Comparator.comparing(Stuck::kind));
        return new Explanation(goal.getNode().getSerialNr(), lastRules(goal.getNode()),
            stuck.subList(0, Math.min(MAX_STUCK, stuck.size())));
    }

    private void visit(Goal goal, PosInOccurrence position, Map<String, Stuck> found) {
        Term term = position.subTerm();
        if (term.op() instanceof SModality modality) {
            visitProgram(goal, position, modality, found);
        }
        for (int i = 0; i < term.arity(); i++) {
            visit(goal, position.down(i), found);
        }
        String op = name(term.op());
        Map<Integer, Set<String>> byArgument = dispatch.get(op);
        if (byArgument == null) {
            return;
        }
        for (Map.Entry<Integer, Set<String>> entry : byArgument.entrySet()) {
            int i = entry.getKey();
            if (i >= term.arity() || !(term.sub(i).op() instanceof Function f) || f.arity() == 0
                    || !constructors.contains(name(f))) {
                continue;
            }
            String argument = name(f);
            if (!entry.getValue().contains(argument)) {
                add(found, Kind.NO_RULE, term, op, i, argument, List.of());
                return;
            }
            List<String> refused = refused(goal, position);
            if (refused != null && !refused.isEmpty()) {
                add(found, Kind.REFUSED, term, op, i, argument, refused);
                return;
            }
        }
    }

    private void visitProgram(Goal goal, PosInOccurrence position, SModality modality,
            Map<String, Stuck> found) {
        var program = modality.programBlock().program();
        if (program.getChildCount() == 0) {
            return;
        }
        List<String> refused = refused(goal, position);
        if (refused == null) {
            return;
        }
        SolidityProgramElement active =
            NodeInfo.computeActiveStatement((SolidityProgramElement) program.getChild(0));
        String statement = truncate(String.valueOf(active));
        String key = "program/" + statement;
        Stuck previous = found.get(key);
        found.put(key, new Stuck(Kind.PROGRAM, statement,
            active == null ? "?" : active.getClass().getSimpleName(), -1, "", List.of(),
            refused, previous == null ? 1 : previous.occurrences() + 1));
    }

    private void add(Map<String, Stuck> found, Kind kind, Term term, String op, int argument,
            String argumentOperator, List<String> refused) {
        String key = kind + "/" + op + "/" + argumentOperator;
        Stuck previous = found.get(key);
        if (previous != null) {
            found.put(key, new Stuck(previous.kind(), previous.term(), previous.operator(),
                previous.argument(), previous.argumentOperator(), previous.rulesForOperator(),
                previous.refused(), previous.occurrences() + 1));
            return;
        }
        String text = OutputStreamProofSaver.printTerm(term, proof.getServices());
        List<String> rules = new ArrayList<>(rulesByOperator.getOrDefault(op, Set.of()));
        if (rules.size() > MAX_RULE_NAMES) {
            int more = rules.size() - MAX_RULE_NAMES;
            rules = new ArrayList<>(rules.subList(0, MAX_RULE_NAMES));
            rules.add("... " + more + " more");
        }
        found.put(key,
            new Stuck(kind, truncate(text), op, argument, argumentOperator, rules, refused, 1));
    }

    private List<String> refused(Goal goal, PosInOccurrence position) {
        List<String> refused = new ArrayList<>();
        var strategy = proof.getActiveStrategy();
        for (TacletApp app : goal.ruleAppIndex().getTacletAppAt(position,
            goal.getOverlayServices())) {
            if (!(app.taclet() instanceof SolFindTaclet find) || find.find().arity() == 0
                    || !app.uninstantiatedVars().isEmpty()) {
                continue;
            }
            try {
                var cost = strategy.computeCost(app, position, goal);
                if (cost instanceof TopRuleAppCost) {
                    refused.add(app.rule().name() + " (infinite cost)");
                } else if (!strategy.isApprovedApp(app, position, goal)) {
                    refused.add(app.rule().name() + " (not approved)");
                } else {
                    return null;
                }
            } catch (RuntimeException e) {
                refused.add(app.rule().name() + " (" + e.getClass().getSimpleName() + ")");
            }
        }
        return refused;
    }

    private static List<String> lastRules(Node leaf) {
        List<String> names = new ArrayList<>();
        int repeats = 0;
        for (Node node = leaf.parent(); node != null && names.size() < LAST_RULES; node =
            node.parent()) {
            RuleApp app = node.getAppliedRuleApp();
            if (app == null) {
                continue;
            }
            String name = app.rule().name().toString();
            if (!names.isEmpty() && names.get(names.size() - 1).startsWith(name)) {
                repeats++;
                names.set(names.size() - 1, name + " x" + (repeats + 1));
            } else {
                repeats = 0;
                names.add(name);
            }
        }
        return names;
    }

    private static String name(Operator op) {
        String name = op.name().toString();
        int colons = name.lastIndexOf("::");
        return colons < 0 ? name : name.substring(colons + 2);
    }

    private static String truncate(String text) {
        String flat = text.strip().replaceAll("\\s+", " ");
        return flat.length() <= TERM_CHARS ? flat : flat.substring(0, TERM_CHARS) + "...";
    }

    public static String format(List<Explanation> explanations) {
        StringBuilder out = new StringBuilder();
        for (Explanation explanation : explanations) {
            out.append("--- why goal ").append(explanation.serial()).append(" is open ---\n");
            out.append("last rules (newest first): ")
                    .append(explanation.lastRules().isEmpty() ? "none"
                            : String.join(", ", explanation.lastRules()))
                    .append('\n');
            if (explanation.stuck().isEmpty()) {
                out.append("nothing stuck found: no program left and no read on a constructor"
                    + " without a rule; the goal may be a false or unsupported first-order"
                    + " fact, see --open-goals\n");
            }
            for (Stuck stuck : explanation.stuck()) {
                String times = stuck.occurrences() > 1 ? " (" + stuck.occurrences() + " places)"
                        : "";
                switch (stuck.kind()) {
                    case PROGRAM -> {
                        out.append("stuck program: no rule executes the active statement")
                                .append(times).append('\n');
                        out.append("  statement (").append(stuck.operator()).append("): ")
                                .append(stuck.term()).append('\n');
                        if (!stuck.refused().isEmpty()) {
                            out.append("  matching rules the strategy refuses: ")
                                    .append(String.join(", ", stuck.refused())).append('\n');
                        }
                    }
                    case NO_RULE -> {
                        out.append("stuck read: no rule for ").append(stuck.operator())
                                .append(" on ").append(stuck.argumentOperator())
                                .append(" (argument ").append(stuck.argument()).append(")")
                                .append(times).append('\n');
                        out.append("  term: ").append(stuck.term()).append('\n');
                        out.append("  rules for ").append(stuck.operator()).append(": ")
                                .append(String.join(", ", stuck.rulesForOperator())).append('\n');
                    }
                    case REFUSED -> {
                        out.append("refused: a rule matches ").append(stuck.operator())
                                .append(" on ").append(stuck.argumentOperator())
                                .append(" but the strategy refuses it").append(times).append('\n');
                        out.append("  term: ").append(stuck.term()).append('\n');
                        out.append("  rules: ").append(String.join(", ", stuck.refused()))
                                .append('\n');
                    }
                }
            }
        }
        return out.toString();
    }
}
