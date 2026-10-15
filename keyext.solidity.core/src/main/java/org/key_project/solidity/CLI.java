/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.key_project.solidity.control.GoalExplanation;
import org.key_project.solidity.control.ProofScript;
import org.key_project.solidity.control.ProofSession;
import org.key_project.solidity.control.StepBudget;
import org.key_project.solidity.control.TacletChoices;
import org.key_project.solidity.program.parser.SolcWrapper;
import org.key_project.solidity.proof.Goal;
import org.key_project.solidity.proof.Proof;
import org.key_project.solidity.proof.init.SolidityProblemSpec;
import org.key_project.solidity.proof.init.SolidityProblemSynthesizer;
import org.key_project.solidity.proof.io.LoadErrors;
import org.key_project.solidity.proof.io.OutputStreamProofSaver;
import org.key_project.solidity.proof.io.ProofSaver;
import org.key_project.solidity.runtime.SolidityRuntimeCheck;

import picocli.CommandLine;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

public class CLI {
    @Option(names = { "-V", "--version" }, versionHelp = true, description = "display version info")
    boolean versionInfoRequested;

    @Option(names = { "-h", "--help" }, usageHelp = true, description = "display this help message")
    boolean usageHelpRequested;

    @Parameters(paramLabel = "FILE", description = "the file to prove")
    File file;

    @Option(names = { "--output", "-o" }, description = "where to write the proof to")
    File outputFile;

    @Option(names = "--prove", negatable = true, defaultValue = "true", fallbackValue = "true",
        description = "whether to attempt to prove the file")
    boolean prove;

    @Option(names = "--replay", negatable = true, defaultValue = "true", fallbackValue = "true",
        description = "whether to replay the loaded proof")
    boolean replay;

    @Option(names = { "--verbose", "-v" },
        description = "whether to print additional information; implies `--print-stats`")
    boolean verbose;

    @Option(names = { "--print-stats", "-s" },
        description = "print proof statistics (nodes, branches, time)")
    boolean printStats;

    @Option(names = { "-t", "--timeout" }, defaultValue = "-1",
        description = "timeout for the prover (ms)")
    long timeout;

    @Option(names = { "-m", "--max" }, defaultValue = "10000",
        description = "maximal number of rule applications")
    int max;

    @Option(names = { "-f", "--function" },
        description = "for a .sol FILE: the function to prove; all of them if omitted")
    String function;

    @Option(names = { "-c", "--contract" },
        description = "for a .sol FILE: the contract to prove against, if it declares several")
    String contract;

    @Option(names = { "-O", "--option" }, paramLabel = "<category:choice>",
        description = "for a .sol FILE: a taclet option to prove under; repeatable. A .key FILE "
            + "declares its own options with \\withOptions instead.")
    List<String> choices = new ArrayList<>();

    @Option(names = "--open-goals", arity = "0..1", fallbackValue = "2000",
        description = "on an unclosed proof, print each open goal's sequent, truncated to this "
            + "many characters (default ${FALLBACK-VALUE}); implied by --verbose")
    Integer openGoalChars;

    @Option(names = "--max-goals", defaultValue = "3",
        description = "how many open goals --open-goals prints at most")
    int maxGoals;

    @Option(names = { "-q", "--quiet" },
        description = "for a .sol FILE: suppress per-function progress and statistics")
    boolean quiet;

    @Option(names = "--print-problem",
        description = "for a .sol FILE with --function: print the generated .key problem "
            + "instead of proving it")
    boolean printProblem;

    @Option(names = "--solc",
        description = "for a .sol FILE: compile it with solc and run it on an in-process EVM "
            + "instead of proving, reporting compiler diagnostics and any failing assert. "
            + "Honours --contract and --function.")
    boolean solc;

    @Option(names = "--why",
        description = "on an unclosed proof, explain each open goal: the stop reason, the last "
            + "rules applied, and the terms no rule reduces (a read on a constructor without a "
            + "matching rule, or a matching rule the strategy refuses)")
    boolean why;

