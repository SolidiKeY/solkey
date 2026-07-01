/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.taclets;

import java.nio.file.Path;
import java.util.stream.Stream;

import org.key_project.solidity.proof.Proof;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.key_project.solidity.testutil.SolidityExampleTests.example;
import static org.key_project.solidity.testutil.SolidityExampleTests.loadAndProve;

/// User-visible showcase examples that simply load and close via automode:
///
/// - `functionBody/archive.key` — a function copying one field into another is inlined via
/// function-body expansion and the resulting field relation is proved.
/// - `functionBody/expFctBdy.key` — a call `res = expFctBdy(a, b)@C` is inlined (parameter
/// bindings, body and result assignment) and `res = 3` / field `f = a` is proved.
/// - `newVariable/newVariable.key` — a rule introduces a fresh program variable with `\new` and
/// executes two assignments one at a time through a context block, proving `q = 42` (exercises
/// ProgramContextAdder together with the fresh-variable mechanism).
public class SimpleExampleTests {

    @ParameterizedTest(name = "{0}")
    @MethodSource("examples")
    void exampleCloses(String name, Path file) throws Exception {
        Proof proof = loadAndProve(file);
        assertTrue(proof.closed(),
            () -> name + " should close; open goals: " + proof.openGoals().size());
    }

    static Stream<Arguments> examples() {
        return Stream.of(
            Arguments.of("archive", example("functionBody/archive.key")),
            Arguments.of("expFctBdy", example("functionBody/expFctBdy.key")),
            Arguments.of("newVariable", example("newVariable/newVariable.key")));
    }

    /// Per-statement execution of `expFctBdy` produces intermediate empty blocks; computing a
    /// node's name (as the API / tree view does) must not fail on those. Regression guard for
    /// NodeInfo.computeFirstStatement on an empty program block.
    @Test
    void expFctBdyNodeNamesDoNotThrow() throws Exception {
        Proof proof = loadAndProve(example("functionBody/expFctBdy.key"));
        assertTrue(proof.closed(), "expFctBdy expansion proof should close");
        for (var it = proof.root().subtreeIterator(); it.hasNext();) {
            it.next().name();
        }
    }
}
