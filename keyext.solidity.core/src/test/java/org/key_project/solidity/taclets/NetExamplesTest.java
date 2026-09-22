/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.taclets;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import org.key_project.solidity.proof.Proof;
import org.key_project.solidity.testutil.SolidityExampleTests;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Runs the `net` ledger examples — the `.key` problems of `keyext.solidity.examples/net/`
/// covering `msg.sender`/`msg.value`, `.transfer` under both `transferSemantics` choices, and
/// the contract-invariant-at-transfer obligations (`CInv` via a per-problem `insertCInv`
/// taclet, `docs/net.md`). They stay `.key`-based because the synthesized `.sol` obligations
/// cannot carry an invariant, an antecedent (e.g. on `selfBalance`), or the
/// `transferSemantics` taclet option; the program bodies themselves load from the `.sol`
/// beside each problem via `\programSource`. The negative twins under the
/// `org/key_project/solidity/examples/open/` test resources pin the diamond funding
/// obligation by staying open.
@Tag("solidityExamples")
public class NetExamplesTest {

    private static final String OPEN_EXAMPLES_RESOURCE = "org/key_project/solidity/examples/open";

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

    @ParameterizedTest(name = "{0}")
    @MethodSource("openExamples")
    void unfundedDiamondStaysOpen(String name, Path key) throws Exception {
        Proof proof = SolidityExampleTests.loadAndProve(key, 2000, 10000);
        assertFalse(proof.closed(),
            () -> name + " must stay open: a diamond transfer without funding owes"
                + " 0 <= v & v <= selfBalance");
    }

    static Stream<Arguments> openExamples() throws Exception {
        URL resource = NetExamplesTest.class.getClassLoader().getResource(OPEN_EXAMPLES_RESOURCE);
        assertNotNull(resource, "missing resource dir: " + OPEN_EXAMPLES_RESOURCE);
        try (Stream<Path> files = Files.list(Path.of(resource.toURI()))) {
            return files.filter(p -> p.getFileName().toString().endsWith(".key"))
                    .sorted()
                    .map(p -> Arguments.of(p.getFileName().toString(), p))
                    .toList()
                    .stream();
        }
    }
}
