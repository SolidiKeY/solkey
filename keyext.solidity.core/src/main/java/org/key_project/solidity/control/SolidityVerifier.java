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
import org.key_project.solidity.settings.ProofSettings;

import org.jspecify.annotations.Nullable;

public final class SolidityVerifier {

    public record Outcome(String function, boolean closed, List<String> openGoals, long millis,
            @Nullable String error, @Nullable String proof,
            ProofSession.@Nullable Summary summary) {
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
        long started = System.currentTimeMillis();
        try {
            ProofSession session = ProofSession.start(solFile, spec, limits);
            return outcome(session, String.valueOf(spec.function()), maxGoals, withProof,
                System.currentTimeMillis() - started);
        } catch (Exception e) {
            return failed(String.valueOf(spec.function()), e, System.currentTimeMillis() - started);
        }
    }

    public static Outcome outcome(ProofSession session, String function, int maxGoals,
            boolean withProof, long millis) throws IOException {
        ProofSession.Summary summary = session.summary();
        return new Outcome(function, summary.closed(), session.openGoalTexts(maxGoals), millis,
            null, withProof ? session.save() : null, summary);
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
