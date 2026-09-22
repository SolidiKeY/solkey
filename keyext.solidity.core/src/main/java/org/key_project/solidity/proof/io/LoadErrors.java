/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.proof.io;

/// Renders a load failure the way a user can act on it.
///
/// [ProblemLoaderException] and the exceptions solc raises are wrappers: the innermost cause
/// carries the location-annotated parser/converter message, while the outer one is generic. Both
/// the CLI and the GUI unwrap the same way, so the same failure reads the same in both.
public final class LoadErrors {

    private LoadErrors() {}

    /// The innermost cause of a throwable, whose message is usually the most specific.
    public static Throwable rootCause(Throwable t) {
        Throwable cur = t;
        while (cur.getCause() != null && cur.getCause() != cur) {
            cur = cur.getCause();
        }
        return cur;
    }

    /// A message for [#rootCause], falling back to its type when it carries none — a bare `null`
    /// in an error dialog tells the user nothing.
    public static String describe(Throwable t) {
        Throwable root = rootCause(t);
        String message = root.getMessage();
        return message == null || message.isBlank() ? root.getClass().getSimpleName() : message;
    }
}
