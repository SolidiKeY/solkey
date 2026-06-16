/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.gui;

import java.nio.file.Files;
import java.nio.file.Path;

import org.key_project.solidity.common.Services;
import org.key_project.solidity.control.KeYEnvironment;
import org.key_project.solidity.parser.KeYIO;
import org.key_project.solidity.proof.Proof;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

/// Regression test for the argument-sort check in `SFunction`: an ill-typed term such as
/// `1 + TRUE` (`add(int, bool)`) must be rejected at construction, while well-typed terms build.
public class IllTypedTermTest {

    static {
        System.setProperty("java.awt.headless", "true");
    }

    private static Path example() {
        Path p = Path.of("keyext.solidity.examples/functionBody/archive.key");
        return Files.exists(p) ? p
                : Path.of("../keyext.solidity.examples/functionBody/archive.key");
    }

    @Test
    void illTypedAdditionIsRejected() throws Exception {
        KeYEnvironment<?> env = KeYEnvironment.load(example());
        Proof proof = env.getLoadedProof();
        Services services = proof.getServices();

        assertDoesNotThrow(() -> new KeYIO(services).parseExpression("1 + 1"),
            "well-typed integer addition should build");
        assertThrows(Exception.class, () -> new KeYIO(services).parseExpression("1 + TRUE"),
            "add(int, bool) is ill-typed and must be rejected");
        assertThrows(Exception.class, () -> new KeYIO(services).parseExpression("1 + TRUE = 1"),
            "a formula containing an ill-typed subterm must be rejected");
    }
}
