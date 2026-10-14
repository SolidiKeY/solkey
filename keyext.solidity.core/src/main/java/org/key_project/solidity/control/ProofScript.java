/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.control;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.key_project.solidity.proof.Goal;

public final class ProofScript {

    public static final String HELP = """
            commands, separated by ';' or newlines; GOAL defaults to the first open goal:
              auto [STEPS]                      run the strategy on every open goal
              auto goal N [STEPS]               run it on one goal
              rule NAME [on "TEXT"] [goal N] [with SV="VALUE" ...]
                                                apply a rule at the first occurrence of TEXT
                                                (no TEXT: a sequent-wide rule)
              rules [on "TEXT"] [goal N]        list the rules matching there, with the
                                                strategy's verdict on each
              show [goal N]                     print the sequent
              goals                             list the open goals
              tree                              print the proof tree
              why                               explain why each open goal is open
              prune N                           prune the proof below node N""";

    private final ProofSession session;
    private final PrintStream out;

    public ProofScript(ProofSession session, PrintStream out) {
        this.session = session;
        this.out = out;
    }

    public void run(String script) throws Exception {
        for (List<String> command : parse(script)) {
            out.println("> " + String.join(" ", command));
            execute(command);
        }
    }

    static List<List<String>> parse(String script) {
        List<List<String>> commands = new ArrayList<>();
        List<String> current = new ArrayList<>();
        StringBuilder token = new StringBuilder();
        boolean quoted = false;
        boolean inToken = false;
        for (int i = 0; i < script.length(); i++) {
            char c = script.charAt(i);
            if (quoted) {
                if (c == '\\' && i + 1 < script.length()) {
                    token.append(script.charAt(++i));
                } else if (c == '"') {
                    quoted = false;
                } else {
                    token.append(c);
                }
            } else if (c == '"') {
                quoted = true;
                inToken = true;
            } else if (c == ';' || c == '\n' || Character.isWhitespace(c)) {
                if (inToken) {
                    current.add(token.toString());
                    token.setLength(0);
                    inToken = false;
                }
                if ((c == ';' || c == '\n') && !current.isEmpty()) {
                    commands.add(current);
                    current = new ArrayList<>();
                }
            } else {
                token.append(c);
                inToken = true;
            }
        }
        if (quoted) {
            throw new IllegalArgumentException("unterminated quote in script");
        }
        if (inToken) {
            current.add(token.toString());
        }
        if (!current.isEmpty()) {
            commands.add(current);
        }
        return commands;
    }

    private void execute(List<String> command) throws Exception {
        Arguments args = new Arguments(command.subList(1, command.size()));
        switch (command.get(0)) {
            case "auto" -> {
                Integer goal = args.option("goal");
                String steps = args.positional();
                ProofSession.Limits previous = currentLimits();
                if (steps != null) {
                    session.configure(previous.withMaxSteps(Integer.parseInt(steps)));
                }
                try {
                    out.println(describe(session.runAutoDetailed(goal)));
                } finally {
                    session.configure(previous);
                }
            }
            case "rule" -> {
                String name = args.required("rule NAME");
                String text = args.text("on");
                int goal = goal(args.option("goal"));
                Map<String, String> with = args.assignments("with");
                var summary = text == null && with.isEmpty() ? session.applyRule(goal, name)
                        : text == null ? session.applyRule(goal, -1, name, with)
                                : session.applyRule(goal, text, name, with);
                out.println("applied " + name + "; " + describe(summary));
            }
            case "rules" -> {
                String text = args.text("on");
                int goal = goal(args.option("goal"));
                var diagnostics = text == null ? session.ruleDiagnosticsAt(goal, -1)
                        : session.ruleDiagnosticsAt(goal, text);
                for (var diagnostic : diagnostics) {
                    var rule = diagnostic.rule();
                    out.println("  " + rule.name() + "  " + diagnostic.disposition()
                        + (diagnostic.cost() == null ? "" : " cost=" + diagnostic.cost())
                        + (rule.missing().isEmpty() ? "" : " needs " + rule.missing())
                        + (rule.sequentWide() ? " (sequent-wide)" : ""));
                }
            }
            case "show" -> out.println(session.node(goal(args.option("goal"))).sequent());
            case "goals" -> {
                for (Goal goal : session.proof().openGoals()) {
                    out.println("  goal " + goal.getNode().getSerialNr());
                }
            }
            case "tree" -> {
                for (var entry : session.tree()) {
                    out.println("  " + entry.serial() + " <- " + entry.parent() + " "
                        + entry.state()
                        + " " + entry.name()
                        + (entry.branchLabel() == null ? "" : " [" + entry.branchLabel() + "]"));
                }
            }
            case "why" -> out.print(
                GoalExplanation
                        .format(GoalExplanation.explain(session.proof(), Integer.MAX_VALUE)));
            case "prune" -> out.println("pruned; " + describe(
                session.prune(Integer.parseInt(args.required("prune N")))));
            default -> throw new IllegalArgumentException(
                "unknown script command '" + command.get(0) + "'\n" + HELP);
        }
        args.requireConsumed(command.get(0));
    }

