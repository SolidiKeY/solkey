/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.taclets;

import java.nio.file.Path;
import java.util.stream.Stream;

import org.key_project.solidity.proof.Proof;
import org.key_project.solidity.testutil.SolidityExampleTests;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.key_project.solidity.testutil.SolidityExampleTests.loadAndProve;

/// Loads the `keyext.solidity.examples/mainFeatures/` showcase problems — one per `test*` function
/// in `PaperTest.sol`. Each problem calls the function in a diamond modality with postcondition
/// `true`; the in-body `assert(a == b)` statements carry the proof obligations (per
/// `docs/require-assert.md`).
public class PaperTestExamplesTest {

    @ParameterizedTest(name = "{0}")
    @MethodSource("examples")
    void paperTestExampleCloses(String name, Path file) throws Exception {
        Proof proof = loadAndProve(file, 50000, 30000);
        assertTrue(proof.closed(),
            () -> name + " should close; open goals: " + proof.openGoals().size()
                + "; first open goal: " + proof.openGoals().head().sequent());
    }

    static Stream<Arguments> examples() {
        return Stream.of(
            example("testStorageWriteAndRead.key"),
            example("testNestedStorageWrites.key"),
            example("testStorageFieldDeepCopy.key"),
            example("testStorageRootDeepCopy.key"),
            example("testStorageAliases.key"),
            example("testStorageMapReadWriteAndDelete.key"),
            example("testStorageDeletePaperCase.key"),
            example("testStorageStructDeleteSkipsMappingMember.key"),
            example("testMemoryAliasing.key"),
            example("testMemoryFieldShallowCopy.key"),
            example("testMemoryRootAlias.key"),
            example("testMemoryDeleteAlias.key"),
            example("testMemoryRootDeleteRebindsOnlyLocal.key"),
            example("testMemoryDeleteIdentityFieldFreshensSlot.key"),
            example("testMemoryDeletePrimitiveField.key"),
            example("testStorageToMemoryCopyRoot.key"),
            example("testStorageToMemoryCopyField.key"),
            example("testStorageToMemoryCopyComplexPath.key"),
            example("testMemoryToStorageCopyRoot.key"),
            example("testMemoryToStorageCopyField.key"),
            example("testMemoryToStorageCopyComplexSource.key"),
            example("testMemoryToStorageCopyComplexTarget.key"),
            example("testMemoryTokenArrayAuxiliaryCases.key"),
            example("testMemoryUintArrayAuxiliaryCases.key"),
            example("testMemoryUintArrayPredecrement.key"),
            example("testMemoryUintArrayPostincrement.key"),
            example("testMemoryUintArrayPostdecrement.key"),
            example("testStoragePushLvaluePrimitive.key"),
            example("testStoragePushReturnAlias.key"),
            example("testStorageComplexReceiverPushLvalueCopy.key"),
            example("testStoragePushLvalueCopiesStorageSource.key"),
            example("testStorageComplexReceiverEmptyPush.key"),
            example("testStoragePushFieldLvalue.key"),
            example("testStorageComplexReceiverPushFieldLvalue.key"),
            example("testStorageNestedPushReturnAlias.key"),
            example("testStorageArrayReadWrite.key"),
            example("testStorageMapStructCopy.key"),
            example("testStorageEvaluationOrder.key"));
    }

    private static Arguments example(String name) {
        return Arguments.of(name, SolidityExampleTests.example("mainFeatures/" + name));
    }

    // --- Deferred: missing taclet support (see docs/taclets-implementation.md) ---

    @Test
    @Disabled("testStorageArrayPushPop: Token(42) struct constructor syntax not supported by parser")
    void testStorageArrayPushPop_disabled() {
    }
}
