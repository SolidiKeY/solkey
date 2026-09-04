/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.proof.init;

import java.io.IOException;
import java.nio.file.Path;

import org.key_project.solidity.program.parser.SolidityOutline;
import org.key_project.solidity.testutil.SolidityExampleTests;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Checks what [SolidityProblemSynthesizer#resolve] refuses. A function the outline reports as
/// unprovable has to be rejected here with its reason: it would otherwise reach
/// [SolidityProblemSynthesizer#problemText], which renders a parameter without a `.key` sort as
/// `null a` and fails in the KeY parser with an unrelated message.
public class SolidityProblemSynthesizerTest {

    private static SolidityProblemSpec spec(String contract, String function) {
        return new SolidityProblemSpec(contract, function);
    }

    @Test
    void aParameterWithoutAKeySortIsRefusedWithItsReason() {
        Path file = SolidityExampleTests.example("net/PiggyBankNet.sol");

        var e = assertThrows(IllegalArgumentException.class,
            () -> SolidityProblemSynthesizer.resolve(file, spec("PiggyBankNet", "payTo")));

        assertTrue(e.getMessage().contains("PiggyBankNet.payTo"), e.getMessage());
        assertTrue(e.getMessage().contains("cannot be proved"), e.getMessage());
        assertTrue(e.getMessage().contains("address payable"), e.getMessage());
    }

    @Test
    void aFunctionReturningAValueIsRefusedWithItsReason() {
        Path file = SolidityExampleTests.example("functionBody/C.sol");

        var e = assertThrows(IllegalArgumentException.class,
            () -> SolidityProblemSynthesizer.resolve(file, spec("C", "expFctBdy")));

        assertTrue(e.getMessage().contains("cannot be proved"), e.getMessage());
        assertTrue(e.getMessage().contains("returns a value"), e.getMessage());
    }

    @Test
    void anUnknownFunctionStillListsTheCandidates() {
        Path file = SolidityExampleTests.testSuite();

        var e = assertThrows(IllegalArgumentException.class, () -> SolidityProblemSynthesizer
                .resolve(file, spec(SolidityExampleTests.TEST_SUITE_CONTRACT, "noSuchFunction")));

        assertTrue(e.getMessage().contains("has no public function noSuchFunction"),
            e.getMessage());
        assertTrue(e.getMessage().contains("testSimpleAssert"), e.getMessage());
    }

    @Test
    void aProvableFunctionResolvesToItself() throws IOException {
        Path file = SolidityExampleTests.testSuite();
        String contract = SolidityExampleTests.TEST_SUITE_CONTRACT;

        assertEquals(spec(contract, "testSimpleAssert"),
            SolidityProblemSynthesizer.resolve(file, spec(contract, "testSimpleAssert")));
        assertEquals(spec(contract, "testSimpleAssert"),
            SolidityProblemSynthesizer.resolve(file, spec(null, "testSimpleAssert")));
    }

    /// The overload the GUI uses, so it does not fork solc a second time for a file it has already
    /// read, has to agree with the one that reads the file itself.
    @Test
    void theOutlineOverloadAgreesWithThePathOne() throws IOException {
        Path file = SolidityExampleTests.testSuite();
        SolidityOutline outline = SolidityOutline.of(file);
        SolidityProblemSpec requested =
            spec(SolidityExampleTests.TEST_SUITE_CONTRACT, "testSimpleAssert");

        assertEquals(SolidityProblemSynthesizer.resolve(file, requested),
            SolidityProblemSynthesizer.resolve(file, outline, requested));

        Path piggy = SolidityExampleTests.example("net/PiggyBankNet.sol");
        SolidityOutline piggyOutline = SolidityOutline.of(piggy);
        var e = assertThrows(IllegalArgumentException.class, () -> SolidityProblemSynthesizer
                .resolve(piggy, piggyOutline, spec("PiggyBankNet", "payTo")));
        assertTrue(e.getMessage().contains("cannot be proved"), e.getMessage());
    }
}
