/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.taclets;

import java.net.URL;
import java.nio.file.Path;

import org.key_project.solidity.control.KeYEnvironment;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/// Checks that loading malformed problems/taclets fails with clear, developer-oriented messages
/// (located where possible, and pointing at the likely cause such as a missing ProgramSVSort).
public class ErrorMessageTest {

    /// Concatenated messages of the whole cause chain of a throwable.
    private static String causeChainMessages(Throwable t) {
        StringBuilder sb = new StringBuilder();
        for (Throwable cur = t; cur != null && cur != cur.getCause(); cur = cur.getCause()) {
            sb.append(cur.getMessage()).append('\n');
        }
        return sb.toString();
    }

    private static String loadExpectingFailure(String resource) {
        URL res = ErrorMessageTest.class.getClassLoader().getResource(resource);
        assertNotNull(res, "test resource must exist: " + resource);
        try {
            KeYEnvironment.load(Path.of(res.toURI()));
            fail("loading " + resource + " should have failed");
            return ""; // unreachable
        } catch (Exception e) {
            return causeChainMessages(e);
        }
    }

    @Test
    void unknownProgramSVSortGivesHelpfulMessage() {
        String msg = loadExpectingFailure(
            "org/key_project/solidity/errors/unknownProgramSVSort.key");
        assertTrue(msg.contains("NoSuchSort"),
            "should name the offending sort, was:\n" + msg);
        assertTrue(msg.contains("Known sorts"),
            "should list the known program SV sorts, was:\n" + msg);
        assertTrue(msg.contains("FunctionBody") && msg.contains("FieldReference"),
            "the known-sorts list should contain registered sorts, was:\n" + msg);
        assertTrue(msg.contains("ProgramSVSort"),
            "should point the developer at ProgramSVSort, was:\n" + msg);
    }

    @Test
    void unknownIdentifierGivesLocatedMessage() {
        String msg = loadExpectingFailure(
            "org/key_project/solidity/errors/unknownIdentifier.key");
        assertTrue(msg.contains("doesNotExist"),
            "should name the offending identifier, was:\n" + msg);
        assertTrue(msg.contains("out of scope"),
            "should explain it is out of scope, was:\n" + msg);
        assertTrue(msg.matches("(?s).*line \\d+.*column \\d+.*"),
            "should pinpoint a location, was:\n" + msg);
    }
}
