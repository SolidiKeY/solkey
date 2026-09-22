/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.speclang.natspec;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class KeyNatspecTest {

    @Test
    void repeatedTagsBecomeOneClauseEach() {
        KeyNatspec spec = KeyNatspec.of("""
                @custom:key requires a == 1
                @custom:key requires b == 2
                @custom:key ensures c == 3""");

        assertEquals(List.of("a == 1", "b == 2"), spec.requires());
        assertEquals(List.of("c == 3"), spec.ensures());
        assertTrue(spec.isSpecified());
        assertFalse(spec.box());
    }

    @Test
    void untaggedLinesContinueTheClauseAndOtherTagsEndIt() {
        KeyNatspec spec = KeyNatspec.of("""
                @notice deposits
                @custom:key invariant a == 1
                   && b == 2
                @dev not a clause
                @custom:key box""");

        assertEquals(List.of("a == 1 && b == 2"), spec.invariants());
        assertTrue(spec.box());
    }

    @Test
    void directivesWithoutAnExpressionAreBareWords() {
        assertTrue(KeyNatspec.of("@custom:key skip").skip());
        assertFalse(KeyNatspec.of("@custom:key skip").isSpecified());
        assertEquals(KeyNatspec.EMPTY, KeyNatspec.of("@notice nothing for KeY"));
        assertEquals(KeyNatspec.EMPTY, KeyNatspec.of(""));
        assertEquals(KeyNatspec.EMPTY, KeyNatspec.of(null));
    }

    @Test
    void aTagQuotedInProseIsNotAClause() {
        KeyNatspec spec = KeyNatspec.of("""
                Functions tagged `@custom:key box` assume their requires.
                @custom:key box""");

        assertEquals(1, spec.clauses().size());
        assertTrue(spec.box());
        assertEquals(KeyNatspec.EMPTY,
            KeyNatspec.of("see `@custom:key invariant` in the README"));
    }

    @Test
    void assignableIsAcceptedAndIgnored() {
        KeyNatspec spec = KeyNatspec.of("@custom:key assignable balances[msg.sender], net(a)");

        assertFalse(spec.isSpecified());
        assertEquals(KeyNatspec.Kind.ASSIGNABLE, spec.clauses().get(0).kind());
    }

    @Test
    void malformedClausesAreRejectedWithTheDirective() {
        var unknown = assertThrows(SpecException.class,
            () -> KeyNatspec.of("@custom:key only_if a == 1"));
        assertTrue(unknown.getMessage().contains("only_if"), unknown.getMessage());

        var empty = assertThrows(SpecException.class, () -> KeyNatspec.of("@custom:key ensures"));
        assertTrue(empty.getMessage().contains("needs an expression"), empty.getMessage());

        var argument = assertThrows(SpecException.class,
            () -> KeyNatspec.of("@custom:key box please"));
        assertTrue(argument.getMessage().contains("takes no argument"), argument.getMessage());
    }
}