    @Option(names = "--json",
        description = "print one JSON object per proof obligation and a final summary object "
            + "instead of the human-readable report")
    boolean json;

    @Option(names = "--trace", description = "print the full stack trace of an error")
    boolean trace;

    @Option(names = "--baseline", paramLabel = "FILE",
        description = "compare each obligation's status with FILE and report only the changes; "
            + "the run fails only on a regression (a closed proof that no longer closes). "
            + "FILE is written when missing")
    File baseline;

    @Option(names = "--baseline-update",
        description = "with --baseline, rewrite FILE with this run's statuses")
    boolean baselineUpdate;

    @Option(names = "--script", paramLabel = "SCRIPT",
        description = "for a single obligation: load it without proof search and run SCRIPT, "
            + "commands separated by ';' — `--script help` lists them")
    String script;

    @Option(names = "--script-file", paramLabel = "FILE",
        description = "like --script, reading the script from FILE")
    File scriptFile;

    @Option(names = "--budget", arity = "0..1", fallbackValue = "", paramLabel = "FILE",
        description = "a deterministic step budget per function instead of -m and -t: "
            + StepBudget.STEPS_PER_SOURCE_BYTE + " steps per byte of the function's source, "
            + "clamped to " + StepBudget.MIN_ESTIMATE + ".." + StepBudget.MAX_ESTIMATE
            + "; with FILE (a --baseline file, keep it in /tmp) a recorded count wins: it times "
            + "1.5 plus " + StepBudget.FLOOR + ", or exactly the count when that run was cut at "
            + "a limit")
    String budget;

    private boolean hintPrinted;
    private StepBudget budgets;

    private static final ObjectMapper JSON = new ObjectMapper();

    public static void main(String[] args) {
        System.exit(execute(args));
    }

    public static int execute(String... args) {
        CLI cli = new CLI();
        CommandLine cmd = new CommandLine(cli);
        try {
            cmd.parseArgs(args);
        } catch (CommandLine.ParameterException e) {
            System.err.println("Error: " + e.getMessage() + " (see --help)");
            return 2;
        }
        if (cmd.isUsageHelpRequested()) {
            cmd.usage(System.out);
            return 0;
        } else if (cmd.isVersionHelpRequested()) {
            cmd.printVersionHelp(System.out);
            return 0;
        }
        if (cli.verbose) {
            cli.printStats = true;
            cli.trace = true;
            if (cli.openGoalChars == null) {
                cli.openGoalChars = 2000;
            }
        }
        if (cli.json) {
            cli.quiet = true;
        }
        boolean success;
        try {
            success = run(cli);
        } catch (Exception | StackOverflowError e) {
            cli.reportError(null, e);
            success = false;
        }
        System.out.flush();
        System.err.flush();
        return success ? 0 : 1;
    }

