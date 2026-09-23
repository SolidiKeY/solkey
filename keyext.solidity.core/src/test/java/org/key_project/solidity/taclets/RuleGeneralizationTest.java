/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.taclets;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.DynamicContainer;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/// Verifies the `// generalization:` comments in `solidityProgramRules.key`. Each comment gives a
/// template taclet with numbered placeholders `⟨0⟩⟨1⟩…` and `⟨name⟩`, followed by a table whose
/// rows name a taclet and the values of the placeholders; filling the template with a row must
/// reproduce that taclet exactly, up to whitespace. See `docs/rule-generalizations.md`.
@Tag("ruleGeneralization")
public class RuleGeneralizationTest {

    private static final String RULES_RESOURCE =
        "org/key_project/solidity/proof/rules/solidityProgramRules.key";
    private static final String MARKER = "// generalization:";
    private static final String NAME = "⟨name⟩";
    private static final Pattern PLACEHOLDER = Pattern.compile("⟨(\\d+)⟩");
    private static final Pattern TACLET_START = Pattern.compile("^ {4}(\\w+) \\{$");

    private record Generalization(int line, String name, String body, List<String> header,
            List<List<String>> rows) {
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

    private static List<Generalization> parseGeneralizations(List<String> lines) {
        List<Generalization> result = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            if (!lines.get(i).trim().equals(MARKER)) {
                continue;
            }
            List<String> content = new ArrayList<>();
            for (int j = i + 1; j < lines.size() && lines.get(j).trim().startsWith("//"); j++) {
                content.add(lines.get(j).trim().substring(2).stripLeading());
            }
            int line = i + 1;
            assertFalse(content.isEmpty(), "line " + line + ": empty generalization");
            String header = content.get(0);
            assertTrue(header.endsWith(" {"),
                "line " + line + ": a generalization starts with `name {`, found " + header);
            int close = content.indexOf("};");
            assertTrue(close > 0, "line " + line + ": template without a closing `};`");
            assertTrue(close + 2 < content.size(),
                "line " + line + ": a generalization needs a table header and at least one row");
            List<List<String>> table = content.subList(close + 1, content.size()).stream()
                    .map(row -> List.of(row.split("\\s{2,}"))).toList();
            result.add(new Generalization(line, header.substring(0, header.length() - 2),
                String.join("\n", content.subList(1, close)), table.get(0),
                table.subList(1, table.size())));
        }
        return result;
    }

    private static String normalize(String text) {
        return text.replaceAll("//[^\n]*", " ").replaceAll("\\s+", " ").trim();
    }

    private static String placeholder(int i) {
        return "⟨" + i + "⟩";
    }

    private static String instantiate(String template, List<String> values) {
        return PLACEHOLDER.matcher(template).replaceAll(
            m -> Matcher.quoteReplacement(values.get(Integer.parseInt(m.group(1)))));
    }

    private static String firstDivergence(String expected, String actual) {
        int limit = Math.min(expected.length(), actual.length());
        int i = 0;
        while (i < limit && expected.charAt(i) == actual.charAt(i)) {
            i++;
        }
        int from = Math.max(0, i - 60);
        return "diverges at offset " + i + ":\n  template ..."
            + expected.substring(from, Math.min(expected.length(), i + 60)) + "...\n  taclet   ..."
            + actual.substring(from, Math.min(actual.length(), i + 60)) + "...";
    }

    private static void checkWellFormed(Generalization g) {
        String where = "line " + g.line() + ": ";
        assertEquals(1, g.name().split(NAME, -1).length - 1,
            where + "the template name must contain " + NAME + " exactly once");
        int holes = g.header().size() - 1;
        List<String> expectedHeader = new ArrayList<>(List.of("taclet"));
        IntStream.range(0, holes).mapToObj(RuleGeneralizationTest::placeholder)
                .forEach(expectedHeader::add);
        assertEquals(expectedHeader, g.header(), where + "table header");
        Set<Integer> used = PLACEHOLDER.matcher(g.body()).results()
                .map(m -> Integer.parseInt(m.group(1)))
                .collect(Collectors.toCollection(TreeSet::new));
        assertEquals(IntStream.range(0, holes).boxed().toList(), List.copyOf(used),
            where + "the template must use exactly the placeholders of the table header");
        for (List<String> row : g.rows()) {
            assertEquals(g.header().size(), row.size(), where + "row " + row);
        }
    }

    private static String divergence(Generalization g, List<String> row,
            Map<String, String> taclets) {
        String name = row.get(0);
        String body = taclets.get(name);
        if (body == null) {
            return "no taclet named " + name + " in " + RULES_RESOURCE;
        }
        String[] affixes = g.name().split(NAME, -1);
        if (!name.startsWith(affixes[0]) || !name.endsWith(affixes[1])
                || name.length() <= affixes[0].length() + affixes[1].length()) {
            return name + " does not match the template name " + g.name();
        }
        String expected = normalize(instantiate(g.body(), row.subList(1, row.size())));
        String actual = normalize(body);
        return expected.equals(actual) ? null
                : name + " is not an instance of the template; "
                    + firstDivergence(expected, actual);
    }

    @TestFactory
    Stream<DynamicNode> everyListedTacletIsAnInstanceOfItsTemplate() {
        List<String> lines = resourceLines();
        Map<String, String> taclets = parseTaclets(lines);
        return parseGeneralizations(lines).stream().map(g -> {
            List<DynamicTest> tests = new ArrayList<>();
            tests.add(DynamicTest.dynamicTest("well formed", () -> checkWellFormed(g)));
            for (List<String> row : g.rows()) {
                tests.add(DynamicTest.dynamicTest(row.get(0), () -> {
                    String problem = divergence(g, row, taclets);
                    if (problem != null) {
                        fail("line " + g.line() + ": " + problem);
                    }
                }));
            }
            return DynamicContainer.dynamicContainer("line " + g.line() + ": " + g.name(), tests);
        });
    }

    @Test
    void noTacletIsListedTwice() {
        List<Generalization> generalizations = parseGeneralizations(resourceLines());
        assertFalse(generalizations.isEmpty(), "no " + MARKER + " comment in " + RULES_RESOURCE);
        Map<String, Integer> listedAt = new HashMap<>();
        for (Generalization g : generalizations) {
            for (List<String> row : g.rows()) {
                Integer previous = listedAt.put(row.get(0), g.line());
                assertNull(previous, row.get(0) + " is listed at lines " + previous + " and "
                    + g.line());
            }
        }
    }

    @Test
    void checkerDetectsAnInjectedDivergence() {
        List<String> lines = List.of(
            "    " + MARKER,
            "    //     foo" + NAME + "Rule {",
            "    //         x = a ⟨0⟩ b;",
            "    //     };",
            "    //     taclet      ⟨0⟩",
            "    //     fooAddRule  +",
            "    //     fooMulRule  *",
            "    fooAddRule {",
            "        x = a + b;",
            "    };",
            "    fooMulRule {",
            "        x = b * a;",
            "    };");
        Generalization g = parseGeneralizations(lines).get(0);
        checkWellFormed(g);
        Map<String, String> taclets = parseTaclets(lines);
        assertNull(divergence(g, g.rows().get(0), taclets));
        assertNotNull(divergence(g, g.rows().get(1), taclets));
    }
}