    private ProofSession.Limits currentLimits() {
        var settings = session.proof().getSettings().getStrategySettings();
        return ProofSession.Limits.defaults().withMaxSteps(settings.getMaxSteps())
                .withTimeoutMillis(settings.getTimeout());
    }

    private int goal(Integer serial) {
        if (serial != null) {
            return serial;
        }
        var goals = session.proof().openGoals();
        if (goals.isEmpty()) {
            throw new IllegalStateException("the proof has no open goal");
        }
        return goals.head().getNode().getSerialNr();
    }

    private String describe(ProofSession.Summary summary) {
        return summary.closed() ? "proof closed"
                : summary.openGoals() + " open goal(s): "
                    + session.proof().openGoals().stream().map(g -> g.getNode().getSerialNr())
                            .toList();
    }

    private String describe(ProofSession.SearchResult result) {
        return result.stopReason() + ", " + result.appliedRules() + " rules applied, "
            + result.closedGoals() + " goal(s) closed; " + describe(session.summary());
    }

    private static final class Arguments {
        private final List<String> tokens;

        Arguments(List<String> tokens) {
            this.tokens = new ArrayList<>(tokens);
        }

        Integer option(String key) {
            String value = take(key);
            return value == null ? null : Integer.parseInt(value);
        }

        String text(String key) {
            return take(key);
        }

        String positional() {
            for (int i = 0; i < tokens.size(); i++) {
                if (!isKeyword(tokens.get(i))) {
                    return tokens.remove(i);
                }
                i++;
            }
            return null;
        }

        String required(String usage) {
            String value = positional();
            if (value == null) {
                throw new IllegalArgumentException("usage: " + usage);
            }
            return value;
        }

        Map<String, String> assignments(String key) {
            Map<String, String> result = new LinkedHashMap<>();
            int at = tokens.indexOf(key);
            if (at < 0) {
                return result;
            }
            tokens.remove(at);
            while (at < tokens.size() && tokens.get(at).contains("=")) {
                String assignment = tokens.remove(at);
                int eq = assignment.indexOf('=');
                result.put(assignment.substring(0, eq), assignment.substring(eq + 1));
            }
            return result;
        }

        void requireConsumed(String command) {
            if (!tokens.isEmpty()) {
                throw new IllegalArgumentException(
                    "unexpected arguments to " + command + ": " + tokens + "\n" + HELP);
            }
        }

        private String take(String key) {
            int at = tokens.indexOf(key);
            if (at < 0) {
                return null;
            }
            if (at + 1 >= tokens.size()) {
                throw new IllegalArgumentException(key + " needs a value");
            }
            tokens.remove(at);
            return tokens.remove(at);
        }

        private static boolean isKeyword(String token) {
            return token.equals("on") || token.equals("goal");
        }
    }
}
