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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/// Verifies the `// generalized by: family(op=..., ...)` annotations in
/// `solidityProgramRules.key`: every annotated taclet must be an instance of its family's
/// shared skeleton, obtained by replacing the member's operator-specific hole strings with
/// numbered placeholders. All members of a group must produce a byte-identical skeleton, so a
/// claimed generalization that does not hold (any difference beyond the declared holes) fails
/// this test. See `docs/rule-generalizations.md`.
@Tag("ruleGeneralization")
public class RuleGeneralizationTest {

    private static final String RULES_RESOURCE =
        "org/key_project/solidity/proof/rules/solidityProgramRules.key";
    private static final String MARKER = "// generalized by: ";

    private record Member(String name, String op, String fixity, String nameHole,
            List<String> bodyHoles) {
    }

    private record Group(String family, String variant, String loc, List<Member> members) {
    }

    private record Taclet(String name, String body, List<String> comments) {
    }

    private static Member m(String name, String op, String nameHole, String... bodyHoles) {
        return new Member(name, op, null, nameHole, List.of(bodyHoles));
    }

    private static Member mf(String name, String op, String fixity, String nameHole,
            String... bodyHoles) {
        return new Member(name, op, fixity, nameHole, List.of(bodyHoles));
    }

    private static Group g(String family, String variant, String loc, Member... members) {
        return new Group(family, variant, loc, List.of(members));
    }

