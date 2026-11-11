/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.taclets;

import java.io.IOException;
import java.util.Set;
import java.util.stream.Stream;

import org.key_project.solidity.proof.Proof;
import org.key_project.solidity.testutil.SolidityExampleTests;

import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.key_project.solidity.testutil.SolidityExampleTests.proveTacletFunction;

/// Exercises the user-visible pre-licenciate-paper taclet starters: the focused, one-rule-each
/// functions of `keyext.solidity.examples/taclets/`.
///
/// There are no `.key` problem files. The loader synthesizes the obligation for each function —
/// a call in a modality with postcondition `true` — and the `assert` statements in the body carry
/// the specification. The end-to-end functions (`test*`) run in [PaperTestExamplesTest].
public class TacletStarterExamplesTest {

    private static final int SHARDS = 8;

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

    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    abstract static class Shard {
        private final int index;

        Shard(int index) {
            this.index = index;
        }

        Stream<Arguments> examples() throws IOException {
            return SolidityExampleTests.shard(SolidityExampleTests.tacletFunctions(
                name -> !name.startsWith("test") && !name.startsWith(KNOWN_STUCK_PREFIX)),
                index, SHARDS);
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("examples")
        void tacletStarterExampleHasExpectedStatus(String function) throws Exception {
            Proof proof =
                proveTacletFunction(function, 10000, SolidityExampleTests.KEEP_TIMEOUT);
            if (SYMBOLIC_TAIL_OPENS.contains(function)) {
                assertFalse(proof.closed(), function
                    + " unexpectedly closed without known storage-tail contents");
            } else {
                assertTrue(proof.closed(),
                    () -> SolidityExampleTests.describeOpenGoals(function, proof));
            }
        }
    }

    static class Shard0 extends Shard {
        Shard0() {
            super(0);
        }
    }

    static class Shard1 extends Shard {
        Shard1() {
            super(1);
        }
    }

    static class Shard2 extends Shard {
        Shard2() {
            super(2);
        }
    }

    static class Shard3 extends Shard {
        Shard3() {
            super(3);
        }
    }

    static class Shard4 extends Shard {
        Shard4() {
            super(4);
        }
    }

    static class Shard5 extends Shard {
        Shard5() {
            super(5);
        }
    }

    static class Shard6 extends Shard {
        Shard6() {
            super(6);
        }
    }

    static class Shard7 extends Shard {
        Shard7() {
            super(7);
        }
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = { "unprovableInvariantBreakPostcondition",
        "unprovableInvariantForContinueUpdate", "unprovableInvariantConditionSideEffect",
        "unprovableInvariantConditionStorageSideEffect" })
    void loopInvariantRejectsFalsePostcondition(String function) throws Exception {
        Proof proof = proveTacletFunction(function, 10000, SolidityExampleTests.KEEP_TIMEOUT);
        assertFalse(proof.closed(), function + " unexpectedly proved a false assertion");
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = { "unprovableVariantConditionLowersBodyRestores" })
    void loopVariantRejectsNonTermination(String function) throws Exception {
        Proof proof = proveTacletFunction(function, 10000, SolidityExampleTests.KEEP_TIMEOUT);
        assertFalse(proof.closed(), function + " unexpectedly proved a loop that never ends");
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = { "unprovableStorageShortCircuitMutation" })
    void shortCircuitRejectsSkippedMutation(String function) throws Exception {
        Proof proof = proveTacletFunction(function, 10000, SolidityExampleTests.KEEP_TIMEOUT);
        assertFalse(proof.closed(), function + " unexpectedly proved a skipped storage write");
    }
}
