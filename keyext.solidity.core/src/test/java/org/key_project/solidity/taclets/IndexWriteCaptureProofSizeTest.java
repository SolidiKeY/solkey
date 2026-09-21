/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.taclets;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.key_project.prover.rules.RuleApp;
import org.key_project.solidity.proof.Node;
import org.key_project.solidity.proof.Proof;
import org.key_project.solidity.testutil.SolidityExampleTests;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/// Pins the proof size of `TestSuite.sol` under both `indexWriteCapture` options, the measurement
/// behind the "Capture partition" table of `docs/taclets-implementation.md`. Every provable
/// function is proved under `receiverThenIndex` and under `allAtOnce`; the totals and means
/// asserted below say what each option costs in proof nodes and in applications of the rules the
/// option switches between, and the per-function figures must equal the checked-in baseline.
///
/// Manual only — tagged out of `test` and not a CI job, because it runs 556 proofs. Run it with
/// `./gradlew :keyext.solidity.core:testProofSize`, and regenerate the baseline and the numbers
/// asserted here by adding
/// `-Dorg.key_project.solidity.taclets.IndexWriteCaptureProofSizeTest.update=true`.
@Tag("proofSize")
public class IndexWriteCaptureProofSizeTest {

    private static final List<String> CHOICES = List.of("receiverThenIndex", "allAtOnce");

    private static final int RECEIVER_THEN_INDEX = 0;

    private static final int ALL_AT_ONCE = 1;

    /// The rules the choice switches between, `\rules(indexWriteCapture:…)` of
    /// `solidityProgramRules.key`: the two-step split, then the merged `…CaptureAll` five.
    private static final List<Set<String>> CAPTURE_RULES = List.of(
        Set.of("storageIndexWrite_unfold_leftFst",
            "storageIndexWriteStorageRef_unfold_leftFst",
            "memoryToStorageIndex_unfold_leftFst",
            "memoryIndexWrite_unfold_leftFst",
            "memoryIndexWriteMemRef_unfold_leftFst",
            "storageIndexWriteNonSimpleIndexCapture",
            "storageIndexWriteStorageRefNonSimpleIndexCapture",
            "memoryToStorageIndexNonSimpleIndexCapture",
            "memoryIndexWriteNonSimpleIndexCapture",
            "memoryIndexWriteMemRefNonSimpleIndexCapture"),
        Set.of("storageIndexWriteCaptureAll",
            "storageIndexWriteStorageRefCaptureAll",
            "memoryToStorageIndexCaptureAll",
            "memoryIndexWriteCaptureAll",
            "memoryIndexWriteMemRefCaptureAll"));

    private static final int MAX_STEPS = 30000;

    private static final String BASELINE_RESOURCE =
        "org/key_project/solidity/proofsize/indexWriteCapture.csv";

    private static final String HEADER = "function,receiverThenIndexNodes,"
        + "receiverThenIndexRuleApps,receiverThenIndexClosed,allAtOnceNodes,allAtOnceRuleApps,"
        + "allAtOnceClosed";

    private static final String UPDATE_PROPERTY =
        "org.key_project.solidity.taclets.IndexWriteCaptureProofSizeTest.update";

    private static final boolean UPDATING = Boolean.getBoolean(UPDATE_PROPERTY);

    private static final double MEAN_TOLERANCE = 0.005;

    private record Measurement(int nodes, int ruleApps, boolean closed) {
        @Override
        public String toString() {
            return nodes + (closed ? "" : "!") + "|" + ruleApps;
        }
    }

    private static Map<String, List<Measurement>> measurements = Map.of();

