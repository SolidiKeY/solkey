/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.taclets;

import java.io.IOException;
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
import static org.junit.jupiter.api.Assumptions.assumeFalse;

/// Checks the contracts of `keyext.solidity.examples/contracts/`, the solidiKeY example
/// contracts with their specification carried in `@custom:key` natspec tags, and of
/// `keyext.solidity.examples/real-world/`, published contracts specified the same way, and of
/// `real-world-plain/`, the same programs with only their own `require`s and `assert`s.
///
/// Every `.sol` in the directories is enumerated and every function an obligation can be
/// generated for is checked, so a new contract joins the suite by being written — the contract
/// name is taken from the file name. The functions in [#KNOWN_OPEN] need an invariant
/// automode cannot establish, including quantified invariants and known storage tails, or, in
/// `real-world-plain/`, an assumption only the removed specification stated: there the
/// obligation is that the function never reverts. Each case must load and leave its proof open,
/// so an improvement that closes one turns the entry stale. The cases in [#TOO_SLOW] run out of
/// steps only after minutes, so they are skipped.
@Tag("solidityExamples")
public class ContractExamplesTest {

    private static final List<String> DIRECTORIES =
        List.of("contracts", "real-world", "real-world-plain",
            "real-world-plain/open", "real-world-plain/solidikey");

    private static final Set<String> KNOWN_OPEN = Set.of(
        "contracts/MultiAuction.placeOrIncreaseBid",
        "contracts/MultiAuction.withdraw",
        "contracts/MultiAuction.myTest",
        "contracts/ArrayCopyShapes.fixedToDynamicCopyTakesSourceLength",
        "contracts/ArrayCubeDFD.popPushAtDepth1ResetsRemovedElement",
        "contracts/ArrayCubeDFD.popPushAtDepth3ResetsRemovedElement",
        "contracts/ArrayCubeDFD.pushReturnAliasSharesWrites",
        "contracts/ArrayCubeDFD.storageAssignmentCopiesDeeply",
        "contracts/ArrayMatrixConversions.dynamicDynamicShrinkClearsReusedRow",
        "contracts/ArrayMatrixConversions.fixedRowsToDynamicInner",
        "contracts/ArrayMatrixDD.popPushAtDepth2ResetsRemovedElement",
        "contracts/ArrayMatrixDD.pushReturnAliasSharesWrites",
        "contracts/ArrayMatrixDF.pushReturnAliasSharesWrites",
        "contracts/FixedLengths.emptyPushFixedElementReadsZero",
        "contracts/FixedLengths.emptyPushReadsZero",
        "contracts/FixedLengths.fixedCopyIsIndependent",
        "real-world-plain/Account.withdraw",
        "real-world-plain/ArrayRemoveByShifting.remove",
        "real-world-plain/ArrayReplaceFromEnd.remove",
        "real-world-plain/DocsBallot.giveRightToVote",
        "real-world-plain/DocsBallot.delegate",
        "real-world-plain/DocsBallot.vote",
        "real-world-plain/DocsBallot.winningProposal",
        "real-world-plain/DocsBallot.winnerName",
        "real-world-plain/EtherGame.deposit",
        "real-world-plain/EtherGame.claimReward",
        "real-world-plain/EtherStore.withdraw",
        "real-world-plain/EtherWallet.withdraw",
        "real-world-plain/FunctionModifier.changeOwner",
        "real-world-plain/FunctionModifier.decrement",
        "real-world-plain/IterableMapping.getKeyAtIndex",
        "real-world-plain/IterableMapping.removeFromMapping",
        "real-world-plain/KingOfEther.claimThrone",
        "real-world-plain/KingOfEther.withdraw",
        "real-world-plain/Ownable.constructor",
        "real-world-plain/Ownable.renounceOwnership",
        "real-world-plain/Ownable.transferOwnership",
        "real-world-plain/OZCounters.decrement",
        "real-world-plain/OZMath.ceilDiv",
        "real-world-plain/OZNonces.useCheckedNonce",
        "real-world-plain/OZOwnable2Step.constructor",
        "real-world-plain/OZOwnable2Step.renounceOwnership",
        "real-world-plain/OZOwnable2Step.transferOwnership",
        "real-world-plain/OZOwnable2Step.acceptOwnership",
        "real-world-plain/OZPausable.normalProcess",
        "real-world-plain/OZPausable.drasticMeasure",
        "real-world-plain/OZPausable.pause",
        "real-world-plain/OZPausable.unpause",
        "real-world-plain/OZReentrancyGuard.callback",
        "real-world-plain/OZReentrancyGuard.countLocalRecursive",
        "real-world-plain/OZReentrancyGuard.guardedCheckEntered",
        "real-world-plain/OZReentrancyGuard.unguardedCheckNotEntered",
        "real-world-plain/Purchase.constructor",
        "real-world-plain/Purchase.abort",
        "real-world-plain/Purchase.confirmPurchase",
        "real-world-plain/Purchase.confirmReceived",
        "real-world-plain/Purchase.refundSeller",
        "real-world-plain/SendEther.sendViaTransfer",
        "real-world-plain/SendEther.sendViaSend",
        "real-world-plain/SendEther.sendViaCall",
        "real-world-plain/SimpleAuction.bid",
        "real-world-plain/SimpleAuction.auctionEnd",
        "real-world-plain/SolmateOwned.setOwner",
        "real-world-plain/TimeLock.withdraw",
        "real-world-plain/WETH9.withdraw",
        "real-world-plain/WETH9.transferFrom",
        "real-world-plain/open/ArrayRemoveByShifting.remove",
        "real-world-plain/open/DocsBallot.winningProposal",
        "real-world-plain/open/DocsBallot.winnerName",
        "real-world-plain/solidikey/Escrow.constructor",
        "real-world-plain/solidikey/Escrow.placeInEscrow",
        "real-world-plain/solidikey/Escrow.releaseEscrow",
        "real-world-plain/solidikey/Escrow.withdrawFromEscrow",
        "real-world-plain/solidikey/MultiAuction.placeOrIncreaseBid",
        "real-world-plain/solidikey/MultiAuction.withdraw",
        "real-world-plain/solidikey/MultiAuction.closeAuction",
        "real-world-plain/solidikey/PiggyBank.addMoney",
        "real-world-plain/solidikey/PiggyBank.breakPiggyBank");