    private static final List<Group> SPEC = List.of(
        g("storageCompoundAssign", "plain", "root",
            m("storageRootAddAssign", "add", "Add", "+=", "+"),
            m("storageRootSubAssign", "sub", "Sub", "-=", "-"),
            m("storageRootMulAssign", "mul", "Mul", "*=", "*")),
        g("storageCompoundAssign", "plain", "field",
            m("storageFieldAddAssign", "add", "Add", "+=", "+"),
            m("storageFieldSubAssign", "sub", "Sub", "-=", "-"),
            m("storageFieldMulAssign", "mul", "Mul", "*=", "*")),
        g("storageCompoundAssign", "plain", "index",
            m("storageIndexAddAssign", "add", "Add", "+=", "+"),
            m("storageIndexSubAssign", "sub", "Sub", "-=", "-"),
            m("storageIndexMulAssign", "mul", "Mul", "*=", "*")),
        g("storageCompoundAssign", "guarded", "root",
            m("storageRootDivAssign", "div", "Div", "/=", "/"),
            m("storageRootModAssign", "mod", "Mod", "%=", "%")),
        g("storageCompoundAssign", "guarded", "field",
            m("storageFieldDivAssign", "div", "Div", "/=", "/"),
            m("storageFieldModAssign", "mod", "Mod", "%=", "%")),
        g("storageCompoundAssign", "guarded", "index",
            m("storageIndexDivAssign", "div", "Div", "/=", "/"),
            m("storageIndexModAssign", "mod", "Mod", "%=", "%")),
        g("storageCompoundAssign", "unfold", "field",
            m("storageFieldAddAssign_unfold_leftFst", "add", "Add", "+="),
            m("storageFieldSubAssign_unfold_leftFst", "sub", "Sub", "-="),
            m("storageFieldMulAssign_unfold_leftFst", "mul", "Mul", "*="),
            m("storageFieldDivAssign_unfold_leftFst", "div", "Div", "/="),
            m("storageFieldModAssign_unfold_leftFst", "mod", "Mod", "%=")),
        g("storageCompoundAssign", "unfold", "index",
            m("storageIndexAddAssign_unfold_leftFst", "add", "Add", "+="),
            m("storageIndexSubAssign_unfold_leftFst", "sub", "Sub", "-="),
            m("storageIndexMulAssign_unfold_leftFst", "mul", "Mul", "*="),
            m("storageIndexDivAssign_unfold_leftFst", "div", "Div", "/="),
            m("storageIndexModAssign_unfold_leftFst", "mod", "Mod", "%=")),
        g("binaryOp", "unfoldLeft", null,
            m("addition_unfold_left", "add", "addition", "+"),
            m("subtraction_unfold_left", "sub", "subtraction", "-"),
            m("multiplication_unfold_left", "mul", "multiplication", "*"),
            m("power_unfold_left", "pow", "power", "**"),
            m("division_unfold_left", "div", "division", "/"),
            m("modulo_unfold_left", "mod", "modulo", "%")),
        g("binaryOp", "unfoldRight", null,
            m("addition_unfold_right", "add", "addition", "+"),
            m("subtraction_unfold_right", "sub", "subtraction", "-"),
            m("multiplication_unfold_right", "mul", "multiplication", "*"),
            m("power_unfold_right", "pow", "power", "**"),
            m("division_unfold_right", "div", "division", "/"),
            m("modulo_unfold_right", "mod", "modulo", "%")),
        g("binaryOp", "assignment", null,
            m("additionAssignment", "add", "addition", "s#se1 + s#se2", "se1 + se2"),
            m("subtractionAssignment", "sub", "subtraction", "s#se1 - s#se2", "se1 - se2"),
            m("multiplicationAssignment", "mul", "multiplication", "s#se1 * s#se2",
                "se1 * se2"),
            m("powerAssignment", "pow", "power", "s#se1 ** s#se2", "pow(se1, se2)")),
        g("binaryOp", "guardedAssignment", null,
            m("divisionAssignment", "div", "division", "s#se1 / s#se2", "se1 / se2"),
            m("moduloAssignment", "mod", "modulo", "s#se1 % s#se2", "se1 % se2")),
        g("storageIncDec", "stmt", "root",
            mf("storageRootPreincrement", "inc", "pre", "Preincrement", "++s#gp", "+"),
            mf("storageRootPostincrement", "inc", "post", "Postincrement", "s#gp++", "+"),
            mf("storageRootPredecrement", "dec", "pre", "Predecrement", "--s#gp", "-"),
            mf("storageRootPostdecrement", "dec", "post", "Postdecrement", "s#gp--", "-")),
        g("storageIncDec", "stmt", "field",
            mf("storageFieldPreincrement", "inc", "pre", "Preincrement", "++s#sp.s#a", "+"),
            mf("storageFieldPostincrement", "inc", "post", "Postincrement", "s#sp.s#a++",
                "+"),
            mf("storageFieldPredecrement", "dec", "pre", "Predecrement", "--s#sp.s#a", "-"),
            mf("storageFieldPostdecrement", "dec", "post", "Postdecrement", "s#sp.s#a--",
                "-")),
        g("storageIncDec", "stmt", "index",
            mf("storageIndexPreincrement", "inc", "pre", "Preincrement", "++s#sp[s#i]", "+"),
            mf("storageIndexPostincrement", "inc", "post", "Postincrement", "s#sp[s#i]++",
                "+"),
            mf("storageIndexPredecrement", "dec", "pre", "Predecrement", "--s#sp[s#i]", "-"),
            mf("storageIndexPostdecrement", "dec", "post", "Postdecrement", "s#sp[s#i]--",
                "-")),
        g("storageIncDec", "assign", "root",
            mf("storageRootPreincrementAssignment", "inc", "pre", "Preincrement", "++s#gp",
                "+"),
            mf("storageRootPredecrementAssignment", "dec", "pre", "Predecrement", "--s#gp",
                "-")),
        g("storageIncDec", "assign", "root",
            mf("storageRootPostincrementAssignment", "inc", "post", "Postincrement",
                "s#gp++", "+"),
            mf("storageRootPostdecrementAssignment", "dec", "post", "Postdecrement",
                "s#gp--", "-")),
        g("storageIncDec", "assign", "field",
            mf("storageFieldPreincrementAssignment", "inc", "pre", "Preincrement",
                "++s#sp.s#a", "+"),
            mf("storageFieldPredecrementAssignment", "dec", "pre", "Predecrement",
                "--s#sp.s#a", "-")),
        g("storageIncDec", "assign", "field",
            mf("storageFieldPostincrementAssignment", "inc", "post", "Postincrement",
                "s#sp.s#a++", "+"),
            mf("storageFieldPostdecrementAssignment", "dec", "post", "Postdecrement",
                "s#sp.s#a--", "-")),
        g("storageIncDec", "assign", "index",
            mf("storageIndexPreincrementAssignment", "inc", "pre", "Preincrement",
                "++s#sp[s#i]", "+"),
            mf("storageIndexPredecrementAssignment", "dec", "pre", "Predecrement",
                "--s#sp[s#i]", "-")),
        g("storageIncDec", "assign", "index",
            mf("storageIndexPostincrementAssignment", "inc", "post", "Postincrement",
                "s#sp[s#i]++", "+"),
            mf("storageIndexPostdecrementAssignment", "dec", "post", "Postdecrement",
                "s#sp[s#i]--", "-")),
        g("storageIncDec", "unfold", "field",
            mf("storageFieldPreincrement_unfold_leftFst", "inc", "pre", "Preincrement",
                "++s#nsp.s#a", "++s#sp.s#a"),
            mf("storageFieldPostincrement_unfold_leftFst", "inc", "post", "Postincrement",
                "s#nsp.s#a++", "s#sp.s#a++"),
            mf("storageFieldPredecrement_unfold_leftFst", "dec", "pre", "Predecrement",
                "--s#nsp.s#a", "--s#sp.s#a"),
            mf("storageFieldPostdecrement_unfold_leftFst", "dec", "post", "Postdecrement",
                "s#nsp.s#a--", "s#sp.s#a--")),
        g("storageIncDec", "unfold", "index",
            mf("storageIndexPreincrement_unfold_leftFst", "inc", "pre", "Preincrement",
                "++s#nsp[s#i]", "++s#sp[s#i]"),
            mf("storageIndexPostincrement_unfold_leftFst", "inc", "post", "Postincrement",
                "s#nsp[s#i]++", "s#sp[s#i]++"),
            mf("storageIndexPredecrement_unfold_leftFst", "dec", "pre", "Predecrement",
                "--s#nsp[s#i]", "--s#sp[s#i]"),
            mf("storageIndexPostdecrement_unfold_leftFst", "dec", "post", "Postdecrement",
                "s#nsp[s#i]--", "s#sp[s#i]--")),
        g("localIncDec", "decl", null,
            mf("localDeclPreincrement", "inc", "pre", "Preincrement", "++s#lv", "+"),
            mf("localDeclPredecrement", "dec", "pre", "Predecrement", "--s#lv", "-")),
        g("localIncDec", "decl", null,
            mf("localDeclPostincrement", "inc", "post", "Postincrement", "s#lv++", "+"),
            mf("localDeclPostdecrement", "dec", "post", "Postdecrement", "s#lv--", "-")),
        g("localIncDec", "assign", null,
            mf("localAssignPreincrement", "inc", "pre", "Preincrement", "++s#lv", "+"),
            mf("localAssignPredecrement", "dec", "pre", "Predecrement", "--s#lv", "-")),
        g("localIncDec", "assign", null,
            mf("localAssignPostincrement", "inc", "post", "Postincrement", "s#lv++", "+"),
            mf("localAssignPostdecrement", "dec", "post", "Postdecrement", "s#lv--", "-")),
        g("localIncDec", "stmt", null,
            mf("localPreincrement", "inc", "pre", "Preincrement", "++s#lv", "+"),
            mf("localPostincrement", "inc", "post", "Postincrement", "s#lv++", "+"),
            mf("localPredecrement", "dec", "pre", "Predecrement", "--s#lv", "-"),
            mf("localPostdecrement", "dec", "post", "Postdecrement", "s#lv--", "-")),
        g("localCompoundAssign", "plain", null,
            m("localAddAssign", "add", "Add", "+=", "+"),
            m("localSubAssign", "sub", "Sub", "-=", "-"),
            m("localMulAssign", "mul", "Mul", "*=", "*")),
        g("localCompoundAssign", "guarded", null,
            m("localDivAssign", "div", "Div", "/=", "/"),
            m("localModAssign", "mod", "Mod", "%=", "%")),
        g("compoundAssignRhsCapture", null, null,
            m("addAssignValueRhsCapture", "add", "add", "+="),
            m("subAssignValueRhsCapture", "sub", "sub", "-="),
            m("mulAssignValueRhsCapture", "mul", "mul", "*="),
            m("divAssignValueRhsCapture", "div", "div", "/="),
            m("modAssignValueRhsCapture", "mod", "mod", "%=")));

