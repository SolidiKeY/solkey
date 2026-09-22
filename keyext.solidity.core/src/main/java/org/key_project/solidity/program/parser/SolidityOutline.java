/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.parser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.key_project.solidity.speclang.natspec.KeyNatspec;
import org.key_project.solidity.speclang.natspec.SpecException;

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

    /// A state variable or struct member: its name and solc's `typeString`.
    public record Variable(String name, String type) {
    }

    /// `documentation` is the contract's natspec comment, which carries the contract invariant;
    /// `enums` maps an enum name to its members in declaration order, `structs` a struct name to
    /// its members.
    public record Contract(String name, String documentation, List<Variable> stateVariables,
            Map<String, List<String>> enums, Map<String, List<Variable>> structs,
            List<Function> functions) {

        public Contract(String name, List<Function> functions) {
            this(name, "", List.of(), Map.of(), Map.of(), functions);
        }


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
    public record Function(String name, List<Parameter> parameters, List<Parameter> returns,
            String stateMutability, String documentation, Span source) {

        public Function(String name, List<Parameter> parameters, int resultCount,
                String documentation, Span source) {
            this(name, parameters, unnamed(resultCount), "nonpayable", documentation, source);
        }

        private static List<Parameter> unnamed(int count) {
            List<Parameter> returns = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                returns.add(new Parameter("", ""));
            }
            return List.copyOf(returns);
        }

        public int resultCount() {
            return returns.size();
        }

        public boolean payable() {
            return "payable".equals(stateMutability);
        }

        /// The natspec clauses of this function, or the reason they cannot be read.
        public KeyNatspec natspec() {
            return KeyNatspec.of(documentation);
        }

        /// Why no obligation can be generated for this function, or empty when one can: it is
        /// not skipped, it returns nothing or exactly one named value with a `.key` sort (the
        /// `\result` of an `ensures` clause), and every parameter needs a `.key` sort, so the
        /// obligation can bind it to an unconstrained program variable.
        public Optional<String> unsupportedReason() {
            try {
                if (natspec().skip()) {
                    return Optional.of("skipped by " + KeyNatspec.TAG + " skip");
                }
            } catch (SpecException e) {
                return Optional.of(e.getMessage());
            }
            if (returns.size() > 1) {
                return Optional.of("returns more than one value");
            }
            if (returns.size() == 1
                    && (returns.get(0).name().isEmpty() || returns.get(0).keySort() == null)) {
                return Optional.of("returns a value without a name or a .key sort; name it to"
                    + " refer to it as \\result, or move the specification into the body as"
                    + " assert");
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
            if (type.matches("u?int\\d*") || type.startsWith("enum ")) {
                return "int";
            }
            return type.equals("bool") ? "bool" : null;
        }
    }

    public static SolidityOutline of(Path solFile) throws IOException {
        JsonNode root = new ObjectMapper().readTree(SolcWrapper.getJsonSolidity(solFile));
        List<Contract> contracts = new ArrayList<>();
        for (JsonNode node : root.get("nodes").values()) {
            if ("ContractDefinition".equals(text(node, "nodeType"))) {
                contracts.add(contractOf(node));
            }
        }
        return new SolidityOutline(contracts);
    }

    public Optional<Contract> contract(String name) {
        return contracts.stream().filter(c -> c.name().equals(name)).findFirst();
    }

    private static Contract contractOf(JsonNode contract) {
        List<Variable> stateVariables = new ArrayList<>();
        Map<String, List<String>> enums = new LinkedHashMap<>();
        Map<String, List<Variable>> structs = new LinkedHashMap<>();
        for (JsonNode node : contract.get("nodes").values()) {
            switch (text(node, "nodeType")) {
                case "VariableDeclaration" -> stateVariables.add(variableOf(node));
                case "EnumDefinition" -> enums.put(text(node, "name"),
                    node.get("members").valueStream().map(m -> text(m, "name")).toList());
                case "StructDefinition" -> structs.put(text(node, "name"),
                    node.get("members").valueStream().map(SolidityOutline::variableOf).toList());
                default -> {
                }
            }
        }
        return new Contract(text(contract, "name"), documentationOf(contract),
            List.copyOf(stateVariables), enums, structs, functionsOf(contract));
    }

    private static String documentationOf(JsonNode node) {
        return node.has("documentation") && node.get("documentation").isObject()
                ? text(node.get("documentation"), "text")
                : "";
    }

    private static Variable variableOf(JsonNode node) {
        return new Variable(text(node, "name"), text(node.get("typeDescriptions"), "typeString"));
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
                parametersOf(node, "parameters"), parametersOf(node, "returnParameters"),
                text(node, "stateMutability"),
                documentation != null ? text(documentation, "text") : "",
                Span.union(Span.parse(text(documentation, "src")),
                    Span.parse(text(node, "src")))));
        }
        return functions;
    }

    private static List<Parameter> parametersOf(JsonNode function, String field) {
        List<Parameter> parameters = new ArrayList<>();
        for (JsonNode node : function.get(field).get("parameters").values()) {
            parameters.add(new Parameter(text(node, "name"),
                text(node.get("typeDescriptions"), "typeString")));
        }
        return parameters;
    }

    private static String text(JsonNode node, String field) {
        return node != null && node.has(field) ? node.get(field).asString() : "";
    }

}
