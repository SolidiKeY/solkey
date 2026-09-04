/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.parser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
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

    /// A range of the source file, as solc reports it in a node's `src` field.
    ///
    /// solc counts **bytes**, not characters, so a file with a non-ASCII comment above a function
    /// slices wrongly when these offsets are applied to a `String`. [#textIn] takes the raw bytes
    /// for that reason; callers read the file once and pass the array around.
    public record Span(int byteOffset, int byteLength) {

        /// The span of a node solc gave no usable `src` for. Slicing it yields no text.
        public static final Span NONE = new Span(0, 0);

        public boolean isEmpty() {
            return byteLength <= 0;
        }

        /// The text this span covers in `source`, or the empty string when the span is unusable —
        /// including when it runs past the end because the file changed after solc read it.
        public String textIn(byte[] source) {
            if (isEmpty() || byteOffset < 0 || byteOffset >= source.length) {
                return "";
            }
            int available = Math.min(byteLength, source.length - byteOffset);
            return new String(source, byteOffset, available, StandardCharsets.UTF_8);
        }

        /// The 1-based line this span starts on in `source`.
        public int lineIn(byte[] source) {
            int line = 1;
            for (int i = 0; i < byteOffset && i < source.length; i++) {
                if (source[i] == '\n') {
                    line++;
                }
            }
            return line;
        }

        /// Parses a `"<offset>:<length>:<fileIndex>"` field, yielding [#NONE] when it is absent or
        /// malformed: an outline is still useful without source ranges.
        static Span parse(String src) {
            String[] parts = src.split(":");
            if (parts.length < 2) {
                return NONE;
            }
            try {
                return new Span(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
            } catch (NumberFormatException e) {
                return NONE;
            }
        }

        /// The smallest span covering both, ignoring an empty one.
        static Span union(Span a, Span b) {
            if (a.isEmpty()) {
                return b;
            }
            if (b.isEmpty()) {
                return a;
            }
            int start = Math.min(a.byteOffset, b.byteOffset);
            int end = Math.max(a.byteOffset + a.byteLength, b.byteOffset + b.byteLength);
            return new Span(start, end - start);
        }
    }

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
    ///
    /// `source` spans the declaration as written, natspec included, so it can be shown to a user
    /// choosing what to prove.
    public record Function(String name, List<Parameter> parameters, int resultCount,
            String documentation, Span source) {

        /// Why no obligation can be generated for this function, or empty when one can: it has to
        /// return nothing (its specification lives in the body as `assert`) and every parameter
        /// needs a `.key` sort, so the obligation can bind it to an unconstrained program
        /// variable.
        public Optional<String> unsupportedReason() {
            if (resultCount != 0) {
                return Optional.of(
                    "returns a value; the specification has to live in the body as assert");
            }
            for (Parameter parameter : parameters) {
                if (parameter.keySort() == null) {
                    return Optional.of("parameter " + parameter.name() + " has unsupported type "
                        + parameter.type());
                }
            }
            return Optional.empty();
        }

        /// Whether an obligation can be generated for this function; [#unsupportedReason] says
        /// why not.
        public boolean isProvable() {
            return unsupportedReason().isEmpty();
        }
    }

    public record Parameter(String name, String type) {

        /// The `\programVariables` sort this parameter is declared with in the generated
        /// obligation, or `null` if the type has none.
        public String keySort() {
            return type.matches("u?int\\d*") ? "int" : null;
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
            JsonNode documentation = node.has("documentation") ? node.get("documentation") : null;
            functions.add(new Function(text(node, "name"),
                parametersOf(node), count(node, "returnParameters"),
                documentation != null ? text(documentation, "text") : "",
                Span.union(Span.parse(text(documentation, "src")),
                    Span.parse(text(node, "src")))));
        }
        return functions;
    }

    private static List<Parameter> parametersOf(JsonNode function) {
        List<Parameter> parameters = new ArrayList<>();
        for (JsonNode node : function.get("parameters").get("parameters").values()) {
            parameters.add(new Parameter(text(node, "name"),
                text(node.get("typeDescriptions"), "typeString")));
        }
        return parameters;
    }

    private static String text(JsonNode node, String field) {
        return node != null && node.has(field) ? node.get(field).asString() : "";
    }

    private static int count(JsonNode node, String field) {
        return node.has(field) ? node.get(field).get("parameters").size() : 0;
    }
}