    private static String annotation(Group group, Member member) {
        StringBuilder sb = new StringBuilder(MARKER);
        sb.append(group.family()).append("(op=").append(member.op());
        if (group.loc() != null) {
            sb.append(", loc=").append(group.loc());
        }
        if (member.fixity() != null) {
            sb.append(", fixity=").append(member.fixity());
        }
        if (group.variant() != null) {
            sb.append(", variant=").append(group.variant());
        }
        return sb.append(")").toString();
    }

    private static String groupLabel(Group group) {
        StringBuilder sb = new StringBuilder(group.family());
        if (group.variant() != null) {
            sb.append("/").append(group.variant());
        }
        if (group.loc() != null) {
            sb.append("/").append(group.loc());
        }
        String fixity = group.members().get(0).fixity();
        if (fixity != null) {
            sb.append("/").append(fixity);
        }
        return sb.toString();
    }

    private static String normalize(String text) {
        return text.replaceAll("//[^\n]*", " ").replaceAll("\\s+", " ").trim();
    }

    private static String placeholder(int i) {
        return "⟨" + i + "⟩";
    }

    private static String skeleton(String name, String nameHole, List<String> bodyHoles,
            String body) {
        assertTrue(name.contains(nameHole),
            name + ": name hole '" + nameHole + "' does not occur in the taclet name");
        String result = name.replace(nameHole, "⟨name⟩") + " { ";
        String normalized = normalize(body);
        for (int i = 0; i < bodyHoles.size(); i++) {
            String hole = normalize(bodyHoles.get(i));
            for (int j = i + 1; j < bodyHoles.size(); j++) {
                assertTrue(!normalize(bodyHoles.get(j)).contains(hole),
                    name + ": hole '" + hole + "' is a substring of later hole '"
                        + bodyHoles.get(j) + "'; reorder the holes (most specific first)");
            }
            assertTrue(normalized.contains(hole),
                name + ": hole '" + hole + "' does not occur in the taclet body");
            normalized = normalized.replace(hole, placeholder(i));
        }
        return result + normalized;
    }