    private static boolean run(CLI cli) throws IOException {
        if ("help".equals(cli.script)) {
            System.out.println(ProofScript.HELP);
            return true;
        }
        Path f = cli.file.toPath();
        if (!cli.file.isFile()) {
            System.err.println("No such file: " + cli.file.getAbsolutePath());
            return false;
        }
        String malformed = TacletChoices.malformed(cli.choices);
        if (malformed != null) {
            System.err.println("Error: " + malformed);
            return false;
        }
        boolean sol = f.getFileName().toString().endsWith(".sol");
        if (cli.solc) {
            if (!sol) {
                System.err.println("--solc applies to .sol files only");
                return false;
            }
            if (!cli.choices.isEmpty()) {
                System.err.println("--option selects taclets, which --solc does not use");
                return false;
            }
            return runSolc(cli, f);
        }
        if (!sol && (cli.function != null || cli.contract != null || !cli.choices.isEmpty())) {
            System.err.println("--function, --contract and --option apply to .sol files only");
            return false;
        }
        if (cli.printProblem) {
            if (!sol || cli.function == null) {
                System.err.println("--print-problem needs a .sol FILE and --function");
                return false;
            }
            try {
                SolidityProblemSpec spec =
                    SolidityProblemSynthesizer.resolve(f, cli.spec(cli.function));
                System.out.print(SolidityProblemSynthesizer.problemText(f, spec));
                return true;
            } catch (Exception e) {
                cli.reportError("reading", e);
                return false;
            }
        }
        if (cli.script != null || cli.scriptFile != null) {
            if (sol && cli.function == null) {
                System.err.println("--script needs --function for a .sol FILE");
                return false;
            }
            String text = cli.scriptFile != null
                    ? Files.readString(cli.scriptFile.toPath(), StandardCharsets.UTF_8)
                    : cli.script;
            return runScript(cli, sol ? cli.spec(cli.function) : null, text);
        }
        List<Outcome> outcomes = new ArrayList<>();
        if (!sol) {
            outcomes.add(prove(cli, null, f.getFileName().toString()));
        } else if (cli.function != null) {
            outcomes.add(prove(cli, cli.spec(cli.function), cli.function));
        } else {
            final List<String> functions;
            try {
                functions = SolidityProblemSynthesizer.provableFunctions(f, cli.contract);
            } catch (Exception e) {
                cli.reportError("reading", e);
                return false;
            }
            for (String function : functions) {
                Outcome outcome = prove(cli, cli.spec(function), function);
                System.out.flush();
                System.err.flush();
                if (!cli.json) {
                    System.out.println((outcome.closed() ? "PASS " : "FAIL ") + function);
                }
                outcomes.add(outcome);
            }
        }
        boolean allClosed = outcomes.stream().allMatch(Outcome::closed);
        if (cli.json) {
            Map<String, Object> summary = new LinkedHashMap<>();
            summary.put("summary", true);
            summary.put("closed", outcomes.stream().filter(Outcome::closed).count());
            summary.put("total", outcomes.size());
            System.out.println(JSON.writeValueAsString(summary));
        } else if (sol && cli.function == null) {
            System.out.println(outcomes.stream().filter(Outcome::closed).count() + "/"
                + outcomes.size() + " closed");
            if (cli.baseline == null) {
                printFailures(outcomes.stream().filter(o -> !o.closed())
                        .map(o -> o.name() + " (" + o.describe() + ")").toList());
            }
        }
        if (cli.baseline != null) {
            return compareWithBaseline(cli, outcomes);
        }
        return allClosed;
    }

    private static boolean compareWithBaseline(CLI cli, List<Outcome> outcomes)
            throws IOException {
        Path file = cli.baseline.toPath();
        String prefix = cli.contractName() + ".";
        if (!Files.exists(file) || cli.baselineUpdate) {
            StepBudget.locked(file, () -> {
                StepBudget previous = StepBudget.loadIfExists(file);
                StepBudget updated = StepBudget.empty();
                previous.entries().forEach((key, entry) -> {
                    if (cli.function != null || !key.startsWith(prefix)) {
                        updated.put(key, entry);
                    }
                });
                for (Outcome outcome : outcomes) {
                    updated.put(prefix + outcome.name(), outcome.entry());
                }
                updated.write(file);
                return null;
            });
            System.out.println("baseline: recorded " + outcomes.size() + " statuses and step "
                + "counts in " + file);
            return true;
        }
        StepBudget previous = StepBudget.load(file);
        int regressions = 0;
        int changes = 0;
        for (Outcome outcome : outcomes) {
            StepBudget.Entry before = previous.entry(cli.contractName(), outcome.name());
            String was = before == null ? "NEW" : before.status();
            if (!was.equals(outcome.status())) {
                changes++;
                boolean regression = "PROVED".equals(was);
                regressions += regression ? 1 : 0;
                System.out.println((regression ? "REGRESSED " : "CHANGED   ") + outcome.name()
                    + ": " + was + " -> " + outcome.status()
                    + (outcome.closed() ? "" : " (" + outcome.stopReason() + ")"));
            }
        }
        if (cli.function == null) {
            for (var entry : previous.entries().entrySet()) {
                String key = entry.getKey();
                String name = key.startsWith(prefix) ? key.substring(prefix.length()) : key;
                if (key.contains(".") && !key.startsWith(prefix)) {
                    continue;
                }
                if (outcomes.stream().noneMatch(o -> o.name().equals(name))) {
                    changes++;
                    System.out.println("GONE      " + name + ": " + entry.getValue().status());
                }
            }
        }
        System.out.println("baseline: " + changes + " changed, " + regressions + " regressed"
            + (changes > 0 ? " (--baseline-update records this run)" : ""));
        return regressions == 0;
    }

