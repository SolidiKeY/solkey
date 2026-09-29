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
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Verifies the `// generalization:` comments in `solidityProgramRules.key`. A comment lists
/// taclets that differ only in a few tokens and shows their shared template, with `⟨name⟩` in the
/// name and numbered placeholders `⟨0⟩⟨1⟩…` in the body, followed by one row per taclet giving
/// the placeholder values. The test computes that comment from the listed taclets — aligning
/// their bodies token by token so that only the differing tokens become columns — and requires
/// the file to carry exactly it. See `docs/rule-generalizations.md`.
@Tag("ruleGeneralization")
public class RuleGeneralizationTest {

    private static final String RULES_RESOURCE =
        "org/key_project/solidity/proof/rules/solidityProgramRules.key";
    private static final Path RULES_SOURCE = Path.of("src/main/resources", RULES_RESOURCE);
    private static final String UPDATE_PROPERTY =
        "org.key_project.solidity.taclets.RuleGeneralizationTest.update";
    private static final String MARKER = "// generalization:";
    private static final String CLOSE = "//     };";
    private static final String NAME = "⟨name⟩";
    private static final Pattern PLACEHOLDER = Pattern.compile("⟨(\\d+)⟩");
    private static final Pattern TACLET_START = Pattern.compile("^ {4}(\\w+) \\{$");
    private static final Pattern TOKEN = Pattern.compile("[A-Za-z0-9_]+|\\s+|.");

    private record Comment(int start, int end, List<String> lines, List<String> names) {
    }

    private record Body(String indent, List<String> raw, List<String> tokens) {
    }

