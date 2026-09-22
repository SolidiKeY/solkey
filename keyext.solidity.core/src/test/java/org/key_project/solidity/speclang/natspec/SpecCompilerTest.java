/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.speclang.natspec;

import java.util.List;
import java.util.Map;

import org.key_project.solidity.program.parser.SolidityOutline;
import org.key_project.solidity.program.parser.SolidityOutline.Parameter;
import org.key_project.solidity.program.parser.SolidityOutline.Span;
import org.key_project.solidity.program.parser.SolidityOutline.Variable;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Golden text for every construct of the specification language, against a hand-built
/// outline so no solc run is needed.
public class SpecCompilerTest {

    private static final SolidityOutline.Function DEPOSIT = new SolidityOutline.Function(
        "deposit", List.of(new Parameter("x", "uint256"), new Parameter("ok", "bool")),
        List.of(new Parameter("r", "uint256")), "payable", "", Span.NONE);

    private static final SolidityOutline.Contract C = new SolidityOutline.Contract("C", "",
        List.of(new Variable("owner", "address payable"), new Variable("n", "uint256"),
            new Variable("open", "bool"), new Variable("state", "enum C.State"),
            new Variable("balances", "mapping(address => uint256)"),
            new Variable("bidded", "mapping(address => bool)"),
            new Variable("bidders", "address[] storage ref"),
            new Variable("acc", "struct C.Account storage ref"),
            new Variable("grid", "mapping(uint256 => mapping(uint256 => uint256))")),
        Map.of("State", List.of("Open", "Closed")),
        Map.of("Account", List.of(new Variable("id", "uint256"), new Variable("live", "bool"))),
        List.of(DEPOSIT));

    private static final SpecCompiler COMPILER = new SpecCompiler(C, DEPOSIT);

    private static String invariant(String text) {
        return COMPILER.formula(text, SpecCompiler.Context.invariant(), "test");
    }

    private static String ensures(String text) {
        return COMPILER.formula(text,
            SpecCompiler.Context.ensures(SpecCompiler.parameterTypes(DEPOSIT)), "test");
    }

    @Test
    void stateVariablesMappingsArraysAndStructs() {
        assertEquals("(find<[int]>(s, cons1(C$n)) = 5)", invariant("n == 5"));
        assertEquals("(find<[bool]>(s, cons1(C$open)) = TRUE)", invariant("open"));
        assertEquals("(find<[int]>(s, cons2(C$balances, at(find<[int]>(s, cons1(C$owner))))) = 0)",
            invariant("balances[owner] == 0"));
        assertEquals("(find<[bool]>(s, cons2(C$bidded, at(msgSender))) = TRUE)",
            invariant("bidded[msg.sender]"));
        assertEquals("(find<[int]>(s, cons2(C$bidders, size)) > 0)",
            invariant("bidders.length > 0"));
        assertEquals("(find<[int]>(s, cons2(C$bidders, at(0))) = find<[int]>(s, cons1(C$owner)))",
            invariant("bidders[0] == owner"));
        assertEquals("(find<[int]>(s, cons2(C$acc, C$Account$id)) = 1)",
            invariant("acc.id == 1"));
        assertEquals("(find<[bool]>(s, cons2(C$acc, C$Account$live)) = TRUE)",
            invariant("acc.live"));
        assertEquals("(find<[int]>(s, cons3(C$grid, at(1), at(2))) = 3)",
            invariant("grid[1][2] == 3"));
    }

    @Test
    void enumsMessageNetAndThis() {
        assertEquals("(find<[int]>(s, cons1(C$state)) = 1)", invariant("state == State.Closed"));
        assertEquals("(selectSt<[int]>(n, at(find<[int]>(s, cons1(C$owner)))) = msgValue)",
            invariant("net(owner) == msg.value"));
        assertEquals("!(find<[int]>(s, cons1(C$owner)) = self)",
            invariant("owner != address(this)"));
        assertEquals(
            "(selectSt<[int]>(n, at(msgSender)) = neg(selectSt<[int]>(n, at(find<[int]>(s, cons1(C$owner))))))",
            invariant("net(msg.sender) == -net(owner)"));
    }

    @Test
    void connectivesQuantifiersAndArithmetic() {
        assertEquals(
            "((find<[int]>(s, cons1(C$n)) < 1) -> !((find<[bool]>(s, cons1(C$open)) = TRUE)))",
            invariant("n < 1 -> !open"));
        assertEquals(
            "(\\forall int a; ((selectSt<[int]>(n, at(a)) > 0) -> (find<[bool]>(s, cons2(C$bidded, at(a))) = TRUE)))",
            invariant("\\forall address a; net(a) > 0 -> bidded[a]"));
        assertEquals(
            "(\\exists int i; (((i >= 0) & (i < find<[int]>(s, cons2(C$bidders, size)))) & (find<[int]>(s, cons2(C$bidders, at(i))) = find<[int]>(s, cons1(C$owner)))))",
            invariant("\\exists uint i; i >= 0 && i < bidders.length && bidders[i] == owner"));
        assertEquals(
            "(div((find<[int]>(s, cons1(C$n)) * 2), 3) = mod(find<[int]>(s, cons1(C$n)), -4))",
            invariant("n * 2 / 3 == n % -4"));
        assertEquals("((find<[bool]>(s, cons1(C$open)) = TRUE) <-> true)",
            invariant("open == true"));
        assertEquals(
            "!((find<[bool]>(s, cons1(C$open)) = TRUE) <-> (find<[int]>(s, cons1(C$n)) = 0))",
            invariant("open != (n == 0)"));
    }

    @Test
    void parametersResultAndOldInEnsures() {
        assertEquals("(result = (x + 1))", ensures("\\result == x + 1"));
        assertEquals(
            "((ok = TRUE) -> (find<[int]>(storage, cons1(C$n)) = (find<[int]>(old, cons1(C$n)) + x)))",
            ensures("ok -> n == \\old(n) + x"));
        assertEquals(
            "(selectSt<[int]>(net, at(msgSender)) = selectSt<[int]>(oldNet, at(msgSender)))",
            ensures("net(msg.sender) == \\old(net(msg.sender))"));
    }

    @Test
    void mistakesAreReportedWithTheClause() {
        assertContains("unknown identifier nope", () -> invariant("nope == 1"));
        assertContains("\\old is only allowed in ensures", () -> invariant("\\old(n) == 1"));
        assertContains("\\result is only allowed in ensures", () -> invariant("\\result == 1"));
        assertContains("neither a mapping nor an array", () -> invariant("n[0] == 1"));
        assertContains("has no member Half", () -> invariant("state == State.Half"));
        assertContains("expected a boolean", () -> invariant("n"));
        assertContains("cannot be used as a value", () -> invariant("balances == 0"));
        assertContains("only msg.sender and msg.value", () -> invariant("msg.gas == 0"));
        assertContains("test: ", () -> invariant("n =="));
    }

    private static void assertContains(String expected, Runnable action) {
        var e = assertThrows(SpecException.class, action::run);
        assertTrue(e.getMessage().contains(expected), e.getMessage());
    }
}
