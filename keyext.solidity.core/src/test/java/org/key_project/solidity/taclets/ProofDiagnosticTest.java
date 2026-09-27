/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.taclets;

import org.key_project.solidity.proof.Proof;

import org.junit.jupiter.api.Test;

import static org.key_project.solidity.testutil.SolidityExampleTests.describeOpenGoals;
import static org.key_project.solidity.testutil.SolidityExampleTests.proveTestSuiteFunction;

/// Diagnostic test: runs testStorageWriteAndRead with a small step limit and prints open goals.
/// Not meant to close — used to inspect proof state.
public class ProofDiagnosticTest {

    @Test
    void diagnoseOpenGoals() throws Exception {
        Proof proof = proveTestSuiteFunction("testStorageWriteAndRead", 30, -1);
        System.out.println(describeOpenGoals("testStorageWriteAndRead", proof,
            Integer.MAX_VALUE, Integer.MAX_VALUE));
    }
}
