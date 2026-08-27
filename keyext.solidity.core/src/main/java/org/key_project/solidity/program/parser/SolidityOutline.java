/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.parser;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/// The contracts and functions a `.sol` file declares, read straight from solc's AST JSON.
///
/// The full parsing pipeline cannot answer this: [SolJSONParser] needs a `Services`, which only
/// exists once a proof obligation has been built — and building one requires knowing which
/// function to prove. This scan closes that circle.
public record SolidityOutline(List<Contract> contracts) {

    public record Contract(String name, List<Function> functions) {

        public Optional<Function> function(String name) {
            return functions.stream().filter(f -> f.name().equals(name)).findFirst();
        }

        public List<Function> provableFunctions() {
            return functions.stream().filter(Function::isProvable).toList();
        }
    }

    /// `documentation` is the function's natspec comment, used to carry per-function directives
    /// into the generated obligation (see `SolidityProblemSynthesizer`).
    public record Function(String name, int parameterCount, int resultCount,
            String documentation) {

        /// Whether an obligation can be generated for this function: it takes no arguments and
        /// returns nothing, so its specification has to live in the body as `assert`.
        public boolean isProvable() {
            return parameterCount == 0 && resultCount == 0;
        }
    }

    public static SolidityOutline of(Path solFile) throws IOException {
        JsonNode root = new ObjectMapper().readTree(SolcWrapper.getJsonSolidity(solFile));
        List<Contract> contracts = new ArrayList<>();
        for (JsonNode node : root.get("nodes").values()) {
            if ("ContractDefinition".equals(text(node, "nodeType"))) {
                contracts.add(new Contract(text(node, "name"), functionsOf(node)));
            }
        }
        return new SolidityOutline(contracts);
    }

    public Optional<Contract> contract(String name) {
        return contracts.stream().filter(c -> c.name().equals(name)).findFirst();
    }

    private static List<Function> functionsOf(JsonNode contract) {
        List<Function> functions = new ArrayList<>();
        for (JsonNode node : contract.get("nodes").values()) {
            if (!"FunctionDefinition".equals(text(node, "nodeType"))
                    || !"function".equals(text(node, "kind"))
                    || !"public".equals(text(node, "visibility"))) {
                continue;
            }
            functions.add(new Function(text(node, "name"),
                count(node, "parameters"), count(node, "returnParameters"),
                node.has("documentation") ? text(node.get("documentation"), "text") : ""));
        }
        return functions;
    }

    private static String text(JsonNode node, String field) {
        return node != null && node.has(field) ? node.get(field).asString() : "";
    }

    private static int count(JsonNode node, String field) {
        return node.has(field) ? node.get(field).get("parameters").size() : 0;
    }
}
