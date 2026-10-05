/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.proof.init;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.key_project.solidity.control.ProofSession;
import org.key_project.solidity.control.SolidityVerifier;
import org.key_project.solidity.program.parser.SolidityOutline;
import org.key_project.solidity.program.parser.SoliditySources;
import org.key_project.solidity.testutil.SolidityExampleTests;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

    private static Path stringParameter(Path dir) throws IOException {
        Path file = dir.resolve("S.sol");
        Files.writeString(file, """
                // SPDX-License-Identifier: GPL-2.0-only
                pragma solidity ^0.8.0;
                contract S {
                    function label(string memory text) public {}
                }""");
        return file;
    }

    @Test
    void aParameterWithoutAKeySortIsRefusedWithItsReason(@TempDir Path dir) throws IOException {
        Path file = stringParameter(dir);

        var e = assertThrows(IllegalArgumentException.class,
            () -> SolidityProblemSynthesizer.resolve(file, spec("S", "label")));

        assertTrue(e.getMessage().contains("S.label"), e.getMessage());
        assertTrue(e.getMessage().contains("cannot be proved"), e.getMessage());
        assertTrue(e.getMessage().contains("string"), e.getMessage());
    }

    @Test
    void anAddressParameterIsDeclaredAsAnInt() throws IOException {
        Path file = SolidityExampleTests.example("net/PiggyBankNet.sol");

        String text =
            SolidityProblemSynthesizer.problemText(file, spec("PiggyBankNet", "payToPlus"));

        assertTrue(text.contains("    int a;\n    int x;\n"), text);
        assertTrue(text.contains("payToPlus(a, x)@PiggyBankNet;"), text);
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
        assertFalse(text.contains("\\schemaVar \\variables"), text);
        assertTrue(text.contains("(\\exists int hb; (\\forall int a; "), text);
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
    void theOutlineOverloadAgreesWithThePathOne(@TempDir Path dir) throws IOException {
        Path file = SolidityExampleTests.testSuite();
        SolidityOutline outline = SolidityOutline.of(file);
        SolidityProblemSpec requested =
            spec(SolidityExampleTests.TEST_SUITE_CONTRACT, "testSimpleAssert");

        assertEquals(SolidityProblemSynthesizer.resolve(file, requested),
            SolidityProblemSynthesizer.resolve(file, outline, requested));

        Path unsupported = stringParameter(dir);
        SolidityOutline unsupportedOutline = SolidityOutline.of(unsupported);
        var e = assertThrows(IllegalArgumentException.class, () -> SolidityProblemSynthesizer
                .resolve(unsupported, unsupportedOutline, spec("S", "label")));
        assertTrue(e.getMessage().contains("cannot be proved"), e.getMessage());
    }

    @Test
    void aSourceRegisteredInMemoryIsProvedWithoutAFile() throws IOException {
        Path file = Path.of("/nonexistent-solkey-dir/InMemory.sol");
        SoliditySources.register(file, """
                // SPDX-License-Identifier: GPL-2.0-only
                pragma solidity ^0.8.0;
                contract InMemory {
                    function holds() public pure {
                        uint8 x = 1;
                        assert(x == 1);
                    }
                    function fails() public pure {
                        uint8 x = 1;
                        assert(x == 2);
                    }
                }""");

        assertEquals(java.util.List.of("holds", "fails"), SolidityVerifier.functions(file, null));
        var holds = SolidityVerifier.verify(file, spec(null, "holds"), 10000, -1, 3);
        var fails = SolidityVerifier.verify(file, spec(null, "fails"), 10000, -1, 3);

        assertTrue(holds.closed(), String.valueOf(holds));
        assertFalse(fails.closed(), String.valueOf(fails));
        assertEquals(null, fails.error());
        assertFalse(fails.openGoals().isEmpty());
        assertEquals(null, holds.proof());

        var saved = SolidityVerifier.verify(file, spec(null, "holds"), 10000, -1, 3, true);
        assertTrue(saved.proof() != null && saved.proof().contains("\\proof"),
            String.valueOf(saved.proof()));
    }

    private static Path inMemory(String name) {
        Path file = Path.of("/nonexistent-solkey-dir/" + name + ".sol");
        SoliditySources.register(file, """
                // SPDX-License-Identifier: GPL-2.0-only
                pragma solidity ^0.8.0;
                contract %s {
                    function holds() public pure {
                        uint8 x = 1;
                        assert(x == 1);
                    }
                    function fails() public pure {
                        uint8 x = 1;
                        assert(x == 2);
                    }
                }""".formatted(name));
        return file;
    }

    @Test
    void aMistypedTacletOptionIsRejectedWithTheKnownChoices() {
        Path file = inMemory("Typo");
        var outcome = SolidityVerifier.verify(file,
            new SolidityProblemSpec(null, "holds",
                java.util.List.of("transferSemantics:withCalback")),
            ProofSession.Limits.defaults(), 3, false);

        assertFalse(outcome.closed());
        assertTrue(String.valueOf(outcome.error()).contains("known choices"), outcome.error());
    }

    @Test
    void strategySettingsAreValidatedAndApplied() {
        Path file = inMemory("Strategy");
        var model = SolidityVerifier.verify(file, spec(null, "holds"),
            new ProofSession.Limits(10000, -1, java.util.Map.of(
                org.key_project.solidity.strategy.StrategyProperties.NON_LIN_ARITH_OPTIONS_KEY,
                org.key_project.solidity.strategy.StrategyProperties.NON_LIN_ARITH_COMPLETION)),
            3, false);
        var bad = SolidityVerifier.verify(file, spec(null, "holds"),
            new ProofSession.Limits(10000, -1,
                java.util.Map.of("SPLITTING_OPTIONS_KEY", "SOMETIMES")),
            3, false);

        assertTrue(model.closed(), String.valueOf(model));
        assertTrue(model.summary().nodes() > 1, String.valueOf(model.summary()));
        assertTrue(String.valueOf(bad.error()).contains("no strategy setting"), bad.error());
    }

    @Test
    void theGeneratedProblemIsShown() throws IOException {
        assertTrue(SolidityVerifier.problem(inMemory("Problem"), spec(null, "holds"))
                .contains("\\problem"));
    }

    @Test
    void aSessionIsWalkedPrunedSteppedAndReplayed(@TempDir Path dir) throws Exception {
        Path file = inMemory("Session");
        ProofSession session =
            ProofSession.start(file, spec(null, "fails"), ProofSession.Limits.defaults());
        var tree = session.tree();
        var open = tree.stream().filter(e -> e.state().equals("open")).findFirst().orElseThrow();
        assertEquals(-1, tree.getFirst().parent());
        assertTrue(session.node(open.serial(), true).openGoal());

        var root = tree.getFirst().serial();
        var pruned = session.prune(root);
        assertEquals(1, pruned.nodes());
        String sequent = session.node(root, true).sequent();
        var rules = session.rulesAt(root, sequent.indexOf("fails"));
        var complete = rules.rules().stream()
                .filter(r -> !r.sequentWide() && r.missing().isEmpty()).findFirst()
                .orElseThrow(() -> new AssertionError(sequent + "\n" + rules));
        assertTrue(session.apply(root, complete.index(), java.util.Map.of()).nodes() > 1);
        var stepped = session.tree().stream().filter(e -> e.state().equals("open")).findFirst()
                .orElseThrow();
        assertFalse(session.runAuto(stepped.serial()).closed());

        ProofSession holds =
            ProofSession.start(file, spec(null, "holds"), ProofSession.Limits.defaults());
        var closedRoot = holds.tree().getFirst().serial();
        var refused = assertThrows(IllegalArgumentException.class, () -> holds.prune(closedRoot));
        assertTrue(refused.getMessage().contains("closed branch"), refused.getMessage());
        var goal = session.tree().stream().filter(e -> e.state().equals("open")).findFirst()
                .orElseThrow();
        assertThrows(IllegalArgumentException.class, () -> session.prune(goal.serial()));
        assertTrue(holds.summary().closed());
        Path sol = dir.resolve("Session.sol");
        Files.writeString(sol, SoliditySources.read(file));
        Path proof = dir.resolve("Session.holds.proof");
        Files.writeString(proof, holds.save().replace("\"" + file + "\"", "\"Session.sol\""));
        ProofSession replayed = ProofSession.load(proof, ProofSession.Limits.defaults(), false);
        assertTrue(replayed.summary().closed());
        assertTrue(replayed.replayErrors().isEmpty(), String.valueOf(replayed.replayErrors()));
    }
}
