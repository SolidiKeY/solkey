/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.key_project.logic.Choice;
import org.key_project.logic.Namespace;
import org.key_project.solidity.control.KeYEnvironment;
import org.key_project.solidity.program.parser.SolcWrapper;
import org.key_project.solidity.proof.Goal;
import org.key_project.solidity.proof.init.SolidityProblemSpec;
import org.key_project.solidity.proof.init.SolidityProblemSynthesizer;
import org.key_project.solidity.proof.io.LoadErrors;
import org.key_project.solidity.proof.io.OutputStreamProofSaver;
import org.key_project.solidity.proof.io.ProblemLoaderException;
import org.key_project.solidity.proof.io.ProofSaver;
import org.key_project.solidity.runtime.SolidityRuntimeCheck;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
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

    @Option(names = "--solc",
        description = "for a .sol FILE: compile it with solc and run it on an in-process EVM "
            + "instead of proving, reporting compiler diagnostics and any failing assert. "
            + "Honours --contract and --function.")
    boolean solc;

    public static void main(String[] args) {
        System.exit(execute(args));
    }

    public static int execute(String... args) {
        CLI cli = new CLI();
        CommandLine cmd = new CommandLine(cli);
        cmd.parseArgs(args);
        if (cmd.isUsageHelpRequested()) {
            cmd.usage(System.out);
            return 0;
        } else if (cmd.isVersionHelpRequested()) {
            cmd.printVersionHelp(System.out);
            return 0;
        }
        if (cli.verbose) {
            cli.printStats = true;
            if (cli.openGoalChars == null) {
                cli.openGoalChars = 2000;
            }
        }
        boolean success = run(cli);
        System.out.flush();
        System.err.flush();
        return success ? 0 : 1;
    }

    private static boolean run(CLI cli) {
        Path f = cli.file.toPath();
        if (!cli.file.isFile()) {
            System.err.println("No such file: " + cli.file.getAbsolutePath());
            return false;
        }
        String malformed = malformedChoice(cli.choices);
        if (malformed != null) {
            System.err.println("Error: " + malformed);
            return false;
        }
        if (cli.solc) {
            if (!f.getFileName().toString().endsWith(".sol")) {
                System.err.println("--solc applies to .sol files only");
                return false;
            }
            if (!cli.choices.isEmpty()) {
                System.err.println("--option selects taclets, which --solc does not use");
                return false;
            }
            return runSolc(cli, f);
        }
        if (!f.getFileName().toString().endsWith(".sol")) {
            if (cli.function != null || cli.contract != null || !cli.choices.isEmpty()) {
                System.err.println(
                    "--function, --contract and --option apply to .sol files only");
                return false;
            }
            return prove(cli, null).closed();
        }
        if (cli.function != null) {
            return prove(cli, new SolidityProblemSpec(cli.contract, cli.function, cli.choices))
                    .closed();
        }
        final List<String> functions;
        try {
            functions = SolidityProblemSynthesizer.provableFunctions(f, cli.contract);
        } catch (Exception e) {
            // solc reports a rejected source as an unchecked exception, so this cannot narrow to
            // IOException without letting a compile error escape as a stack trace.
            System.err.println("Error while reading " + cli.file + ":");
            System.err.println("  " + LoadErrors.describe(e));
            return false;
        }
        int closed = 0;
        List<String> failures = new ArrayList<>();
        for (String function : functions) {
            Outcome outcome =
                prove(cli, new SolidityProblemSpec(cli.contract, function, cli.choices));
            System.out.flush();
            System.err.flush();
            System.out.println((outcome.closed() ? "PASS " : "FAIL ") + function);
            if (outcome.closed()) {
                closed++;
            } else {
                failures.add(function + " (" + outcome.describe() + ")");
            }
        }
        System.out.println(closed + "/" + functions.size() + " closed");
        if (!failures.isEmpty()) {
            System.out.println("FAILED (" + failures.size() + "): " + String.join(", ", failures));
        }
        return closed == functions.size();
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
            output = new ObjectMapper().readTree(SolcWrapper.diagnose(f));
        } catch (Exception e) {
            System.err.println("Error while compiling " + cli.file + ":");
            System.err.println("  " + LoadErrors.describe(e));
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
            System.err.println("Error while running " + cli.file + ":");
            System.err.println("  " + LoadErrors.describe(e));
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
        if (!failures.isEmpty()) {
            System.out.println(
                "FAILED (" + failures.size() + "): " + String.join(", ", failures));
        }
        return failures.isEmpty();
    }

    /// The result of one proof attempt: whether it closed, how many goals were left open, and how
    /// long the attempt took, so that a whole-file run can recap its failures in one block.
    private record Outcome(boolean closed, int openGoals, long millis) {
        static Outcome closed(long millis) {
            return new Outcome(true, 0, millis);
        }

        static Outcome open(int openGoals, long millis) {
            return new Outcome(false, openGoals, millis);
        }

        static Outcome error() {
            return new Outcome(false, -1, -1);
        }

        String describe() {
            if (openGoals < 0) {
                return "error";
            }
            return openGoals + (openGoals == 1 ? " goal, " : " goals, ") + millis + " ms";
        }
    }

    private static Outcome prove(CLI cli, SolidityProblemSpec spec) {
        try {
            if (cli.verbose)
                System.out.println("Loading...");
            Path f = cli.file.toPath();
            var env = spec == null ? KeYEnvironment.load(f) : KeYEnvironment.load(f, spec);
            if (spec != null) {
                String rejected = unknownChoice(spec.choices(), env.getInitConfig().choiceNS());
                if (rejected != null) {
                    System.err.println("Error: " + rejected);
                    return Outcome.error();
                }
            }
            var loadedProof = env.getLoadedProof();
            if (loadedProof.closed()) {
                if (cli.prove) {
                    System.err.println(
                        "Error: The loaded file already contains a proof.\nUse `--no-prove` to only load the proof.");
                    return Outcome.error();
                }
                if (cli.replay) {
                    if (cli.verbose)
                        System.out.println("Replaying proof...");
                    var replayResult = env.getReplayResult();
                    if (replayResult.hasErrors()) {
                        System.err.println("Error(s) while loading!");
                        if (cli.verbose) {
                            List<Throwable> errors = replayResult.getErrorList();
                            for (int i = 0; i < errors.size(); i++) {
                                Throwable error = errors.get(i);
                                System.err.println("Error " + (i + 1) + ": " + error);
                            }
                        }
                        return Outcome.error();
                    }
                    System.out.println("Loading and proof replay successful");
                    return Outcome.closed(0);
                }
                System.out.println("Loading successful");
                return Outcome.closed(0);
            } else {
                if (cli.prove) {
                    if (!cli.quiet) {
                        System.out.println("Proving...");
                    }
                    var stratSettings = loadedProof.getSettings().getStrategySettings();
                    stratSettings.setTimeout(cli.timeout);
                    stratSettings.setMaxSteps(cli.max);
                    long started = System.currentTimeMillis();
                    env.getProofControl().startAndWaitForAutoMode(loadedProof);
                    long elapsed = System.currentTimeMillis() - started;
                    if (cli.printStats && !cli.quiet) {
                        System.out.println(loadedProof.getStatistics());
                    }
                    if (cli.outputFile != null) {
                        try {
                            ProofSaver.saveToFile(cli.outputFile.getAbsoluteFile(), loadedProof);
                        } catch (IOException e) {
                            System.err.println("Error saving proof to file: " + e.getMessage());
                            return Outcome.error();
                        }
                    }
                    if (!loadedProof.closed()) {
                        int open = loadedProof.openGoals().size();
                        if (!cli.quiet) {
                            System.err.println("Proof not closed. " + open + " goals remaining");
                        }
                        printOpenGoals(cli, loadedProof.openGoals());
                        return Outcome.open(open, elapsed);
                    } else {
                        System.out.println("Loading and proof successful");
                        return Outcome.closed(elapsed);
                    }
                } else {
                    System.out.println("Loading successful");
                    return Outcome.closed(0);
                }
            }
        } catch (ProblemLoaderException e) {
            System.err.println("Error while loading " + cli.file + ":");
            System.err.println("  " + LoadErrors.describe(e));
            if (cli.verbose) {
                e.printStackTrace();
            } else {
                System.err.println("(run with --verbose for the full stack trace)");
            }
            return Outcome.error();
        }
    }

    private static @Nullable String unknownChoice(List<String> choices,
            Namespace<@NonNull Choice> declared) {
        if (choices.isEmpty()) {
            return null;
        }
        Map<String, Set<String>> byCategory = new TreeMap<>();
        for (Choice c : declared.allElements()) {
            byCategory.computeIfAbsent(c.category(), k -> new TreeSet<>())
                    .add(c.name().toString());
        }
        for (String choice : choices) {
            String category = choice.substring(0, choice.indexOf(':'));
            Set<String> known = byCategory.get(category);
            if (known == null) {
                return "no such taclet option category: " + category + "; known categories: "
                    + String.join(", ", byCategory.keySet());
            }
            if (!known.contains(choice)) {
                return "no such choice for " + category + ": "
                    + choice.substring(choice.indexOf(':') + 1) + "; known choices: "
                    + known.stream().map(k -> k.substring(k.indexOf(':') + 1))
                            .collect(Collectors.joining(", "));
            }
        }
        return null;
    }

    private static @Nullable String malformedChoice(List<String> choices) {
        for (String choice : choices) {
            int colon = choice.indexOf(':');
            if (colon < 1 || colon != choice.lastIndexOf(':') || colon == choice.length() - 1) {
                return "--option expects <category>:<choice>, but got: " + choice;
            }
        }
        return null;
    }

    private static boolean hintPrinted = false;

    /// Prints the sequent of each open goal, so that an unclosed proof explains itself in the run
    /// that produced it instead of needing a second, differently instrumented one.
    private static void printOpenGoals(CLI cli, Iterable<Goal> goals) {
        if (cli.openGoalChars == null) {
            if (!hintPrinted) {
                hintPrinted = true;
                System.err.println("(run with --open-goals to print the remaining sequents)");
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
            System.err.println("--- open goal " + (printed + 1) + " ---");
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
