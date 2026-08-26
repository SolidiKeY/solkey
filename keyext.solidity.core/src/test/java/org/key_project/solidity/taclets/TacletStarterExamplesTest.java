/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.taclets;

import java.nio.file.Path;
import java.util.stream.Stream;

import org.key_project.solidity.proof.Proof;
import org.key_project.solidity.testutil.SolidityExampleTests;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.key_project.solidity.testutil.SolidityExampleTests.loadAndProve;

/// Exercises the user-visible pre-licenciate-paper taclet starters. These examples live in the
/// filesystem examples module, not in the test resources scanned by [RulesTest].
public class TacletStarterExamplesTest {

    @ParameterizedTest(name = "{0}")
    @MethodSource("examples")
    void tacletStarterExampleCloses(String name, Path file) throws Exception {
        Proof proof = loadAndProve(file, 10000, SolidityExampleTests.KEEP_TIMEOUT);
        assertTrue(proof.closed(),
            () -> name + " should close; open goals: " + proof.openGoals().size()
                + "; first open goal: " + proof.openGoals().head().sequent());
    }

    static Stream<Arguments> examples() {
        return Stream.of(
            example("storage-root-read-write.key"),
            example("storage-root-copy-source.key"),
            example("storage-root-copy-struct.key"),
            example("storage-field-copy-struct.key"),
            example("storage-root-multiple-writes.key"),
            example("storage-root-disjoint.key"),
            example("storage-field-global-age.key"),
            example("storage-field-write-read.key"),
            example("storage-field-disjoint-roots.key"),
            example("storage-field-decomposition.key"),
            example("storage-field-deep-write-read.key"),
            example("storage-field-deep-value.key"),
            example("storage-index-root-array.key"),
            example("storage-index-root-mapping.key"),
            example("storage-index-multiple-writes.key"),
            example("storage-field-disjoint-fields.key"),
            example("storage-index-decomposition.key"),
            example("storage-matrix-write-read.key"),
            example("storage-index-array-out-of-bounds-box.key"),
            example("storage-alias-write-balance.key"),
            example("storage-alias-write-token.key"),
            example("storage-alias-rebind-original.key"),
            example("storage-alias-rebind-alias.key"),
            example("storage-local-decl-skip.key"),
            example("storage-field-read-bind-local.key"),
            example("storage-field-read-store-root.key"),
            example("storage-root-add-assign.key"),
            example("storage-field-add-assign.key"),
            example("storage-index-add-assign.key"),
            example("storage-field-deep-add-assign.key"),
            example("storage-root-sub-assign.key"),
            example("storage-field-sub-assign.key"),
            example("storage-index-sub-assign.key"),
            example("storage-field-deep-sub-assign.key"),
            example("storage-root-mul-assign.key"),
            example("storage-field-mul-assign.key"),
            example("storage-index-mul-assign.key"),
            example("storage-field-deep-mul-assign.key"),
            example("storage-root-div-assign.key"),
            example("storage-field-div-assign.key"),
            example("storage-index-div-assign.key"),
            example("storage-field-deep-div-assign.key"),
            example("storage-root-mod-assign.key"),
            example("storage-field-mod-assign.key"),
            example("storage-index-mod-assign.key"),
            example("storage-field-deep-mod-assign.key"),
            example("storage-root-preincrement.key"),
            example("storage-root-postincrement.key"),
            example("storage-root-predecrement.key"),
            example("storage-root-preincrement-assign.key"),
            example("storage-root-postincrement-assign.key"),
            example("storage-root-postdecrement-assign.key"),
            example("storage-field-preincrement.key"),
            example("storage-field-postincrement.key"),
            example("storage-field-predecrement.key"),
            example("storage-field-postdecrement.key"),
            example("storage-field-preincrement-assign.key"),
            example("storage-field-postincrement-assign.key"),
            example("storage-field-predecrement-assign.key"),
            example("storage-field-postdecrement-assign.key"),
            example("storage-index-preincrement.key"),
            example("storage-index-postincrement.key"),
            example("storage-index-predecrement.key"),
            example("storage-index-postdecrement.key"),
            example("storage-index-preincrement-assign.key"),
            example("storage-index-postincrement-assign.key"),
            example("storage-index-predecrement-assign.key"),
            example("storage-index-postdecrement-assign.key"),
            example("storage-deep-field-preincrement.key"),
            example("storage-deep-field-postincrement.key"),
            example("storage-root-delete.key"),
            example("storage-field-delete.key"),
            example("storage-root-delete-struct.key"),
            example("storage-index-delete.key"),
            example("storage-index-delete-mapping-struct.key"),
            example("storage-index-delete-mapping-bool.key"),
            example("storage-push-value.key"),
            example("storage-push-nonsimple-arg.key"),
            example("storage-push-empty.key"),
            example("storage-push-return-assign.key"),
            example("storage-push-local-bind.key"),
            example("storage-pop-nonempty.key"),
            example("storage-pop-empty-box.key"),
            example("storage-pop-after-push.key"),
            example("storage-index-decompose-after-push.key"),
            example("storage-index-copysource-after-push.key"),
            example("memory-decl-fresh.key"),
            example("memory-root-delete-fresh.key"),
            example("memory-decl-default.key"),
            example("memory-root-alias.key"),
            example("memory-field-alias.key"),
            example("memory-deep-field.key"),
            example("memory-field-reference-assign.key"),
            example("memory-delete.key"),
            example("memory-array-index.key"),
            example("memory-struct-array-index.key"),
            example("storage-to-memory.key"),
            example("memory-to-storage.key"),
            example("addition-simple.key"),
            example("addition-storage-read.key"),
            example("addition-storage-write.key"),
            example("addition-both-storage.key"),
            example("subtraction-simple.key"),
            example("subtraction-storage-read.key"),
            example("multiplication-simple.key"),
            example("power-simple.key"),
            example("division-simple.key"),
            example("modulo-simple.key"),
            example("less-than-simple.key"),
            example("greater-than-simple.key"),
            example("less-equal-simple.key"),
            example("greater-equal-simple.key"),
            example("not-equal-simple.key"),
            example("logical-and-simple.key"),
            example("logical-or-simple.key"),
            example("logical-not-simple.key"),
            example("unary-minus-simple.key"),
            example("net-manual-update.key"),
            example("net-msg-value.key"),
            example("net-transfer-simple.key"),
            example("net-transfer-capture-argument.key"),
            example("net-transfer-capture-receiver.key"));
    }

    private static Arguments example(String name) {
        return Arguments.of(name, SolidityExampleTests.example("taclets/" + name));
    }
}
