/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.taclets;

import java.io.IOException;
import java.util.Set;
import java.util.stream.Stream;

import org.key_project.solidity.proof.Proof;
import org.key_project.solidity.testutil.SolidityExampleTests;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.key_project.solidity.testutil.SolidityExampleTests.proveTestSuiteFunction;

/// Exercises the user-visible pre-licenciate-paper taclet starters: the focused, one-rule-each
/// functions of `keyext.solidity.examples/TestSuite.sol`.
///
/// There are no `.key` problem files. The loader synthesizes the obligation for each function —
/// a call in a modality with postcondition `true` — and the `assert` statements in the body carry
/// the specification. The end-to-end functions (`test*`) run in [PaperTestExamplesTest].
public class TacletStarterExamplesTest {

    private static final Set<String> SYMBOLIC_TAIL_OPENS = Set.of(
        "storagePushReturnPrimitiveBindComplexReceiver",
        "storagePushCompoundTarget",
        "storagePushDivModTarget",
        "storagePushIncrementTarget",
        "storagePushCompoundSigned",
        "storagePushPopCallReceiver",
        "storageArrayMethodReferencesComplexReceiver",
        "storageArrayMethodReferencesComposedCallReceiver",
        "storagePushReturnPrimitiveBind");

    private static final String KNOWN_STUCK_PREFIX = "unprovable";

    @ParameterizedTest(name = "{0}")
    @MethodSource("examples")
    void tacletStarterExampleHasExpectedStatus(String function) throws Exception {
        Proof proof = proveTestSuiteFunction(function, 10000, SolidityExampleTests.KEEP_TIMEOUT);
        if (SYMBOLIC_TAIL_OPENS.contains(function)) {
            assertFalse(proof.closed(), function
                + " unexpectedly closed without known storage-tail contents");
        } else {
            assertTrue(proof.closed(),
                () -> SolidityExampleTests.describeOpenGoals(function, proof));
        }
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = { "unprovableInvariantBreakPostcondition",
        "unprovableInvariantForContinueUpdate", "unprovableInvariantConditionSideEffect",
        "unprovableInvariantConditionStorageSideEffect" })
    void loopInvariantRejectsFalsePostcondition(String function) throws Exception {
        Proof proof = proveTestSuiteFunction(function, 10000, SolidityExampleTests.KEEP_TIMEOUT);
        assertFalse(proof.closed(), function + " unexpectedly proved a false assertion");
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = { "unprovableVariantConditionLowersBodyRestores" })
    void loopVariantRejectsNonTermination(String function) throws Exception {
        Proof proof = proveTestSuiteFunction(function, 10000, SolidityExampleTests.KEEP_TIMEOUT);
        assertFalse(proof.closed(), function + " unexpectedly proved a loop that never ends");
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = { "unprovableStorageShortCircuitMutation" })
    void shortCircuitRejectsSkippedMutation(String function) throws Exception {
        Proof proof = proveTestSuiteFunction(function, 10000, SolidityExampleTests.KEEP_TIMEOUT);
        assertFalse(proof.closed(), function + " unexpectedly proved a skipped storage write");
    }

    static Stream<Arguments> examples() throws IOException {
        return SolidityExampleTests.testSuiteFunctions(
            name -> !name.startsWith("test") && !name.startsWith(KNOWN_STUCK_PREFIX));
    }
}
