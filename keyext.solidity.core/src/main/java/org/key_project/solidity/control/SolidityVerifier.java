/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.control;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.key_project.logic.Choice;
import org.key_project.solidity.proof.init.SolidityProblemSpec;
import org.key_project.solidity.proof.init.SolidityProblemSynthesizer;
import org.key_project.solidity.proof.io.LoadErrors;
import org.key_project.solidity.proof.io.ProblemLoaderException;
import org.key_project.solidity.settings.ProofSettings;

import org.jspecify.annotations.Nullable;

public final class SolidityVerifier {

    public enum Status {
        PROVED, OPEN, ERROR
    }

    public record VerificationOptions(ProofSession.Limits limits, int maxGoals,
            boolean withProof) {
        public VerificationOptions {
            java.util.Objects.requireNonNull(limits);
            if (maxGoals < 0) {
                throw new IllegalArgumentException("maxGoals must be nonnegative");
            }
        }

        public static VerificationOptions defaults() {
            return new VerificationOptions(ProofSession.Limits.defaults(), 3, false);
        }
    }

    public record Outcome(String function, boolean closed, List<String> openGoals, long millis,
            @Nullable String error, @Nullable String proof,
            ProofSession.@Nullable Summary summary) {
        public Status status() {
            return error != null ? Status.ERROR : closed ? Status.PROVED : Status.OPEN;
        }
    }

    public record DetailedOutcome(Outcome outcome, ProofSession.@Nullable SearchResult search,
            @Nullable Throwable exception) {
        public Status status() {
            return outcome.status();
        }
    }

    public record ChoiceCategory(String category, Set<String> choices, @Nullable String selected) {
    }

    private SolidityVerifier() {
    }

    public static List<String> functions(Path solFile, @Nullable String contract)
            throws IOException {
        return SolidityProblemSynthesizer.provableFunctions(solFile, contract);
    }

    public static String problem(Path solFile, SolidityProblemSpec spec) throws IOException {
        return SolidityProblemSynthesizer.problemText(solFile,
            SolidityProblemSynthesizer.resolve(solFile, spec));
    }

    public static Outcome verify(Path solFile, SolidityProblemSpec spec, int maxSteps,
            long timeout, int maxGoals) {
        return verify(solFile, spec, maxSteps, timeout, maxGoals, false);
    }

    public static Outcome verify(Path solFile, SolidityProblemSpec spec, int maxSteps,
            long timeout, int maxGoals, boolean withProof) {
        return verify(solFile, spec, new ProofSession.Limits(maxSteps, timeout, Map.of()),
            maxGoals, withProof);
    }

    public static Outcome verify(Path solFile, SolidityProblemSpec spec,
            ProofSession.Limits limits, int maxGoals, boolean withProof) {
        return verify(solFile, spec, new VerificationOptions(limits, maxGoals, withProof));
    }

    public static Outcome verify(Path solFile, SolidityProblemSpec spec,
            VerificationOptions options) {
        return verifyDetailed(solFile, spec, options).outcome();
    }

    public static DetailedOutcome verifyDetailed(Path solFile, SolidityProblemSpec spec,
            VerificationOptions options) {
        long started = System.currentTimeMillis();
        try (ProofSession session = ProofSession.open(solFile, spec, options.limits())) {
            ProofSession.SearchResult search = session.runAutoDetailed(null);
            Outcome outcome = outcome(session, String.valueOf(spec.function()), options.maxGoals(),
                options.withProof(), System.currentTimeMillis() - started);
            return new DetailedOutcome(outcome, search, search.exception());
        } catch (Exception e) {
            return new DetailedOutcome(failed(String.valueOf(spec.function()), e,
                System.currentTimeMillis() - started), null, e);
        }
    }

    public static Outcome verifyOrThrow(Path solFile, SolidityProblemSpec spec,
            VerificationOptions options) throws ProblemLoaderException, IOException {
        long started = System.currentTimeMillis();
        try (ProofSession session = ProofSession.open(solFile, spec, options.limits())) {
            session.runAuto(null);
            return outcome(session, String.valueOf(spec.function()), options.maxGoals(),
                options.withProof(), System.currentTimeMillis() - started);
        }
    }

    public static Outcome outcome(ProofSession session, String function, int maxGoals,
            boolean withProof, long millis) throws IOException {
        ProofSession.Summary summary = session.summary();
        ProofSession.SearchResult search = session.lastSearch();
        Throwable exception = search == null ? null : search.exception();
        return new Outcome(function, summary.closed(), session.openGoalTexts(maxGoals), millis,
            exception == null ? null : LoadErrors.describe(exception),
            withProof ? session.save() : null, summary);
    }

    public static Outcome failed(String function, Exception e, long millis) {
        return new Outcome(function, false, List.of(), millis, LoadErrors.describe(e), null,
            null);
    }

    public static List<ChoiceCategory> choices(ProofSession session) {
        var initConfig = session.proof().getInitConfig();
        Map<String, String> selected = new TreeMap<>();
        for (Choice choice : initConfig.getActivatedChoices()) {
            selected.put(choice.category(), choice.name().toString());
        }
        Map<String, String> defaults =
            ProofSettings.DEFAULT_SETTINGS.getChoiceSettings().getDefaultChoices();
        return TacletChoices.byCategory(initConfig.choiceNS()).entrySet().stream()
                .map(e -> new ChoiceCategory(e.getKey(), e.getValue(),
                    selected.getOrDefault(e.getKey(), defaults.get(e.getKey()))))
                .toList();
    }
}
