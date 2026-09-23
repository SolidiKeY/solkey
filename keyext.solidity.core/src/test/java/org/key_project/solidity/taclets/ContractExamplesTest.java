/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.taclets;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.key_project.solidity.proof.Proof;
import org.key_project.solidity.proof.init.SolidityProblemSynthesizer;
import org.key_project.solidity.testutil.SolidityExampleTests;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Proves the contracts of `keyext.solidity.examples/contracts/`, the solidiKeY example
/// contracts with their specification carried in `@custom:key` natspec tags, and of
/// `keyext.solidity.examples/real-world/`, published contracts specified the same way.
///
/// Every `.sol` in the directories is enumerated and every function an obligation can be
/// generated for is proved, so a new contract joins the suite by being written — the contract
/// name is taken from the file name. The functions in [#KNOWN_OPEN] state an invariant
/// automode cannot establish yet (MultiAuction's quantified one); they are checked to load and
/// to leave the proof open, so a calculus improvement that closes one turns the entry stale.
@Tag("solidityExamples")
public class ContractExamplesTest {

    private static final List<String> DIRECTORIES = List.of("contracts", "real-world");

    private static final Set<String> KNOWN_OPEN =
        Set.of("MultiAuction.placeOrIncreaseBid", "MultiAuction.withdraw", "MultiAuction.myTest");

    @ParameterizedTest(name = "{0}/{1}.{2}")
    @MethodSource("examples")
    void contractExampleCloses(String directory, String contract, String function)
            throws Exception {
        Path sol = contractSource(directory, contract);
        Proof proof = SolidityExampleTests.prove(
            SolidityExampleTests.load(sol, contract, function), 20000, 60000);
        if (KNOWN_OPEN.contains(contract + "." + function)) {
            assertFalse(proof.closed(), contract + "." + function
                + " closed; remove it from KNOWN_OPEN");
        } else {
            assertTrue(proof.closed(),
                () -> SolidityExampleTests.describeOpenGoals(contract + "." + function, proof));
        }
    }

    static Stream<Arguments> examples() throws IOException {
        Stream.Builder<Arguments> args = Stream.builder();
        for (String directory : DIRECTORIES) {
            for (String contract : contracts(directory)) {
                SolidityProblemSynthesizer
                        .provableFunctions(contractSource(directory, contract), contract)
                        .stream()
                        .sorted()
                        .forEach(function -> args.add(Arguments.of(directory, contract, function)));
            }
        }
        return args.build();
    }

    private static List<String> contracts(String directory) throws IOException {
        try (Stream<Path> files = Files.list(SolidityExampleTests.examplesDir(directory))) {
            return files.map(p -> p.getFileName().toString())
                    .filter(name -> name.endsWith(".sol"))
                    .map(name -> name.substring(0, name.length() - ".sol".length()))
                    .sorted()
                    .toList();
        }
    }

    private static Path contractSource(String directory, String contract) {
        return SolidityExampleTests.example(directory + "/" + contract + ".sol");
    }
}
