/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.parser;

import org.jspecify.annotations.Nullable;
import tools.jackson.databind.JsonNode;

/// Signals that the `solc --ast-compact-json` output could not be converted into a KeY for
/// Solidity AST. Carries the `src` position of the offending JSON node when one is available.
public class SolidityParseException extends RuntimeException {

    public SolidityParseException(String message) {
        super(message);
    }

    public SolidityParseException(String message, @Nullable JsonNode node) {
        super(node != null && node.has("src")
                ? message + " (at src " + node.get("src").asString() + ")"
                : message);
    }
}
