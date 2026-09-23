/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.taclets;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.key_project.logic.PosInTerm;
import org.key_project.logic.Term;
import org.key_project.prover.rules.Taclet;
import org.key_project.prover.sequent.PosInOccurrence;
import org.key_project.prover.sequent.SequentFormula;
import org.key_project.solidity.control.KeYEnvironment;
import org.key_project.solidity.proof.Goal;
import org.key_project.solidity.proof.Proof;
import org.key_project.solidity.proof.io.AbstractProblemLoader.ReplayResult;
import org.key_project.solidity.proof.io.ProofSaver;
import org.key_project.solidity.rule.TacletApp;
import org.key_project.solidity.testutil.SolidityExampleTests;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Every taclet of the Solidity rule files must be applied by some example: a function of
/// `keyext.solidity.examples/TestSuite.sol` proved in automode, or a saved proof in
/// `keyext.solidity.examples/proofs/` for the taclets no TestSuite proof reaches.
///
/// Each `proofs/NAME.key` is a problem whose proof applies the taclet `NAME`: a logic lemma for
/// the taclets the Solidity calculus never produces a term for, or a TestSuite obligation where
/// automode prefers a competing rule. Its `NAME.proof` beside it is replayed here. Regenerate
/// the proofs with `-Dorg.key_project.solidity.taclets.TacletCoverageTest.update=true`: each
/// problem is proved in automode one step at a time, applying `NAME` as soon as it is
/// applicable.
///
/// Each function is proved under the default taclet options; the functions that apply an
/// option-guarded rule are proved again under the non-default options, so the other variants are
/// reached too. A rule counts as covered whether or not the proof closes; closing is what
/// [TacletStarterExamplesTest] and [PaperTestExamplesTest] check.
@Tag("solidityExamples")
public class TacletCoverageTest {

    private static final String RULES_DIR = "org/key_project/solidity/proof/rules/";

    private static final List<String> RULE_FILES = List.of("solidityProgramRules.key",
        "memoryRules.key", "structRules.key", "structMemoryRules.key", "listRules.key",
        "updateRules.key", "cast.key", "precRules.key");

    private static final Pattern TACLET_HEADER =
        Pattern.compile("^    ([A-Za-z_][A-Za-z0-9_]*) \\{$", Pattern.MULTILINE);

    private static final List<String> ALTERNATIVE_CHOICES =
        List.of("indexWriteCapture:allAtOnce", "transferSemantics:withCallback");

    private static final Pattern BLOCK_COMMENT = Pattern.compile("/\\*.*?\\*/", Pattern.DOTALL);

    private static final int MAX_STEPS = 10000;

    private static final String PROOFS_DIR = "proofs";

    private static final String UPDATE_PROPERTY =
        "org.key_project.solidity.taclets.TacletCoverageTest.update";

    private static final Set<String> applied = new HashSet<>();

    private static final Set<String> loadedTaclets = new HashSet<>();

    @BeforeAll
    static void proveEveryExample() throws Exception {
        if (Boolean.getBoolean(UPDATE_PROPERTY)) {
            for (Path problem : savedProofProblems()) {
                saveProof(problem);
            }
        }
        List<String> functions = SolidityExampleTests
                .testSuiteFunctions(name -> !name.startsWith("unprovable"))
                .map(arguments -> (String) arguments.get()[0]).toList();
        Set<String> optionGuarded = new HashSet<>();
        List<String> optionDependent = new ArrayList<>();
        for (String function : functions) {
            Proof proof = SolidityExampleTests.proveTestSuiteFunction(function, MAX_STEPS,
                SolidityExampleTests.KEEP_TIMEOUT);
            if (loadedTaclets.isEmpty()) {
                for (Taclet taclet : proof.getInitConfig().getTaclets()) {
                    loadedTaclets.add(taclet.name().toString());
                    if (isGuardedByAlternative(taclet)) {
                        optionGuarded.add(taclet.name().toString());
                    }
                }
            }
            Set<String> rules = SolidityExampleTests.appliedRuleNames(proof);
            applied.addAll(rules);
            if (rules.stream().anyMatch(optionGuarded::contains)) {
                optionDependent.add(function);
            }
        }
        for (String function : optionDependent) {
            applied.addAll(SolidityExampleTests.appliedRuleNames(
                SolidityExampleTests.proveTestSuiteFunction(function, MAX_STEPS,
                    SolidityExampleTests.KEEP_TIMEOUT, ALTERNATIVE_CHOICES)));
        }
        for (Path problem : savedProofProblems()) {
            Path file = proofOf(problem);
            Set<String> rules = SolidityExampleTests.appliedRuleNames(replay(file));
            assertTrue(rules.contains(tacletOf(problem)),
                file + " must apply " + tacletOf(problem) + "; regenerate it with -D"
                    + UPDATE_PROPERTY + "=true");
            applied.addAll(rules);
        }
    }

    private static boolean isGuardedByAlternative(Taclet taclet) {
        String guard = taclet.getChoices().toString();
        return ALTERNATIVE_CHOICES.stream()
                .anyMatch(choice -> guard.contains(choice.substring(0, choice.indexOf(':'))));
    }

