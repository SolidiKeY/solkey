/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.proof.init;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.key_project.solidity.program.parser.SolidityOutline;
import org.key_project.solidity.testutil.SolidityExampleTests;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

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
    void aFunctionReturningAnUnnamedValueIsRefusedWithItsReason(@TempDir Path dir)
            throws IOException {
        Path file = dir.resolve("R.sol");
        Files.writeString(file, """
                // SPDX-License-Identifier: GPL-2.0-only
                pragma solidity ^0.8.0;
                contract R {
                    function unnamed() public pure returns (uint) { return 1; }
                }""");

        var e = assertThrows(IllegalArgumentException.class,
            () -> SolidityProblemSynthesizer.resolve(file, spec("R", "unnamed")));

        assertTrue(e.getMessage().contains("cannot be proved"), e.getMessage());
        assertTrue(e.getMessage().contains("returns a value"), e.getMessage());
    }

    @Test
    void aNamedReturnBecomesTheResultVariable() throws IOException {
        Path file = SolidityExampleTests.example("functionBody/C.sol");

        String text = SolidityProblemSynthesizer.problemText(file, spec("C", "expFctBdy"));

        assertTrue(text.contains("int result;"), text);
        assertTrue(text.contains("\\<{ result = expFctBdy(x, y)@C; }\\>(true)"), text);
    }

    @Test
    void anUnspecifiedFunctionKeepsThePlainObligation() throws IOException {
        Path file = SolidityExampleTests.testSuite();

        String text = SolidityProblemSynthesizer.problemText(file,
            spec(SolidityExampleTests.TEST_SUITE_CONTRACT, "testSimpleAssert"));

        assertEquals("""
                \\programSource "%s";

                \\problem {
                    \\<{ testSimpleAssert()@%s; }\\>(true)
                }
                """.formatted(file.toAbsolutePath(), SolidityExampleTests.TEST_SUITE_CONTRACT),
            text);
    }

    @Test
    void aSpecifiedFunctionGetsTheInvariantTacletAndTheLedgerObligation() throws IOException {
        Path file = SolidityExampleTests.example("contracts/Escrow.sol");

        String text = SolidityProblemSynthesizer.problemText(file, spec("Escrow", "placeInEscrow"));

        assertEquals(
            """
                    \\programSource "%s";

                    \\rules {
                        insertCInv {
                            \\schemaVar \\term Struct s, n;
                            \\find(CInv(s, n))
                            \\replacewith(
                                // sender != receiver :
                                !(find<[int]>(s, cons1(Escrow$sender)) = find<[int]>(s, cons1(Escrow$receiver)))
                                // amountInEscrow == net(sender) + net(receiver) :
                                & (find<[int]>(s, cons1(Escrow$amountInEscrow)) = (selectSt<[int]>(n, at(find<[int]>(s, cons1(Escrow$sender)))) + selectSt<[int]>(n, at(find<[int]>(s, cons1(Escrow$receiver))))))
                                // state != State.AwaitingDeposit || net(sender) == 0 :
                                & (!(find<[int]>(s, cons1(Escrow$state)) = 0) | (selectSt<[int]>(n, at(find<[int]>(s, cons1(Escrow$sender)))) = 0))
                                // state == State.DepositPlaced || amountInEscrow == 0 :
                                & ((find<[int]>(s, cons1(Escrow$state)) = 1) | (find<[int]>(s, cons1(Escrow$amountInEscrow)) = 0)))
                            \\heuristics(simplify)
                        };
                    }

                    \\problem {
                        // msg.value >= 0 :
                        geq(msgValue, 0)
                        // msg.sender == sender && state == State.AwaitingDeposit && msg.value > 0 :
                        & (((msgSender = find<[int]>(storage, cons1(Escrow$sender))) & (find<[int]>(storage, cons1(Escrow$state)) = 0)) & (msgValue > 0))
                        & CInv(storage, net) ->
                        {net := storeSt(net, at(msgSender), selectSt<[int]>(net, at(msgSender)) + msgValue)
                         || selfBalance := selfBalance + msgValue}
                        \\[{ placeInEscrow()@Escrow; }\\]
                            (CInv(storage, net)
                             // net(sender) == msg.value && state == State.DepositPlaced :
                             & ((selectSt<[int]>(net, at(find<[int]>(storage, cons1(Escrow$sender)))) = msgValue) & (find<[int]>(storage, cons1(Escrow$state)) = 1)))
                    }
                    """
                    .formatted(file.toAbsolutePath()),
            text);
    }

    @Test
    void oldAndQuantifiersDeclareWhatTheyNeed() throws IOException {
        Path file = SolidityExampleTests.example("contracts/MultiAuction.sol");

        String text = SolidityProblemSynthesizer.problemText(file,
            spec("MultiAuction", "placeOrIncreaseBid"));

        assertTrue(text.contains("    Struct old;\n    Struct oldNet;\n"), text);
        assertTrue(text.contains("{old := storage || oldNet := net\n     || net := storeSt("),
            text);
        assertTrue(text.contains("\\schemaVar \\variables int hb;\n"), text);
        assertTrue(text.contains("\\varcond(\\noFreeVarIn(s), \\noFreeVarIn(n))"), text);
        assertTrue(text.contains("find<[int]>(old, cons2(MultiAuction$balances, at(msgSender)))"),
            text);
        assertTrue(text.contains("// msg.value >= 0 :\n    geq(msgValue, 0)"), text);

        String nonPayable = SolidityProblemSynthesizer.problemText(file,
            spec("MultiAuction", "withdraw"));
        assertTrue(nonPayable.contains("// msg.value == 0 :\n    msgValue = 0"), nonPayable);
    }

    @Test
    void aBrokenClauseNamesTheFunctionAndTheClause(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("B.sol");
        Files.writeString(file, """
                // SPDX-License-Identifier: GPL-2.0-only
                pragma solidity ^0.8.0;
                contract B {
                    uint n;
                    /// @custom:key ensures nope == 1
                    function f() public { n = 1; }
                }""");

        var e = assertThrows(IllegalArgumentException.class,
            () -> SolidityProblemSynthesizer.problemText(file, spec("B", "f")));

        assertTrue(e.getMessage().contains("B.f ensures"), e.getMessage());
        assertTrue(e.getMessage().contains("unknown identifier nope"), e.getMessage());
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