    private static boolean runScript(CLI cli, SolidityProblemSpec spec, String text) {
        Path f = cli.file.toPath();
        ProofSession.Limits limits = cli.limits();
        try (ProofSession session = spec == null ? ProofSession.load(f, limits, false)
                : ProofSession.open(f, spec, limits)) {
            try {
                new ProofScript(session, System.out).run(text);
            } finally {
                var summary = session.summary();
                System.out.println("result: " + (summary.closed() ? "proof closed"
                        : summary.openGoals() + " open goal(s)"));
                if (cli.outputFile != null) {
                    ProofSaver.saveToFile(cli.outputFile.getAbsoluteFile(), session.proof());
                }
            }
            return true;
        } catch (Exception e) {
            cli.reportError("running the script on", e);
            return false;
        }
    }

    /// Compiles `f`, prints what solc says about it, and then runs it.
    ///
    /// Compiling alone answers "is this Solidity?", which is rarely the question: a proof that
    /// will not close usually has a contract that compiles perfectly well. So the file is also
    /// deployed on an in-process EVM and its functions are called, which answers the question
    /// actually being asked — does the `assert` hold when the code runs?
    private static boolean runSolc(CLI cli, Path f) {
        final JsonNode output;
        try {
            output = SolcWrapper.readJson(SolcWrapper.diagnose(f));
        } catch (Exception e) {
            cli.reportError("compiling", e);
            return false;
        }
        int errors = 0;
        int warnings = 0;
        for (JsonNode diagnostic : output.path("errors").values()) {
            String severity = diagnostic.path("severity").asString("");
            String message = diagnostic.path("formattedMessage")
                    .asString(diagnostic.path("message").asString(""));
            if ("error".equals(severity)) {
                errors++;
                System.err.println(message);
            } else {
                warnings++;
                if (!cli.quiet) {
                    System.out.println(message);
                }
            }
        }
        System.out.println("solc " + SolcWrapper.version() + ": " + errors
            + (errors == 1 ? " error, " : " errors, ") + warnings
            + (warnings == 1 ? " warning" : " warnings"));
        if (errors > 0) {
            // Nothing to run: solc produced no bytecode.
            return false;
        }
        return runOnEvm(cli, f);
    }

    /// Runs the selected functions on an EVM and prints one line each, `N/M ok` last.
    private static boolean runOnEvm(CLI cli, Path f) {
        final List<SolidityRuntimeCheck.Verdict> verdicts;
        try {
            verdicts = SolidityRuntimeCheck.run(f, cli.contract, cli.function);
        } catch (Exception e) {
            cli.reportError("running", e);
            return false;
        }
        if (verdicts.isEmpty()) {
            System.out.println("no function to run");
            return true;
        }
        int skipped = 0;
        List<String> failures = new ArrayList<>();
        for (SolidityRuntimeCheck.Verdict verdict : verdicts) {
            boolean good = verdict.outcome() == SolidityRuntimeCheck.Outcome.OK;
            final String label;
            if (good) {
                label = "OK   ";
            } else if (verdict.outcome() == SolidityRuntimeCheck.Outcome.SKIPPED) {
                skipped++;
                label = "SKIP ";
            } else {
                failures.add(verdict.function() + " (" + verdict.describe() + ")");
                label = "FAIL ";
            }
            if (!cli.quiet || !good) {
                System.out.println(label + verdict.function()
                        + (good ? "" : " — " + verdict.describe()));
            }
        }
        int ran = verdicts.size() - skipped;
        System.out.println((ran - failures.size()) + "/" + ran + " ran without a failing assert"
            + (skipped > 0 ? " (" + skipped + " not run)" : ""));
        printFailures(failures);
        return failures.isEmpty();
    }