    @BeforeAll
    static void measureTestSuite() throws Exception {
        List<String> functions = SolidityExampleTests.testSuiteFunctions(name -> true)
                .map(arguments -> (String) arguments.get()[0]).toList();

        Map<String, List<Measurement>> measured = new LinkedHashMap<>();
        for (String function : functions) {
            List<Measurement> perChoice = new ArrayList<>();
            for (String choice : CHOICES) {
                Proof proof = SolidityExampleTests.proveTestSuiteFunction(function, MAX_STEPS,
                    SolidityExampleTests.KEEP_TIMEOUT, List.of("indexWriteCapture:" + choice));
                perChoice.add(new Measurement(proof.countNodes(),
                    countCaptureRuleApps(proof, CHOICES.indexOf(choice)), proof.closed()));
            }
            measured.put(function, List.copyOf(perChoice));
        }
        measurements = measured;

        if (UPDATING) {
            Path written = writeBaseline(measured);
            System.out.println(
                "baseline rewritten: " + written.toAbsolutePath() + "\n" + summary(measured));
        }
    }

    /// The whole file: what each option costs over every function `run-key.sh FILE.sol` proves.
    @Test
    void wholeFileNodeCounts() {
        assumeFalse(UPDATING, "regeneration run");
        Map<String, List<Measurement>> file = measurements;

        assertEquals(278, file.size(),
            "provable functions of TestSuite.sol, each proved under both options");

        assertEquals(82336, totalNodes(file, RECEIVER_THEN_INDEX),
            "receiverThenIndex: total nodes over the whole file");
        assertEquals(82536, totalNodes(file, ALL_AT_ONCE),
            "allAtOnce: total nodes over the whole file");

        assertEquals(296.17, meanNodes(file, RECEIVER_THEN_INDEX), MEAN_TOLERANCE,
            "receiverThenIndex: mean nodes per function over the whole file");
        assertEquals(296.89, meanNodes(file, ALL_AT_ONCE), MEAN_TOLERANCE,
            "allAtOnce: mean nodes per function over the whole file");
    }

    /// The functions that apply a capture rule at all — the population the option is about. The
    /// remaining functions of the file are identical under either choice.
    @Test
    void ruleApplyingFunctionNodeCounts() {
        assumeFalse(UPDATING, "regeneration run");
        Map<String, List<Measurement>> using = usingFunctions();

        assertEquals(38, using.size(),
            "functions of TestSuite.sol that apply an indexWriteCapture rule");

        assertEquals(36280, totalNodes(using, RECEIVER_THEN_INDEX),
            "receiverThenIndex: total nodes over the functions applying the rules");
        assertEquals(36480, totalNodes(using, ALL_AT_ONCE),
            "allAtOnce: total nodes over the functions applying the rules");

        assertEquals(954.74, meanNodes(using, RECEIVER_THEN_INDEX), MEAN_TOLERANCE,
            "receiverThenIndex: mean nodes per function applying the rules");
        assertEquals(960.00, meanNodes(using, ALL_AT_ONCE), MEAN_TOLERANCE,
            "allAtOnce: mean nodes per function applying the rules");
    }

    /// How often the option-guarded rules themselves fire. `allAtOnce` merges the receiver and
    /// index steps into one application, so it applies fewer rules for the same writes.
    @Test
    void captureRuleApplicationCounts() {
        assumeFalse(UPDATING, "regeneration run");
        Map<String, List<Measurement>> file = measurements;
        Map<String, List<Measurement>> using = usingFunctions();

        assertEquals(46, totalRuleApps(file, RECEIVER_THEN_INDEX),
            "receiverThenIndex: rule applications over the whole file");
        assertEquals(39, totalRuleApps(file, ALL_AT_ONCE),
            "allAtOnce: rule applications over the whole file");

        assertEquals(1.21, meanRuleApps(using, RECEIVER_THEN_INDEX), MEAN_TOLERANCE,
            "receiverThenIndex: mean rule applications per function applying the rules");
        assertEquals(1.03, meanRuleApps(using, ALL_AT_ONCE), MEAN_TOLERANCE,
            "allAtOnce: mean rule applications per function applying the rules");
    }

    @Test
    void everyFunctionClosesUnderBothOptions() {
        assumeFalse(UPDATING, "regeneration run");
        List<String> open = measurements.entrySet().stream()
                .filter(entry -> entry.getValue().stream().anyMatch(m -> !m.closed()))
                .map(entry -> entry.getKey() + " " + render(entry.getValue())).toList();
        assertEquals(List.of(), open, "functions that did not close under both options");
    }