    @Test
    void everySolidityTacletIsApplied() throws IOException {
        List<String> uncovered = solidityTaclets().entrySet().stream()
                .filter(entry -> !applied.contains(entry.getKey()))
                .map(entry -> entry.getKey() + " (" + entry.getValue() + ")")
                .sorted()
                .toList();
        assertEquals(List.of(), uncovered,
            "Solidity taclets no TestSuite.sol function or saved proof applies");
    }

    @Test
    void everyScannedNameIsALoadedTaclet() throws IOException {
        List<String> unknown = solidityTaclets().keySet().stream()
                .filter(name -> !loadedTaclets.contains(name))
                .sorted()
                .toList();
        assertEquals(List.of(), unknown,
            "names read from the rule files that are not taclets: the header pattern drifted");
    }

    static Map<String, String> solidityTaclets() throws IOException {
        Map<String, String> taclets = new LinkedHashMap<>();
        for (String file : RULE_FILES) {
            String text = BLOCK_COMMENT.matcher(readResource(RULES_DIR + file))
                    .replaceAll(comment -> comment.group().replaceAll("[^\\n]", " "));
            Matcher matcher = TACLET_HEADER.matcher(text);
            while (matcher.find()) {
                int line = 1 + (int) text.substring(0, matcher.start()).chars()
                        .filter(c -> c == '\n').count();
                taclets.putIfAbsent(matcher.group(1), file + ":" + line);
            }
        }
        assertFalse(taclets.isEmpty(), "no taclets found in " + RULE_FILES);
        return taclets;
    }

    private static String readResource(String resource) throws IOException {
        try (InputStream in =
            TacletCoverageTest.class.getClassLoader().getResourceAsStream(resource)) {
            assertNotNull(in, "missing rule resource " + resource);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    static List<Path> savedProofProblems() throws IOException {
        Path dir = SolidityExampleTests.examplesDir(PROOFS_DIR);
        if (!Files.isDirectory(dir)) {
            return List.of();
        }
        try (Stream<Path> files = Files.list(dir)) {
            return files.filter(f -> f.toString().endsWith(".key")).sorted().toList();
        }
    }

    private static String tacletOf(Path problem) {
        String name = problem.getFileName().toString();
        return name.substring(0, name.length() - ".key".length());
    }

    private static Path proofOf(Path problem) {
        return problem.resolveSibling(tacletOf(problem) + ".proof");
    }

    private static void saveProof(Path problem) throws Exception {
        String taclet = tacletOf(problem);
        KeYEnvironment env = KeYEnvironment.load(problem);
        Proof proof = env.getLoadedProof();
        var strategySettings = proof.getSettings().getStrategySettings();
        strategySettings.setMaxSteps(1);
        boolean appliedTaclet = false;
        for (int step = 0; step < MAX_STEPS && !appliedTaclet && !proof.closed(); step++) {
            appliedTaclet = applyWhereApplicable(env, proof, taclet);
            if (!appliedTaclet) {
                env.getProofControl().startAndWaitForAutoMode(proof);
            }
        }
        strategySettings.setMaxSteps(MAX_STEPS);
        env.getProofControl().startAndWaitForAutoMode(proof);
        assertTrue(appliedTaclet, taclet + " never became applicable in " + problem);
        assertTrue(proof.closed(), () -> SolidityExampleTests.describeOpenGoals(taclet, proof));
        ProofSaver.saveToFile(proofOf(problem).toFile(), proof);
    }

    private static boolean applyWhereApplicable(KeYEnvironment env, Proof proof,
            String taclet) {
        for (Goal goal : proof.openGoals()) {
            for (boolean antecedent : new boolean[] { true, false }) {
                var formulas = antecedent ? goal.sequent().antecedent()
                        : goal.sequent().succedent();
                for (SequentFormula formula : formulas) {
                    TacletApp app = findApp(env, proof, goal, formula, antecedent,
                        PosInTerm.getTopLevel(), formula.formula(), taclet);
                    if (app != null) {
                        goal.apply(app);
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static @Nullable TacletApp findApp(KeYEnvironment env, Proof proof, Goal goal,
            SequentFormula formula, boolean antecedent, PosInTerm at, Term term,
            String taclet) {
        PosInOccurrence pos = new PosInOccurrence(formula, at, antecedent);
        for (TacletApp app : env.getProofControl().getFindTaclet(goal, pos)) {
            if (app.taclet().name().toString().equals(taclet)) {
                TacletApp positioned = app.setPosInOccurrence(pos, proof.getServices());
                if (!positioned.complete()) {
                    positioned = positioned.tryToInstantiate(proof.getServices());
                }
                if (positioned != null && positioned.complete()) {
                    return positioned;
                }
            }
        }
        for (int i = 0; i < term.arity(); i++) {
            TacletApp app = findApp(env, proof, goal, formula, antecedent, at.down(i),
                term.sub(i), taclet);
            if (app != null) {
                return app;
            }
        }
        return null;
    }

    static Proof replay(Path file) throws Exception {
        KeYEnvironment env = KeYEnvironment.load(file);
        Proof proof = env.getLoadedProof();
        assertNotNull(proof, file + " must load a proof");
        ReplayResult replay = env.getReplayResult();
        if (replay != null) {
            assertFalse(replay.hasErrors(),
                file + " must replay without errors, got: " + replay.getErrorList());
        }
        assertTrue(proof.closed(), file + " must replay to a closed proof");
        return proof;
    }
}