    private static void printFailures(List<String> failures) {
        if (!failures.isEmpty()) {
            System.out.println("FAILED (" + failures.size() + "): " + String.join(", ", failures));
        }
    }

    private SolidityProblemSpec spec(String function) {
        return new SolidityProblemSpec(contract, function, choices);
    }

    private ProofSession.Limits limits() {
        return ProofSession.Limits.steps(max).withTimeoutMillis(timeout);
    }

    private ProofSession.Limits limits(String function) throws IOException {
        if (budget == null) {
            return limits();
        }
        if (budgets == null) {
            budgets = budget.isEmpty() ? StepBudget.empty()
                    : StepBudget.loadIfExists(Path.of(budget));
        }
        Integer recorded = budgets.recordedBudget(contractName(), function);
        int steps =
            recorded != null ? recorded : StepBudget.estimate(file.toPath(), contract, function);
        return ProofSession.Limits.steps(steps).withTimeoutMillis(timeout);
    }

    private String contractName() {
        if (contract != null) {
            return contract;
        }
        String name = file.getName();
        int dot = name.lastIndexOf('.');
        return dot < 0 ? name : name.substring(0, dot);
    }

    private String reportError(String activity, Throwable e) {
        String line = LoadErrors.compact(e, file == null ? null : file.toPath());
        if (json) {
            return line;
        }
        System.err
                .println("Error: " + line + (activity == null ? "" : " (while " + activity + ")"));
        if (trace) {
            e.printStackTrace();
        } else if (!hintPrinted) {
            hintPrinted = true;
            System.err.println("(run with --trace for the stack trace)");
        }
        return line;
    }

    private record Outcome(String name, boolean closed, int openGoals, long millis,
            ProofSession.StopReason stopReason, int steps, String error,
            List<GoalExplanation.Explanation> why, int budget) {
        static Outcome closed(String name, long millis, int steps) {
            return new Outcome(name, true, 0, millis, ProofSession.StopReason.PROVED, steps, null,
                List.of(), -1);
        }

        static Outcome error(String name, String error) {
            return new Outcome(name, false, -1, -1, ProofSession.StopReason.ERROR, 0, error,
                List.of(), -1);
        }

        Outcome withBudget(int budget) {
            return new Outcome(name, closed, openGoals, millis, stopReason, steps, error, why,
                budget);
        }

        StepBudget.Entry entry() {
            return new StepBudget.Entry(status(), steps, stopReason.name());
        }

        String status() {
            return closed ? "PROVED" : error != null ? "ERROR" : "OPEN";
        }

        String describe() {
            if (error != null) {
                return "error: " + error;
            }
            return openGoals + (openGoals == 1 ? " goal, " : " goals, ") + stopReason + ", "
                + millis + " ms";
        }

        Map<String, Object> toJson() {
            Map<String, Object> json = new LinkedHashMap<>();
            json.put("function", name);
            json.put("status", status());
            json.put("stopReason", stopReason == null ? null : stopReason.name());
            json.put("steps", steps);
            if (budget >= 0) {
                json.put("budget", budget);
            }
            json.put("millis", millis);
            json.put("openGoals", openGoals);
            if (error != null) {
                json.put("error", error);
            }
            if (!why.isEmpty()) {
                json.put("why", why);
            }
            return json;
        }
    }

    private static Outcome prove(CLI cli, SolidityProblemSpec spec, String name) {
        Outcome outcome = attempt(cli, spec, name);
        if (cli.budget != null && spec != null) {
            try {
                outcome = outcome.withBudget(cli.limits(name).maxSteps());
            } catch (IOException e) {
                cli.reportError("reading the budget", e);
            }
        }
        if (cli.json) {
            System.out.println(JSON.writeValueAsString(outcome.toJson()));
        }
        return outcome;
    }

