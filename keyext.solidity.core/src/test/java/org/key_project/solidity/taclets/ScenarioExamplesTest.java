/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.taclets;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import org.key_project.solidity.proof.Proof;
import org.key_project.solidity.proof.init.SolidityProblemSynthesizer;
import org.key_project.solidity.testutil.SolidityExampleTests;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertTrue;

/// Runs the scenario example contracts beside `TestSuite.sol` — the SolidityCalculus course
/// examples ported into the supported fragment (`keyext.solidity.examples/README.md`,
/// "Scenario contracts"). Like [PaperTestExamplesTest], the functions are enumerated from the
/// sources, so a new scenario function joins the suite by being written.
public class ScenarioExamplesTest {

    private static final List<String> CONTRACTS =
        List.of("PiggyBank", "Escrow", "Auction", "Casino");

    @ParameterizedTest(name = "{0}.{1}")
    @MethodSource("examples")
    void scenarioExampleCloses(String contract, String function) throws Exception {
        Path sol = SolidityExampleTests.example(contract + ".sol");
        Proof proof =
            SolidityExampleTests.prove(SolidityExampleTests.load(sol, contract, function),
                50000, 30000);
        assertTrue(proof.closed(),
            () -> contract + "." + function + " should close; open goals: "
                + proof.openGoals().size()
                + "; first open goal: " + proof.openGoals().head().sequent());
    }

    static Stream<Arguments> examples() throws IOException {
        Stream.Builder<Arguments> args = Stream.builder();
        for (String contract : CONTRACTS) {
            Path sol = SolidityExampleTests.example(contract + ".sol");
            SolidityProblemSynthesizer.provableFunctions(sol, contract)
                    .stream()
                    .sorted()
                    .forEach(function -> args.add(Arguments.of(contract, function)));
        }
        return args.build();
    }
}