    private static List<String> resourceLines() {
        try (InputStream in =
            RuleGeneralizationTest.class.getClassLoader().getResourceAsStream(RULES_RESOURCE)) {
            assertNotNull(in, "rules resource must be on the classpath: " + RULES_RESOURCE);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8).lines().toList();
        } catch (IOException e) {
            throw new IllegalStateException("cannot read " + RULES_RESOURCE, e);
        }
    }

    private static Map<String, String> parseTaclets(List<String> lines) {
        Map<String, String> taclets = new LinkedHashMap<>();
        for (int i = 0; i < lines.size(); i++) {
            Matcher matcher = TACLET_START.matcher(lines.get(i));
            if (!matcher.matches()) {
                continue;
            }
            StringBuilder body = new StringBuilder();
            int end = i + 1;
            while (end < lines.size() && !lines.get(end).equals("    };")) {
                body.append(lines.get(end)).append('\n');
                end++;
            }
            assertTrue(end < lines.size(), "unterminated taclet " + matcher.group(1));
            assertNull(taclets.put(matcher.group(1), body.toString()),
                "duplicate taclet name " + matcher.group(1));
            i = end;
        }
        return taclets;
    }

    private static List<Comment> parseComments(List<String> lines) {
        List<Comment> comments = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            if (!lines.get(i).trim().equals(MARKER)) {
                continue;
            }
            int end = i + 1;
            while (end < lines.size() && lines.get(end).trim().startsWith("//")
                    && !lines.get(end).trim().equals(MARKER)) {
                end++;
            }
            List<String> comment = lines.subList(i, end).stream().map(String::trim).toList();
            int close = comment.indexOf(CLOSE);
            List<String> names = comment.subList(close < 0 ? 1 : close + 2, comment.size())
                    .stream().map(row -> row.substring(2).trim().split("\\s{2,}")[0]).toList();
            assertFalse(names.isEmpty(), "line " + (i + 1) + ": a generalization lists no taclet");
            comments.add(new Comment(i, end, comment, names));
        }
        return comments;
    }

    private static Body tokenize(String body) {
        String text = body.replaceAll("[ \t]*//[^\n]*", "").replaceAll("(?m)^[ \t]*\n", "");
        String stripped = text.strip();
        List<String> raw = new ArrayList<>();
        Matcher matcher = TOKEN.matcher(stripped);
        while (matcher.find()) {
            raw.add(matcher.group());
        }
        return new Body(text.substring(0, text.length() - text.stripLeading().length()), raw,
            raw.stream().map(t -> t.isBlank() ? " " : t).toList());
    }

    private static String normalize(String text) {
        return String.join("", tokenize(text).tokens());
    }

    private static void matchBlocks(List<String> a, int alo, int ahi, List<String> b, int blo,
            int bhi, int[] match) {
        int besti = alo;
        int bestj = blo;
        int best = 0;
        int[] previous = new int[bhi - blo + 1];
        for (int i = alo; i < ahi; i++) {
            int[] current = new int[bhi - blo + 1];
            for (int j = blo; j < bhi; j++) {
                if (a.get(i).equals(b.get(j))) {
                    int k = previous[j - blo] + 1;
                    current[j - blo + 1] = k;
                    if (k > best) {
                        besti = i - k + 1;
                        bestj = j - k + 1;
                        best = k;
                    }
                }
            }
            previous = current;
        }
        if (best == 0) {
            return;
        }
        for (int k = 0; k < best; k++) {
            match[besti + k] = bestj + k;
        }
        matchBlocks(a, alo, besti, b, blo, bestj, match);
        matchBlocks(a, besti + best, ahi, b, bestj + best, bhi, match);
    }

    private static int[] match(List<String> a, List<String> b) {
        int[] match = new int[a.size()];
        Arrays.fill(match, -1);
        matchBlocks(a, 0, a.size(), b, 0, b.size(), match);
        return match;
    }

    private static List<String> matched(List<String> a, int[] match) {
        List<String> result = new ArrayList<>();
        for (int i = 0; i < a.size(); i++) {
            if (match[i] >= 0) {
                result.add(a.get(i));
            }
        }
        return result;
    }

    private static int[][] align(List<Body> bodies) {
        List<String> common = bodies.get(0).tokens();
        boolean stable = false;
        while (!stable) {
            stable = true;
            for (Body body : bodies) {
                List<String> kept = matched(common, match(common, body.tokens()));
                if (kept.size() < common.size()) {
                    common = kept;
                    stable = false;
                }
            }
        }
        List<String> shared = common;
        return bodies.stream().map(body -> match(shared, body.tokens())).toArray(int[][]::new);
    }

    private static List<String> values(List<Body> bodies, int[][] positions, int from, int to) {
        List<String> values = new ArrayList<>();
        for (int x = 0; x < bodies.size(); x++) {
            List<String> tokens = bodies.get(x).tokens();
            int start = from < 0 ? 0 : positions[x][from] + 1;
            int end = to == positions[x].length ? tokens.size() : positions[x][to];
            values.add(String.join("", tokens.subList(start, end)));
        }
        return values;
    }

    private static boolean isHole(List<String> values) {
        return values.stream().distinct().count() > 1;
    }

    private static boolean isBad(List<String> values) {
        return isHole(values) && values.stream()
                .anyMatch(v -> v.isEmpty() || v.startsWith(" ") || v.endsWith(" "));
    }

    private static List<int[]> regions(boolean[] kept) {
        List<int[]> regions = new ArrayList<>();
        int from = -1;
        for (int k = 0; k <= kept.length; k++) {
            if (k == kept.length || kept[k]) {
                regions.add(new int[] { from, k });
                from = k;
            }
        }
        return regions;
    }

    private static List<int[]> holes(List<Body> bodies, int[][] positions) {
        boolean[] kept = new boolean[positions[0].length];
        Arrays.fill(kept, true);
        while (true) {
            List<int[]> regions = regions(kept);
            List<List<String>> values = regions.stream()
                    .map(r -> values(bodies, positions, r[0], r[1])).toList();
            int bad = -1;
            for (int r = 0; r < regions.size() && bad < 0; r++) {
                if (isBad(values.get(r))) {
                    bad = r;
                }
            }
            if (bad < 0) {
                List<int[]> holes = new ArrayList<>();
                for (int r = 0; r < regions.size(); r++) {
                    if (isHole(values.get(r))) {
                        holes.add(regions.get(r));
                    }
                }
                return holes;
            }
            int left = bad - 1;
            while (left >= 0 && !isHole(values.get(left))) {
                left--;
            }
            int right = bad + 1;
            while (right < regions.size() && !isHole(values.get(right))) {
                right++;
            }
            boolean hasLeft = left >= 0;
            boolean hasRight = right < regions.size();
            if (hasRight && (!hasLeft || right - bad <= bad - left)) {
                for (int r = bad; r < right; r++) {
                    kept[regions.get(r)[1]] = false;
                }
            } else if (hasLeft) {
                for (int r = left; r < bad; r++) {
                    kept[regions.get(r)[1]] = false;
                }
            } else {
                int[] region = regions.get(bad);
                boolean widenRight = region[1] < kept.length && (region[0] < 0
                        || values.get(bad).stream().anyMatch(v -> v.isEmpty() || v.endsWith(" ")));
                kept[widenRight ? region[1] : region[0]] = false;
            }
        }
    }

    private static boolean isBoundary(String name, int at) {
        return at == 0 || at == name.length() || Character.isUpperCase(name.charAt(at))
                || name.charAt(at) == '_' || name.charAt(at - 1) == '_';
    }

    private static boolean allBoundaries(List<String> names, int length, boolean fromEnd) {
        return names.stream().allMatch(n -> isBoundary(n, fromEnd ? n.length() - length : length));
    }

    private static int snap(List<String> names, int length, boolean fromEnd) {
        int snapped = length;
        while (snapped > 0 && !allBoundaries(names, snapped, fromEnd)) {
            snapped--;
        }
        return snapped;
    }

    private static boolean sameChar(List<String> names, int offset, boolean fromEnd) {
        String first = names.get(0);
        return names.stream().allMatch(n -> offset < n.length() && offset < first.length()
                && n.charAt(fromEnd ? n.length() - 1 - offset : offset) == first
                        .charAt(fromEnd ? first.length() - 1 - offset : offset));
    }

    private static boolean leavesAName(List<String> names, int prefix, int suffix) {
        return names.stream().allMatch(n -> prefix + suffix < n.length());
    }

    private static String nameTemplate(List<String> names) {
        int prefix = 0;
        while (sameChar(names, prefix, false)) {
            prefix++;
        }
        int suffix = 0;
        while (sameChar(names, suffix, true)) {
            suffix++;
        }
        prefix = snap(names, prefix, false);
        suffix = snap(names, suffix, true);
        while (suffix > 0 && !leavesAName(names, prefix, suffix)) {
            suffix = snap(names, suffix - 1, true);
        }
        String first = names.get(0);
        return first.substring(0, prefix) + NAME + first.substring(first.length() - suffix);
    }

    private static String instantiate(String template, List<String> values) {
        return PLACEHOLDER.matcher(template).replaceAll(
            m -> Matcher.quoteReplacement(values.get(Integer.parseInt(m.group(1)))));
    }

    private static List<String> generalize(List<String> names, Map<String, String> taclets) {
        List<Body> bodies = names.stream().map(name -> {
            String body = taclets.get(name);
            assertNotNull(body, "no taclet named " + name + " in " + RULES_RESOURCE);
            return tokenize(body);
        }).toList();
        int[][] positions = align(bodies);
        List<int[]> holes = holes(bodies, positions);
        Map<List<String>, Integer> placeholders = new LinkedHashMap<>();
        String[] template = bodies.get(0).raw().toArray(String[]::new);
        for (int[] hole : holes) {
            List<String> values = values(bodies, positions, hole[0], hole[1]);
            int index = placeholders.computeIfAbsent(values, v -> placeholders.size());
            int start = hole[0] < 0 ? 0 : positions[0][hole[0]] + 1;
            int end = hole[1] == positions[0].length ? template.length : positions[0][hole[1]];
            template[start] = "⟨" + index + "⟩";
            Arrays.fill(template, start + 1, end, "");
        }
        String text = bodies.get(0).indent() + String.join("", template);
        List<List<String>> rows = new ArrayList<>();
        List<String> header = new ArrayList<>(List.of("taclet"));
        for (int i = 0; i < placeholders.size(); i++) {
            header.add("⟨" + i + "⟩");
        }
        rows.add(header);
        for (int x = 0; x < names.size(); x++) {
            int row = x;
            List<String> values = placeholders.keySet().stream().map(v -> v.get(row)).toList();
            assertEquals(String.join("", bodies.get(x).tokens()),
                normalize(instantiate(text, values)), names.get(x) + ": template round trip");
            List<String> cells = new ArrayList<>(List.of(names.get(x)));
            cells.addAll(values);
            rows.add(cells);
        }
        int[] widths = new int[rows.get(0).size()];
        for (List<String> row : rows) {
            for (int c = 0; c < row.size(); c++) {
                widths[c] = Math.max(widths[c], row.get(c).length());
            }
        }
        List<String> comment = new ArrayList<>();
        comment.add(MARKER);
        comment.add("//     " + nameTemplate(names) + " {");
        text.lines().forEach(line -> comment.add(("// " + line).stripTrailing()));
        comment.add(CLOSE);
        for (List<String> row : rows) {
            StringBuilder line = new StringBuilder("//   ");
            for (int c = 0; c < row.size(); c++) {
                line.append("  ").append(String.format("%-" + widths[c] + "s", row.get(c)));
            }
            comment.add(line.toString().stripTrailing());
        }
        return comment;
    }

    @TestFactory
    Stream<DynamicTest> everyCommentIsTheMinimalGeneralizationOfItsTaclets() {
        Assumptions.assumeFalse(Boolean.getBoolean(UPDATE_PROPERTY),
            "the generalization comments are being rewritten");
        List<String> lines = resourceLines();
        Map<String, String> taclets = parseTaclets(lines);
        return parseComments(lines).stream().map(comment -> DynamicTest.dynamicTest(
            "line " + (comment.start() + 1) + ": " + String.join(", ", comment.names()), () -> {
                List<String> expected = generalize(comment.names(), taclets);
                assertEquals(String.join("\n", expected), String.join("\n", comment.lines()),
                    "line " + (comment.start() + 1) + ": the comment is not the minimal"
                        + " generalization of its taclets; regenerate with ./gradlew"
                        + " :keyext.solidity.core:testRuleGeneralization -D" + UPDATE_PROPERTY
                        + "=true");
            }));
    }

    @Test
    void noTacletIsListedTwice() {
        List<Comment> comments = parseComments(resourceLines());
        assertFalse(comments.isEmpty(), "no " + MARKER + " comment in " + RULES_RESOURCE);
        Map<String, Integer> listedAt = new HashMap<>();
        for (Comment comment : comments) {
            for (String name : comment.names()) {
                Integer previous = listedAt.put(name, comment.start() + 1);
                assertNull(previous,
                    name + " is listed at lines " + previous + " and " + (comment.start() + 1));
            }
        }
    }

    @Test
    void onlyTheDifferingTokensBecomeColumns() {
        Map<String, String> taclets = Map.of(
            "fooAddRule", "        x = a + b;\n",
            "fooSubRule", "        x = a - b;\n",
            "fooSwapRule", "        x = b * a;\n");
        assertEquals(List.of(MARKER, "//     foo" + NAME + "Rule {", "//         x = a ⟨0⟩ b;",
            CLOSE, "//     taclet      ⟨0⟩", "//     fooAddRule  +", "//     fooSubRule  -"),
            generalize(List.of("fooAddRule", "fooSubRule"), taclets));
        List<String> swapped = generalize(List.of("fooAddRule", "fooSwapRule"), taclets);
        assertTrue(swapped.get(swapped.size() - 1).trim().split("\\s{2,}").length > 2,
            "a reordered body needs more than one column: " + swapped);
    }

    @Test
    void rewriteGeneralizationComments() throws IOException {
        Assumptions.assumeTrue(Boolean.getBoolean(UPDATE_PROPERTY),
            "set -D" + UPDATE_PROPERTY + "=true to rewrite the generalization comments");
        List<String> lines = new ArrayList<>(Files.readAllLines(RULES_SOURCE));
        Map<String, String> taclets = parseTaclets(lines);
        List<Comment> comments = parseComments(lines);
        for (int c = comments.size() - 1; c >= 0; c--) {
            Comment comment = comments.get(c);
            String marker = lines.get(comment.start());
            String indent = marker.substring(0, marker.indexOf("//"));
            List<String> replacement = generalize(comment.names(), taclets).stream()
                    .map(line -> indent + line).toList();
            lines.subList(comment.start(), comment.end()).clear();
            lines.addAll(comment.start(), replacement);
        }
        Files.writeString(RULES_SOURCE, String.join("\n", lines) + "\n");
        Assumptions.abort("rewrote the generalization comments in "
            + RULES_SOURCE.toAbsolutePath() + "; rerun without -D" + UPDATE_PROPERTY);
    }
}