    /// The per-function check, the one part that needs the baseline file. Deleting the file only
    /// skips this test — the totals and means above are asserted from the source — and a
    /// regeneration run writes it back.
    @Test
    void proofSizeMatchesTheBaseline() throws Exception {
        assumeFalse(UPDATING, "regeneration run");
        assumeTrue(baselineResource() != null,
            "no checked-in baseline; regenerate with -D" + UPDATE_PROPERTY + "=true");
        Map<String, List<Measurement>> expected = readBaseline();
        List<String> mismatches = new ArrayList<>();

        for (String function : measurements.keySet()) {
            if (!expected.containsKey(function)) {
                mismatches.add(row(function, null, measurements.get(function)));
            }
        }
        for (Map.Entry<String, List<Measurement>> entry : expected.entrySet()) {
            List<Measurement> got = measurements.get(entry.getKey());
            if (got == null || !got.equals(entry.getValue())) {
                mismatches.add(row(entry.getKey(), entry.getValue(), got));
            }
        }

        if (!mismatches.isEmpty()) {
            fail(report(mismatches, expected, measurements));
        }
    }

    private static int countCaptureRuleApps(Proof proof, int choice) {
        Set<String> rules = CAPTURE_RULES.get(choice);
        int applications = 0;
        Iterator<Node> nodes = proof.root().subtreeIterator();
        while (nodes.hasNext()) {
            RuleApp applied = nodes.next().getAppliedRuleApp();
            if (applied != null && rules.contains(applied.rule().name().toString())) {
                applications++;
            }
        }
        return applications;
    }

    private static Map<String, List<Measurement>> usingFunctions() {
        Map<String, List<Measurement>> using = new LinkedHashMap<>();
        measurements.forEach((function, perChoice) -> {
            if (perChoice.stream().anyMatch(m -> m.ruleApps() > 0)) {
                using.put(function, perChoice);
            }
        });
        return using;
    }

    private static long totalNodes(Map<String, List<Measurement>> selection, int choice) {
        return selection.values().stream().mapToLong(perChoice -> perChoice.get(choice).nodes())
                .sum();
    }

    private static double meanNodes(Map<String, List<Measurement>> selection, int choice) {
        return (double) totalNodes(selection, choice) / selection.size();
    }

    private static long totalRuleApps(Map<String, List<Measurement>> selection, int choice) {
        return selection.values().stream().mapToLong(perChoice -> perChoice.get(choice).ruleApps())
                .sum();
    }

    private static double meanRuleApps(Map<String, List<Measurement>> selection, int choice) {
        return (double) totalRuleApps(selection, choice) / selection.size();
    }

    private static String summary(Map<String, List<Measurement>> measured) {
        StringBuilder text = new StringBuilder();
        appendSummaryLine(text, "whole file", measured);
        appendSummaryLine(text, "functions applying the rules", usingFunctions());
        return text.append("update the assertions of wholeFileNodeCounts, ")
                .append("ruleApplyingFunctionNodeCounts and captureRuleApplicationCounts with ")
                .append("these numbers").toString();
    }

    private static void appendSummaryLine(StringBuilder text, String label,
            Map<String, List<Measurement>> selection) {
        text.append(label).append(" (").append(selection.size()).append("):");
        for (int choice = 0; choice < CHOICES.size(); choice++) {
            text.append(' ').append(CHOICES.get(choice)).append(" nodes total=")
                    .append(totalNodes(selection, choice))
                    .append(String.format(" mean=%.2f", meanNodes(selection, choice)))
                    .append(" ruleApps total=").append(totalRuleApps(selection, choice))
                    .append(String.format(" mean=%.2f", meanRuleApps(selection, choice)));
        }
        text.append('\n');
    }

    private static String row(String function, List<Measurement> expected,
            List<Measurement> actual) {
        return String.format("%-52s %-22s %-22s", function,
            expected == null ? "(absent)" : render(expected),
            actual == null ? "(absent)" : render(actual));
    }

