/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.taclets;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import org.key_project.solidity.proof.Proof;
import org.key_project.solidity.testutil.SolidityExampleTests;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertTrue;

/// Runs the `net` ledger examples — the `.key` problems of `keyext.solidity.examples/net/`
/// covering `msg.sender`/`msg.value`, `.transfer` under both `transferSemantics` choices, and
/// the contract-invariant-at-transfer obligations (`CInv` via a per-problem `insertCInv`
/// taclet, `docs/net.md`). They stay `.key`-based because the synthesized `.sol` obligations
/// cannot carry an invariant, an antecedent (e.g. on `selfBalance`), or the
/// `transferSemantics` taclet option; the program bodies themselves load from the `.sol`
/// beside each problem via `\programSource`.
public class NetExamplesTest {

    @ParameterizedTest(name = "{0}")
    @MethodSource("examples")
    void netExampleCloses(String name) throws Exception {
        Path key = SolidityExampleTests.example("net/" + name);
        Proof proof = SolidityExampleTests.loadAndProve(key, 50000, 30000);
        assertTrue(proof.closed(),
            () -> name + " should close; open goals: " + proof.openGoals().size()
                + "; first open goal: " + proof.openGoals().head().sequent());
    }

    static Stream<Arguments> examples() throws IOException {
        try (Stream<Path> files = Files.list(SolidityExampleTests.examplesDir("net"))) {
            return files.filter(p -> p.getFileName().toString().endsWith(".key"))
                    .map(p -> p.getFileName().toString())
                    .sorted()
                    .map(Arguments::of)
                    .toList()
                    .stream();
        }
    }
}
