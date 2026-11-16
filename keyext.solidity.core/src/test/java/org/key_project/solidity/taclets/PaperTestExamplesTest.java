/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.taclets;

import java.io.IOException;
import java.util.stream.Stream;

import org.key_project.solidity.proof.Proof;
import org.key_project.solidity.testutil.SolidityExampleTests;

import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.key_project.solidity.testutil.SolidityExampleTests.proveTacletFunction;

public class PaperTestExamplesTest {

    private static final int SHARDS = 3;

    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    abstract static class Shard {
        private final int index;

        Shard(int index) {
            this.index = index;
        }

        Stream<Arguments> examples() throws IOException {
            return SolidityExampleTests.shard(
                SolidityExampleTests.tacletFunctions(name -> name.startsWith("test")), index,
                SHARDS);
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("examples")
        void paperTestExampleCloses(String function) throws Exception {
            Proof proof = proveTacletFunction(function, 50000, 30000);
            assertTrue(proof.closed(),
                () -> SolidityExampleTests.describeOpenGoals(function, proof));
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
}