    private static String render(List<Measurement> perChoice) {
        return perChoice.get(RECEIVER_THEN_INDEX) + " / " + perChoice.get(ALL_AT_ONCE);
    }

    private static String report(List<String> mismatches, Map<String, List<Measurement>> expected,
            Map<String, List<Measurement>> actual) {
        StringBuilder message = new StringBuilder("proof sizes differ from the baseline in ")
                .append(mismatches.size()).append(" function(s).\n").append("Node counts are ")
                .append(String.join(" / ", CHOICES))
                .append("; each cell is nodes|indexWriteCapture rule applications, and a ")
                .append("trailing ! on the node count marks a proof that did not close.\n")
                .append(String.format("%-52s %-22s %-22s", "function", "expected", "actual"))
                .append('\n');
        mismatches.forEach(line -> message.append(line).append('\n'));
        return message.append("baseline totals: ").append(totalsOf(expected)).append('\n')
                .append(summary(actual)).append('\n').append("Regenerate with -D")
                .append(UPDATE_PROPERTY).append("=true").toString();
    }

    private static String totalsOf(Map<String, List<Measurement>> selection) {
        StringBuilder text = new StringBuilder();
        for (int choice = 0; choice < CHOICES.size(); choice++) {
            text.append(choice == 0 ? "" : ", ").append(CHOICES.get(choice)).append('=')
                    .append(totalNodes(selection, choice));
        }
        return text.toString();
    }

    private static @Nullable URL baselineResource() {
        return IndexWriteCaptureProofSizeTest.class.getClassLoader().getResource(BASELINE_RESOURCE);
    }

    private static Map<String, List<Measurement>> readBaseline() throws IOException {
        Map<String, List<Measurement>> baseline = new LinkedHashMap<>();
        try (InputStream stream = IndexWriteCaptureProofSizeTest.class.getClassLoader()
                .getResourceAsStream(BASELINE_RESOURCE)) {
            if (stream == null) {
                fail("missing baseline resource " + BASELINE_RESOURCE + "; regenerate with -D"
                    + UPDATE_PROPERTY + "=true");
            }
            new String(stream.readAllBytes(), StandardCharsets.UTF_8).lines()
                    .filter(line -> !line.isBlank() && !line.startsWith("function,"))
                    .forEach(line -> {
                        String[] cells = line.split(",");
                        baseline.put(cells[0],
                            List.of(
                                new Measurement(Integer.parseInt(cells[1]),
                                    Integer.parseInt(cells[2]), Boolean.parseBoolean(cells[3])),
                                new Measurement(Integer.parseInt(cells[4]),
                                    Integer.parseInt(cells[5]), Boolean.parseBoolean(cells[6]))));
                    });
        }
        return baseline;
    }

    private static Path writeBaseline(Map<String, List<Measurement>> measured) throws IOException {
        StringBuilder csv = new StringBuilder(HEADER).append('\n');
        measured.forEach((function, perChoice) -> csv.append(function).append(',')
                .append(perChoice.get(RECEIVER_THEN_INDEX).nodes()).append(',')
                .append(perChoice.get(RECEIVER_THEN_INDEX).ruleApps()).append(',')
                .append(perChoice.get(RECEIVER_THEN_INDEX).closed()).append(',')
                .append(perChoice.get(ALL_AT_ONCE).nodes()).append(',')
                .append(perChoice.get(ALL_AT_ONCE).ruleApps()).append(',')
                .append(perChoice.get(ALL_AT_ONCE).closed()).append('\n'));
        Path target = baselineSource();
        Files.createDirectories(target.getParent());
        Files.writeString(target, csv.toString(), StandardCharsets.UTF_8);
        return target;
    }

    /// The baseline in the source tree, which regeneration overwrites. The classpath copy is
    /// read-only, and the suite may run from the module directory or from the repository root.
    private static Path baselineSource() {
        Path moduleResources = Path.of("src/test/resources");
        Path resources = Files.exists(moduleResources) ? moduleResources
                : Path.of("keyext.solidity.core/src/test/resources");
        return resources.resolve(BASELINE_RESOURCE);
    }
}
