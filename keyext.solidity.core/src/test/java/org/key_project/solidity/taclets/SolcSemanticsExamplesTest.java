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

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertTrue;

/// Runs the ports of the solc compiler's own semantic tests
/// (`keyext.solidity.examples/solc/`), which cross-check the calculus against an externally
/// authored description of Solidity semantics.
///
/// Every `.sol` beside the directory is enumerated and every function an obligation can be
/// generated for is proved, so a new example joins the suite by being written — the contract
/// name is taken from the file name.
///
/// Seven examples started out red and were fixed by the rule and parser changes the port
/// prompted; they stay here as regression tests. The gaps they found are listed in
/// `keyext.solidity.examples/solc/README.md`.
public class SolcSemanticsExamplesTest {

    private static final String DIRECTORY = "solc";

    /// Examples that do not close yet, as `contract.function`. Each entry needs a matching
    /// entry under "Known open" in `keyext.solidity.examples/solc/README.md`; removing the
    /// proof gap should remove the entry here in the same change.
    private static final Set<String> KNOWN_OPEN = Set.of(
        "SolcArrays.pushThenPopRestoresLength");

    @ParameterizedTest(name = "{0}.{1}")
    @MethodSource("examples")
    void solcSemanticsExampleCloses(String contract, String function) throws Exception {
        Path sol = contractSource(contract);
        Proof proof = SolidityExampleTests.prove(
            SolidityExampleTests.load(sol, contract, function), 50000, 30000);
        assertTrue(proof.closed(),
            () -> contract + "." + function + " should close; open goals: "
                + proof.openGoals().size()
                + "; first open goal: " + proof.openGoals().head().sequent());
    }

    static Stream<Arguments> examples() throws IOException {
        Stream.Builder<Arguments> args = Stream.builder();
        for (String contract : contracts()) {
            SolidityProblemSynthesizer.provableFunctions(contractSource(contract), contract)
                    .stream()
                    .sorted()
                    .filter(function -> !KNOWN_OPEN.contains(contract + "." + function))
                    .forEach(function -> args.add(Arguments.of(contract, function)));
        }
        return args.build();
    }

    private static List<String> contracts() throws IOException {
        try (Stream<Path> files = Files.list(SolidityExampleTests.examplesDir(DIRECTORY))) {
            return files.map(p -> p.getFileName().toString())
                    .filter(name -> name.endsWith(".sol"))
                    .map(name -> name.substring(0, name.length() - ".sol".length()))
                    .sorted()
                    .toList();
        }
    }

    private static Path contractSource(String contract) {
        return SolidityExampleTests.example(DIRECTORY + "/" + contract + ".sol");
    }
}