    private static Outcome attempt(CLI cli, SolidityProblemSpec spec, String name) {
        Path f = cli.file.toPath();
        if (cli.verbose) {
            System.out.println("Loading...");
        }
        try (ProofSession session = spec == null ? ProofSession.load(f, cli.limits(), false)
                : ProofSession.open(f, spec, cli.limits(name))) {
            if (session.proof().closed()) {
                if (cli.prove) {
                    System.err.println("Error: The loaded file already contains a proof.\n"
                        + "Use `--no-prove` to only load the proof.");
                    return Outcome.error(name, "the loaded file already contains a proof");
                }
                if (cli.replay) {
                    return replay(cli, session, name);
                }
            } else if (cli.prove) {
                return runAutoMode(cli, session, name);
            }
            if (!cli.json) {
                System.out.println("Loading successful");
            }
            return Outcome.closed(name, 0, 0);
        } catch (Exception e) {
            return Outcome.error(name, cli.reportError("loading", e));
        }
    }

    private static Outcome replay(CLI cli, ProofSession session, String name) {
        if (cli.verbose) {
            System.out.println("Replaying proof...");
        }
        List<String> errors = session.replayErrors();
        if (!errors.isEmpty()) {
            if (!cli.json) {
                System.err.println("Error(s) while loading!");
                if (cli.verbose) {
                    for (int i = 0; i < errors.size(); i++) {
                        System.err.println("Error " + (i + 1) + ": " + errors.get(i));
                    }
                }
            }
            return Outcome.error(name, errors.get(0));
        }
        if (!cli.json) {
            System.out.println("Loading and proof replay successful");
        }
        return Outcome.closed(name, 0, 0);
    }

    private static Outcome runAutoMode(CLI cli, ProofSession session, String name)
            throws IOException {
        if (!cli.quiet) {
            System.out.println("Proving...");
        }
        Proof proof = session.proof();
        ProofSession.SearchResult search = session.runAutoDetailed(null);
        if (cli.printStats && !cli.quiet) {
            System.out.println(proof.getStatistics());
        }
        if (cli.outputFile != null) {
            ProofSaver.saveToFile(cli.outputFile.getAbsoluteFile(), proof);
        }
        if (search.exception() != null) {
            return Outcome.error(name, cli.reportError("proving", search.exception()));
        }
        if (proof.closed()) {
            if (!cli.json) {
                System.out.println("Loading and proof successful");
            }
            return Outcome.closed(name, search.millis(), search.appliedRules());
        }
        int open = proof.openGoals().size();
        List<GoalExplanation.Explanation> why =
            cli.why ? GoalExplanation.explain(proof, cli.maxGoals) : List.of();
        if (!cli.json) {
            if (!cli.quiet || cli.why || cli.openGoalChars != null) {
                System.err.println("Proof not closed: " + search.stopReason() + " after "
                    + search.appliedRules() + " rules, " + open + " goals remaining");
            }
            printOpenGoals(cli, proof.openGoals());
            if (cli.why) {
                System.err.print(GoalExplanation.format(why));
            }
        }
        return new Outcome(name, false, open, search.millis(), search.stopReason(),
            search.appliedRules(), null, why, -1);
    }

    private static void printOpenGoals(CLI cli, Iterable<Goal> goals) {
        if (cli.openGoalChars == null) {
            if (!cli.hintPrinted && !cli.why) {
                cli.hintPrinted = true;
                System.err.println(
                    "(run with --why for the reason, --open-goals for the remaining sequents)");
            }
            return;
        }
        int printed = 0;
        for (Goal goal : goals) {
            if (printed >= cli.maxGoals) {
                System.err.println("... (raise --max-goals to see the rest)");
                break;
            }
            String sequent =
                OutputStreamProofSaver.printSequent(goal.sequent(), goal.getOverlayServices());
            System.err.println("--- open goal " + goal.getNode().getSerialNr() + " ---");
            System.err.println(truncate(sequent, cli.openGoalChars));
            printed++;
        }
    }

    private static String truncate(String text, int limit) {
        if (limit <= 0 || text.length() <= limit) {
            return text;
        }
        return text.substring(0, limit) + "... (" + (text.length() - limit)
            + " more characters, raise --open-goals to see them)";
    }
}
