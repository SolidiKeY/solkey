/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.taclets;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import org.key_project.solidity.control.KeYEnvironment;
import org.key_project.solidity.proof.Proof;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertTrue;

/// Loads the `keyext.solidity.examples/mainFeatures/` showcase problems — one per `test*` function
/// in `PaperTest.sol`. Each problem calls the function in a diamond modality with postcondition
/// `true`; the in-body `assert(a == b)` statements carry the proof obligations (per
/// `docs/require-assert.md`).
public class PaperTestExamplesTest {

    @ParameterizedTest(name = "{0}")
    @MethodSource("examples")
    void paperTestExampleCloses(String name, Path file) throws Exception {
        Proof proof = loadAndRun(file);
        assertTrue(proof.closed(),
            () -> name + " should close; open goals: " + proof.openGoals().size()
                + "; first open goal: " + proof.openGoals().head().sequent());
    }

    private static Proof loadAndRun(Path file) throws Exception {
        KeYEnvironment env = KeYEnvironment.load(file);
        Proof proof = env.getLoadedProof();
        var strategySettings = proof.getSettings().getStrategySettings();
        strategySettings.setMaxSteps(50000);
        strategySettings.setTimeout(30000);
        env.getProofControl().startAndWaitForAutoMode(proof);
        return proof;
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
            example("testMemoryUintArrayAuxiliaryCases.key"),
            example("testMemoryTokenArrayAuxiliaryCases.key"));
    }

    private static Arguments example(String name) {
        Path path = examplesDir().resolve(name);
        assertTrue(Files.exists(path), "example must exist: " + path.toAbsolutePath());
        return Arguments.of(name, path);
    }

    private static Path examplesDir() {
        Path path = Path.of("keyext.solidity.examples/mainFeatures");
        return Files.exists(path) ? path : Path.of("../keyext.solidity.examples/mainFeatures");
    }

    // --- Deferred: missing taclet support (see docs/taclets-implementation.md) ---

    @Test
    @Disabled("testStorageArrayReadWrite: array index after push — bounds check uses pre-push storage, cannot auto-close")
    void testStorageArrayReadWrite_disabled() {
    }

    @Test
    @Disabled("testStorageArrayPushPop: Token(42) struct constructor syntax not supported by parser")
    void testStorageArrayPushPop_disabled() {
    }

    @Test
    @Disabled("testStoragePushLvaluePrimitive: values.push() = 77 — push-returns-lvalue not implemented")
    void testStoragePushLvaluePrimitive_disabled() {
    }

    @Test
    @Disabled("testStoragePushReturnAlias: Token storage t = tokens.push() — push-returns-alias not implemented")
    void testStoragePushReturnAlias_disabled() {
    }

    @Test
    @Disabled("testStorageStructDeleteSkipsMappingMember: delete struct preserves mapping members — no such taclet")
    void testStorageStructDeleteSkipsMappingMember_disabled() {
    }

    @Test
    @Disabled("testStorageEvaluationOrder: a[++i] = ++i — the ++i taclets work (see the memory cases), "
        + "but this example first needs a.push(100); statement sequences, which hit a pre-existing "
        + "void-call statement-suffix reconstruction bug (same family as the other disabled push tests)")
    void testStorageEvaluationOrder_disabled() {
    }

    // --- Deferred: require taclet support for push().field, complex-receiver push, and
    // mapping-element struct copy (see docs/taclets-implementation.md) ---

    @Test
    @Disabled("testStoragePushFieldLvalue: tokens.push().value = 11 — no push-field-lvalue taclet")
    void testStoragePushFieldLvalue_disabled() {
    }

    @Test
    @Disabled("testStorageNestedPushReturnAlias: people.push().account alias — no nested push-return taclet")
    void testStorageNestedPushReturnAlias_disabled() {
    }

    @Test
    @Disabled("testStoragePushLvalueCopiesStorageSource: tokens.push() = storageRef — no push-lvalue-copy taclet")
    void testStoragePushLvalueCopiesStorageSource_disabled() {
    }

    @Test
    @Disabled("testStorageComplexReceiverEmptyPush: bucket.tokens.push() — no complex-receiver push taclet")
    void testStorageComplexReceiverEmptyPush_disabled() {
    }

    @Test
    @Disabled("testStorageComplexReceiverPushLvalueCopy: bucket.tokens.push() = tokRef — no complex-receiver push-lvalue taclet")
    void testStorageComplexReceiverPushLvalueCopy_disabled() {
    }

    @Test
    @Disabled("testStorageComplexReceiverPushFieldLvalue: bucket.tokens.push().value = v — no complex-receiver push-field-lvalue taclet")
    void testStorageComplexReceiverPushFieldLvalue_disabled() {
    }

    @Test
    @Disabled("testStorageMapStructCopy: accountMap[2] = accountMap[1] — no mapping-element struct copy taclet")
    void testStorageMapStructCopy_disabled() {
    }
}