    private static String firstDivergence(String expected, String actual) {
        int limit = Math.min(expected.length(), actual.length());
        int i = 0;
        while (i < limit && expected.charAt(i) == actual.charAt(i)) {
            i++;
        }
        int from = Math.max(0, i - 60);
        return "skeletons diverge at offset " + i + ":\n  expected ..."
            + expected.substring(from, Math.min(expected.length(), i + 60)) + "...\n  actual   ..."
            + actual.substring(from, Math.min(actual.length(), i + 60)) + "...";
    }

    private static final Pattern TACLET_START = Pattern.compile("^ {4}(\\w+) \\{$");

    private static Map<String, Taclet> parseTaclets() {
        List<String> lines;
        try (InputStream in =
            RuleGeneralizationTest.class.getClassLoader().getResourceAsStream(RULES_RESOURCE)) {
            assertTrue(in != null, "rules resource must be on the classpath: " + RULES_RESOURCE);
            lines = new String(in.readAllBytes(), StandardCharsets.UTF_8).lines().toList();
        } catch (IOException e) {
            throw new IllegalStateException("cannot read " + RULES_RESOURCE, e);
        }
        Map<String, Taclet> taclets = new LinkedHashMap<>();
        for (int i = 0; i < lines.size(); i++) {
            Matcher matcher = TACLET_START.matcher(lines.get(i));
            if (!matcher.matches()) {
                continue;
            }
            List<String> comments = new ArrayList<>();
            for (int j = i - 1; j >= 0 && lines.get(j).trim().startsWith("//"); j--) {
                comments.add(0, lines.get(j).trim());
            }
            StringBuilder body = new StringBuilder();
            int end = i + 1;
            while (end < lines.size() && !lines.get(end).equals("    };")) {
                body.append(lines.get(end)).append('\n');
                end++;
            }
            assertTrue(end < lines.size(), "unterminated taclet " + matcher.group(1));
            String name = matcher.group(1);
            assertNull(taclets.put(name, new Taclet(name, body.toString(), comments)),
                "duplicate taclet name " + name);
            i = end;
        }
        return taclets;
    }

