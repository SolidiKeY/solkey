/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.speclang.natspec;

import java.util.ArrayList;
import java.util.List;

import org.key_project.solidity.parser.SolSpecParser;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SpecParserTest {

    /// The tree with each operator applied, fully parenthesized, so precedence is visible.
    private static String shape(SolSpecParser.ExprContext node) {
        return switch (node) {
            case SolSpecParser.ParensContext p -> shape(p.expr());
            case SolSpecParser.UnaryContext u -> "(" + u.op.getText() + shape(u.expr()) + ")";
            case SolSpecParser.QuantifierContext q -> "(" + q.q.getText() + " "
                + q.sort().getText() + " " + q.var.getText() + "; " + shape(q.expr()) + ")";
            case SolSpecParser.IndexContext i -> shape(i.expr(0)) + "[" + shape(i.expr(1)) + "]";
            case SolSpecParser.MemberContext m -> shape(m.expr()) + "." + m.IDENT().getText();
            case SolSpecParser.NetContext n -> "net(" + shape(n.expr()) + ")";
            case SolSpecParser.OldContext o -> "\\old(" + shape(o.expr()) + ")";
            case SolSpecParser.CastContext c -> shape(c.expr());
            default -> {
                if (node.getChildCount() == 3
                        && node.getChild(0) instanceof SolSpecParser.ExprContext left
                        && node.getChild(2) instanceof SolSpecParser.ExprContext right) {
                    yield "(" + shape(left) + " " + node.getChild(1).getText() + " "
                        + shape(right) + ")";
                }
                yield node.getText();
            }
        };
    }

    private static String shape(String source) {
        return shape(SpecParser.parse(source));
    }

    @Test
    void precedenceFollowsSolidity() {
        assertEquals("(((a == (b + (c * 2))) && (d < 3)) || (!e))",
            shape("a == b + c * 2 && d < 3 || !e"));
        assertEquals("((a - b) - c)", shape("a - b - c"));
        assertEquals("((-a) + b)", shape("-a + b"));
    }

    @Test
    void implicationIsRightAssociativeAndLooserThanOr() {
        assertEquals("((a || b) -> (c -> d))", shape("a || b -> c -> d"));
        assertEquals("(a <-> (b -> c))", shape("a <-> b -> c"));
        assertInstanceOf(SolSpecParser.ImplContext.class, SpecParser.parse("a -> b -> c"));
    }

    @Test
    void quantifierBodyExtendsToTheRightUnlessParenthesized() {
        assertEquals("(\\forall address a; ((net(a) > 0) && q))",
            shape("\\forall address a; net(a) > 0 && q"));
        assertEquals("((\\exists uint i; (i < n)) <-> flag)",
            shape("(\\exists uint i; i < n) <-> flag"));
        assertEquals("(x -> (\\forall uint i; (i >= 0)))",
            shape("x -> \\forall uint i; i >= 0"));
    }

    @Test
    void postfixAndKeywords() {
        assertEquals("s.m[k]", shape("s.m[k]"));
        assertEquals("net(msg.sender)", shape("net(msg.sender)"));
        assertEquals("(\\result == \\old(arr.length))", shape("\\result == \\old(arr.length)"));
        assertEquals("(owner != this)", shape("owner != address(this)"));
        assertEquals("(-net(a))", shape("-net(a)"));
        assertInstanceOf(SolSpecParser.BoolLitContext.class, SpecParser.parse("true"));
    }

    @Test
    void helpersFindOldAndQuantifiers() {
        SolSpecParser.ExprContext tree =
            SpecParser.parse("\\old(n) == 1 -> \\forall address a; \\exists uint i; a == i");
        assertTrue(SpecParser.usesOld(tree));
        assertFalse(SpecParser.usesOld(SpecParser.parse("n == 1")));
        List<String> bound = new ArrayList<>();
        SpecParser.forEachQuantifier(tree,
            q -> bound.add(q.sort().getText() + " " + q.var.getText()));
        assertEquals(List.of("address a", "uint i"), bound);
    }

    @Test
    void errorsNameTheExpression() {
        var trailing = assertThrows(SpecException.class, () -> SpecParser.parse("a == 1 b"));
        assertTrue(trailing.getMessage().contains("'b'"), trailing.getMessage());
        assertTrue(trailing.getMessage().contains("a == 1 b"), trailing.getMessage());

        var keyword = assertThrows(SpecException.class, () -> SpecParser.parse("\\fresh(a)"));
        assertTrue(keyword.getMessage().contains("\\fresh(a)"), keyword.getMessage());

        var character = assertThrows(SpecException.class, () -> SpecParser.parse("a ~ b"));
        assertTrue(character.getMessage().contains("~"), character.getMessage());

        assertThrows(SpecException.class, () -> SpecParser.parse("(a == 1"));
        assertThrows(SpecException.class, () -> SpecParser.parse(""));
    }
}