    private static final Set<String> TOO_SLOW = Set.of("real-world-plain/DocsBallot.delegate");

    @ParameterizedTest(name = "{0}/{1}.{2}")
    @MethodSource("examples")
    void contractExampleCloses(String directory, String contract, String function)
            throws Exception {
        assumeFalse(TOO_SLOW.contains(directory + "/" + contract + "." + function));
        Proof proof =
            SolidityExampleTests.proveWithEstimatedBudget(directory, contract, function);
        String name = directory + "/" + contract + "." + function;
        if (KNOWN_OPEN.contains(name)) {
            assertFalse(proof.closed(), name
                + " closed; remove it from KNOWN_OPEN");
        } else {
            assertTrue(proof.closed(),
                () -> SolidityExampleTests.describeOpenGoals(contract + "." + function, proof)
                        + SolidityExampleTests.budgetHint(directory, contract));
        }
    }

    static Stream<Arguments> examples() throws IOException {
        Stream.Builder<Arguments> args = Stream.builder();
        for (String directory : DIRECTORIES) {
            for (String contract : SolidityExampleTests.solContracts(directory)) {
                SolidityProblemSynthesizer
                        .provableFunctions(contractSource(directory, contract), contract)
                        .stream()
                        .sorted()
                        .forEach(function -> args.add(Arguments.of(directory, contract, function)));
            }
        }
        return args.build();
    }

    private static Path contractSource(String directory, String contract) {
        return SolidityExampleTests.example(directory + "/" + contract + ".sol");
    }
}