    private static List<String> markerLines() {
        try (InputStream in =
            RuleGeneralizationTest.class.getClassLoader().getResourceAsStream(RULES_RESOURCE)) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8).lines()
                    .map(String::trim).filter(l -> l.startsWith(MARKER)).toList();
        } catch (IOException e) {
            throw new IllegalStateException("cannot read " + RULES_RESOURCE, e);
        }
    }

    @TestFactory
    Stream<DynamicTest> annotatedTacletsMatchTheirGroupSkeleton() {
        Map<String, Taclet> taclets = parseTaclets();
        List<DynamicTest> tests = new ArrayList<>();
        for (Group group : SPEC) {
            tests.add(DynamicTest.dynamicTest(groupLabel(group), () -> {
                String reference = null;
                String referenceName = null;
                for (Member member : group.members()) {
                    Taclet taclet = taclets.get(member.name());
                    assertTrue(taclet != null, "no taclet named " + member.name()
                        + " in " + RULES_RESOURCE);
                    String expected = annotation(group, member);
                    assertTrue(taclet.comments().contains(expected),
                        member.name() + ": missing or wrong annotation; expected the comment"
                            + " line\n    " + expected + "\ndirectly above the taclet, found "
                            + taclet.comments());
                    String skeleton =
                        skeleton(member.name(), member.nameHole(), member.bodyHoles(),
                            taclet.body());
                    if (reference == null) {
                        reference = skeleton;
                        referenceName = member.name();
                    } else if (!reference.equals(skeleton)) {
                        fail(member.name() + " is not an instance of the same skeleton as "
                            + referenceName + "; " + firstDivergence(reference, skeleton));
                    }
                }
            }));
        }
        return tests.stream();
    }

    @Test
    void everyMarkerInTheFileBelongsToTheSpec() {
        Map<String, String> expectedByName = new HashMap<>();
        for (Group group : SPEC) {
            for (Member member : group.members()) {
                assertNull(expectedByName.put(member.name(), annotation(group, member)),
                    "duplicate spec entry for " + member.name());
            }
        }
        List<String> markers = new ArrayList<>(markerLines());
        for (String expected : expectedByName.values()) {
            assertTrue(markers.remove(expected),
                "annotation declared in the spec is missing from the file: " + expected);
        }
        assertEquals(List.of(), markers,
            "annotations in the file without a corresponding spec entry in "
                + RuleGeneralizationTest.class.getSimpleName());
        assertEquals(97, expectedByName.size(), "spec member count");
    }

    @Test
    void checkerDetectsAnInjectedDivergence() {
        String add = skeleton("fooAddRule", "Add", List.of("+"), "x = a + b;");
        String sub = skeleton("fooSubRule", "Sub", List.of("-"), "x = a - b;");
        assertEquals(add, sub);
        String swapped = skeleton("fooMulRule", "Mul", List.of("*"), "x = b * a;");
        assertNotEquals(add, swapped);
    }
}
