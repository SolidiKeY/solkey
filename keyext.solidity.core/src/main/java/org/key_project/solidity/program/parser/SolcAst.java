/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.parser;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

import org.jspecify.annotations.Nullable;
import tools.jackson.databind.JsonNode;

/// Navigation helpers over solc's compact JSON AST.
public final class SolcAst {
    private SolcAst() {}

    public static JsonNode of(Path solFile) throws IOException {
        return SolcWrapper.readJson(SolcWrapper.getJsonSolidity(solFile));
    }

    public static Optional<JsonNode> contractNode(Path solFile, String contract)
            throws IOException {
        for (JsonNode node : of(solFile).get("nodes").values()) {
            if (contract.equals(text(node, "name"))) {
                return Optional.of(node);
            }
        }
        return Optional.empty();
    }

    public static String text(@Nullable JsonNode node, String field) {
        return node != null && node.has(field) ? node.get(field).asString() : "";
    }
}
