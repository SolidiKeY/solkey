/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.taclets;

import java.io.IOException;
import java.util.List;
import java.util.stream.Stream;

import org.key_project.solidity.proof.Proof;
import org.key_project.solidity.testutil.SolidityExampleTests;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.key_project.solidity.testutil.SolidityExampleTests.proveTestSuiteFunction;

/// Exercises the user-visible pre-licenciate-paper taclet starters: the focused, one-rule-each
/// functions of `keyext.solidity.examples/TestSuite.sol`.
///
/// There are no `.key` problem files. The loader synthesizes the obligation for each function —
/// a call in a modality with postcondition `true` — and the `assert` statements in the body carry
/// the specification. The end-to-end functions (`test*`) run in [PaperTestExamplesTest].
public class TacletStarterExamplesTest {

    private static final String KNOWN_STUCK_PREFIX = "unprovable";

    @ParameterizedTest(name = "{0}")
    @MethodSource("examples")
    void tacletStarterExampleCloses(String function) throws Exception {
        Proof proof = proveTestSuiteFunction(function, 10000, SolidityExampleTests.KEEP_TIMEOUT);
        assertTrue(proof.closed(),
            () -> SolidityExampleTests.describeOpenGoals(function, proof));
    }

    static Stream<Arguments> examples() throws IOException {
        return SolidityExampleTests.testSuiteFunctions(
            name -> !name.startsWith("test") && !name.startsWith(KNOWN_STUCK_PREFIX));
    }

    /// The `recv[idx] = rhs` examples, which are the only ones the `indexWriteCapture` choice
    /// changes: each shape of the capture partition (complex receiver, non-simple index, both)
    /// crossed with each right-hand-side kind. The default choice is already covered by
    /// [#tacletStarterExampleCloses]; this pins the other one, so the two calculi cannot drift
    /// apart into one that closes and one that does not.
    private static final List<String> INDEX_WRITE_EXAMPLES = List.of(
        "indexWriteBothImpureMemRef",
        "indexWriteBothImpureMemToStorage",
        "indexWriteBothImpureMemoryValue",
        "indexWriteBothImpureStorageRef",
        "memoryIndexWriteMemRefImpureReceiver",
        "memoryIndexWriteNse",
        "memoryToStorageIndexImpureReceiver",
        "storageIndexWriteComplexReceiverCopySource",
        "storageIndexWriteNseChain",
        "storageIndexWriteRefSourceImpureIndex",
        "storageIndexWriteRootRefImpureReceiver",
        "storageIndexWriteStorageRefImpureReceiver",
        "storageMatrixNseIndex",
        "testIndexWriteReceiverReadsMutatedVar",
        "testMemoryEvaluationOrder",
        "testMemoryIndexWriteImpureIndexPrimitiveRhs",
        "testMemoryIndexWriteImpureIndexRefRhs",
        "testMemoryToStorageIndexCopyImpureIndex",
        "testNestedIndexWriteImpureReceiverAndIndex",
        "testStorageEvaluationOrder",
        "testStorageIndexWriteImpureIndexPrimitiveRhs",
        "testStorageIndexWriteImpureIndexRefRhs");

    @ParameterizedTest(name = "{1} {0}")
    @MethodSource("indexWriteExamples")
    void indexWriteExampleClosesUnderEitherCapture(String function, String choice)
            throws Exception {
        Proof proof = SolidityExampleTests.proveTestSuiteFunction(function, 30000,
            SolidityExampleTests.KEEP_TIMEOUT, List.of("indexWriteCapture:" + choice));
        assertTrue(proof.closed(),
            () -> SolidityExampleTests.describeOpenGoals(function + " (" + choice + ")", proof));
    }

    static Stream<Arguments> indexWriteExamples() throws IOException {
        List<String> declared = SolidityExampleTests
                .testSuiteFunctions(INDEX_WRITE_EXAMPLES::contains)
                .map(a -> (String) a.get()[0])
                .toList();
        assertEquals(INDEX_WRITE_EXAMPLES.size(), declared.size(),
            "every listed index-write example must still be a provable function of TestSuite.sol");
        return declared.stream()
                .flatMap(f -> Stream.of("receiverThenIndex", "allAtOnce")
                        .map(choice -> Arguments.of(f, choice)));
    }
}
