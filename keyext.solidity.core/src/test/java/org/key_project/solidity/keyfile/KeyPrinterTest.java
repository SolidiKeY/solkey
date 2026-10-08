/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.keyfile;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.key_project.solidity.keyfile.Key.and;
import static org.key_project.solidity.keyfile.Key.assign;
import static org.key_project.solidity.keyfile.Key.constant;
import static org.key_project.solidity.keyfile.Key.eq;
import static org.key_project.solidity.keyfile.Key.implies;
import static org.key_project.solidity.keyfile.Key.labeled;
import static org.key_project.solidity.keyfile.Key.not;
import static org.key_project.solidity.keyfile.Key.num;
import static org.key_project.solidity.keyfile.Key.or;
import static org.key_project.solidity.keyfile.Key.plus;
import static org.key_project.solidity.keyfile.Key.predicate;
import static org.key_project.solidity.keyfile.Key.update;

public class KeyPrinterTest {

    private static final KeyFormula P = predicate("p", constant("x"));
    private static final KeyFormula Q = predicate("q", constant("x"));

    @Test
    void everyOperandIsDelimited() {
        assertEquals("((p(x) | q(x)) & !(x = 1))",
            KeyPrinter.print(and(or(P, Q), not(eq(constant("x"), num(1))))));
        assertEquals("(p(x) & ({x := (x + 1)}q(x)))",
            KeyPrinter.print(and(P, update(List.of(assign("x", plus(constant("x"), num(1)))),
                Q))));
        assertEquals("(p(x) -> (q(x) -> p(x)))", KeyPrinter.print(implies(P, implies(Q, P))));
    }

    @Test
    void aMultiLineClauseStaysOneComment() {
        String text = KeyPrinter.print(new KeyProblem(Path.of("/a/A.sol"), List.of(), List.of(),
            List.of(), List.of(), and(labeled("a &&\n  b", P), Q)));

        assertTrue(text.contains("\\problem {\n    // a && b :\n    p(x)\n    & q(x)\n}\n"), text);
    }

    @Test
    void malformedNamesAreRejectedWhenBuilt() {
        assertThrows(IllegalArgumentException.class, () -> constant("a b"));
        assertThrows(IllegalArgumentException.class, () -> predicate("wf("));
        assertThrows(IllegalArgumentException.class, () -> num("1e3"));
        assertThrows(IllegalArgumentException.class, () -> new KeyProblem.Variable(null, "x"));
        assertThrows(IllegalArgumentException.class, () -> new KeyProblem(Path.of("/a/A.sol"),
            List.of("not an option"), List.of(), List.of(), List.of(), Key.TRUE));
    }
}
