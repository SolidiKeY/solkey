/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.parser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.key_project.solidity.speclang.natspec.KeyNatspec;
import org.key_project.solidity.speclang.natspec.SpecException;

import org.jspecify.annotations.Nullable;
import tools.jackson.databind.JsonNode;

import static org.key_project.solidity.program.parser.SolcAst.text;

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

    /// A state variable or struct member: its name, solc's `typeString` and, for a constant
    /// whose initializer evaluates at compile time, its value (see [SolidityConstants#valueOf]).
    public record Variable(String name, String type, @Nullable String constantValue) {
        public Variable(String name, String type) {
            this(name, type, null);
        }
    }

    /// `documentation` is the contract's natspec comment, which carries the contract invariant;
    /// `enums` maps an enum name to its members in declaration order, `structs` a struct name to
    /// its members, both including the file-level ones; `otherDocumentation` maps each member
    /// that gets no obligation (an internal function, a modifier, a state variable, ...) to its
    /// natspec comment, so a specification placed there is reported instead of dropped.
    public record Contract(String name, String documentation, List<Variable> stateVariables,
            Map<String, List<String>> enums, Map<String, List<Variable>> structs,
            List<Function> functions, Map<String, String> otherDocumentation) {

        public Contract(String name, String documentation, List<Variable> stateVariables,
                Map<String, List<String>> enums, Map<String, List<Variable>> structs,
                List<Function> functions) {
            this(name, documentation, stateVariables, enums, structs, functions, Map.of());
        }

        public Contract(String name, List<Function> functions) {
            this(name, "", List.of(), Map.of(), Map.of(), functions);
        }


        public Optional<Function> function(String id) {
            List<Function> named = functions.stream().filter(f -> f.name().equals(id)).toList();
            if (named.size() == 1) {
                return Optional.of(named.getFirst());
            }
            String wanted = id.replaceAll("\\s", "");
            return functions.stream().filter(f -> f.signature().equals(wanted)).findFirst();
        }

        public Optional<Function> functionAt(int offset) {
            return functions.stream().filter(f -> f.source().byteOffset() <= offset
                    && offset < f.source().byteOffset() + f.source().byteLength()).findFirst();
        }

        public String id(Function function) {
            long sharing = functions.stream().filter(f -> f.name().equals(function.name()))
                    .count();
            return sharing > 1 ? function.signature() : function.name();
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

        public static final String CONSTRUCTOR = "constructor";
        public static final String RECEIVE = "receive";
        public static final String FALLBACK = "fallback";

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

        public String signature() {
            return name + parameters.stream().map(Parameter::canonicalType)
                    .collect(Collectors.joining(",", "(", ")"));
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
        /// not skipped, every value it returns is named and has a `.key` sort (an `ensures`
        /// clause refers to it by name, or as `\result` when it is the only one), and every
        /// parameter needs a `.key` sort, so the obligation can bind it to an unconstrained
        /// program variable.
        public Optional<String> unsupportedReason() {
            try {
                if (natspec().skip()) {
                    return Optional.of("skipped by " + KeyNatspec.TAG + " skip");
                }
            } catch (SpecException e) {
                return Optional.of(e.getMessage());
            }
            for (Parameter ret : returns) {
                if (ret.name().isEmpty() || ret.keySort() == null) {
                    return Optional.of("returns a value without a name or a .key sort; name it to"
                        + " refer to it in ensures, or move the specification into the body as"
                        + " assert");
                }
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
        public boolean isConstructor() {
            return CONSTRUCTOR.equals(name);
        }

        public boolean isProvable() {
            return unsupportedReason().isEmpty();
        }
    }

    public record Parameter(String name, String type) {

        /// The `\programVariables` sort this parameter is declared with in the generated
        /// obligation, or `null` if the type has none.
        public String keySort() {
            if (type.matches("u?int\\d*") || type.matches("address( payable)?")
                    || type.startsWith("enum ")) {
                return "int";
            }
            return type.equals("bool") ? "bool" : null;
        }

        String canonicalType() {
            return type.replaceAll("^(enum|struct|contract) ", "")
                    .replaceAll(" (payable|memory|calldata|storage|ref|pointer)", "")
                    .replaceAll("\\s", "");
        }
    }

    public static SolidityOutline of(Path solFile) throws IOException {
        JsonNode root = SolcAst.of(solFile);
        Map<Integer, JsonNode> constants = new HashMap<>();
        Map<String, List<String>> fileEnums = new LinkedHashMap<>();
        Map<String, List<Variable>> fileStructs = new LinkedHashMap<>();
        for (JsonNode node : root.get("nodes").values()) {
            addConstant(constants, node);
            collectType(node, fileEnums, fileStructs);
            if ("ContractDefinition".equals(text(node, "nodeType"))) {
                node.get("nodes").values().forEach(member -> addConstant(constants, member));
            }
        }
        List<Contract> contracts = new ArrayList<>();
        for (JsonNode node : root.get("nodes").values()) {
            if ("ContractDefinition".equals(text(node, "nodeType"))
                    && !"library".equals(text(node, "contractKind"))) {
                contracts.add(contractOf(node, constants, fileEnums, fileStructs));
            }
        }
        return new SolidityOutline(contracts);
    }

    public Optional<Contract> contract(String name) {
        return contracts.stream().filter(c -> c.name().equals(name)).findFirst();
    }

    public List<String> contractNames() {
        return contracts.stream().map(Contract::name).toList();
    }

    /// The named contract, or the only one when `requested` is null; fails with the candidates
    /// listed otherwise.
    public Contract requireContract(@Nullable String requested, Path solFile) {
        if (requested != null) {
            return contract(requested).orElseThrow(() -> new IllegalArgumentException(solFile
                + " declares no contract " + requested + "; candidates: " + contractNames()));
        }
        if (contracts.size() != 1) {
            throw new IllegalArgumentException("no contract selected for " + solFile
                + "; use --contract with one of: " + contractNames());
        }
        return contracts.get(0);
    }

    private static void addConstant(Map<Integer, JsonNode> constants, JsonNode node) {
        if ("VariableDeclaration".equals(text(node, "nodeType"))
                && node.path("constant").asBoolean(false) && node.has("value")
                && !node.get("value").isNull()) {
            constants.put(node.get("id").asInt(), node);
        }
    }

    private static void collectType(JsonNode node, Map<String, List<String>> enums,
            Map<String, List<Variable>> structs) {
        switch (text(node, "nodeType")) {
            case "EnumDefinition" -> enums.put(text(node, "name"),
                node.get("members").valueStream().map(m -> text(m, "name")).toList());
            case "StructDefinition" -> structs.put(text(node, "name"),
                node.get("members").valueStream().map(SolidityOutline::variableOf).toList());
            default -> {
            }
        }
    }

    private static Contract contractOf(JsonNode contract, Map<Integer, JsonNode> constants,
            Map<String, List<String>> fileEnums, Map<String, List<Variable>> fileStructs) {
        List<Variable> stateVariables = new ArrayList<>();
        Map<String, List<String>> enums = new LinkedHashMap<>();
        Map<String, List<Variable>> structs = new LinkedHashMap<>();
        for (JsonNode node : contract.get("nodes").values()) {
            if ("VariableDeclaration".equals(text(node, "nodeType"))) {
                stateVariables.add(new Variable(text(node, "name"),
                    text(node.get("typeDescriptions"), "typeString"),
                    constants.containsKey(node.get("id").asInt())
                            ? SolidityConstants.valueOf(node.get("value"), constants)
                            : null));
            }
            collectType(node, enums, structs);
        }
        fileEnums.forEach(enums::putIfAbsent);
        fileStructs.forEach(structs::putIfAbsent);
        String documentation = documentationOf(contract);
        List<Function> functions = functionsOf(contract);
        if (functions.stream().noneMatch(Function::isConstructor)
                && declaresInvariant(documentation)) {
            functions.addFirst(new Function(Function.CONSTRUCTOR, List.of(), List.of(),
                "nonpayable", "", Span.NONE));
        }
        return new Contract(text(contract, "name"), documentation, List.copyOf(stateVariables),
            enums, structs, List.copyOf(functions), otherDocumentationOf(contract));
    }

    private static Map<String, String> otherDocumentationOf(JsonNode contract) {
        Map<String, String> documentation = new LinkedHashMap<>();
        for (JsonNode node : contract.get("nodes").values()) {
            String text = documentationOf(node);
            if (!text.contains(KeyNatspec.TAG)) {
                continue;
            }
            String name = text(node, "name");
            String place = switch (text(node, "nodeType")) {
                case "FunctionDefinition" -> {
                    String kind = text(node, "kind");
                    boolean provable = List.of(Function.CONSTRUCTOR, Function.RECEIVE,
                        Function.FALLBACK).contains(kind) || "function".equals(kind)
                                && List.of("public", "external").contains(text(node, "visibility"));
                    yield provable ? null
                            : "function".equals(kind)
                                    ? text(node, "visibility") + " function " + name
                                    : kind;
                }
                case "ModifierDefinition" -> "modifier " + name;
                case "VariableDeclaration" -> "state variable " + name;
                case "EventDefinition" -> "event " + name;
                case "ErrorDefinition" -> "error " + name;
                case "StructDefinition" -> "struct " + name;
                case "EnumDefinition" -> "enum " + name;
                default -> text(node, "nodeType");
            };
            if (place != null) {
                documentation.put(place, text);
            }
        }
        return documentation;
    }

    public static boolean declaresInvariant(String documentation) {
        try {
            return !KeyNatspec.of(documentation).invariants().isEmpty();
        } catch (SpecException e) {
            return true;
        }
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
            if (!"FunctionDefinition".equals(text(node, "nodeType"))) {
                continue;
            }
            String kind = text(node, "kind");
            boolean special = List.of(Function.CONSTRUCTOR, Function.RECEIVE, Function.FALLBACK)
                    .contains(kind);
            if (!special && (!"function".equals(kind)
                    || !List.of("public", "external").contains(text(node, "visibility")))) {
                continue;
            }
            functions.add(functionOf(node));
        }
        return functions;
    }

    public static Function functionOf(JsonNode node) {
        String kind = text(node, "kind");
        boolean special = List.of(Function.CONSTRUCTOR, Function.RECEIVE, Function.FALLBACK)
                .contains(kind);
        JsonNode documentation = node.has("documentation") ? node.get("documentation") : null;
        return new Function(special ? kind : text(node, "name"),
            parametersOf(node, "parameters"), parametersOf(node, "returnParameters"),
            text(node, "stateMutability"),
            documentation != null ? text(documentation, "text") : "",
            Span.union(Span.parse(text(documentation, "src")), Span.parse(text(node, "src"))));
    }

    private static List<Parameter> parametersOf(JsonNode function, String field) {
        List<Parameter> parameters = new ArrayList<>();
        if (!function.has(field)) {
            return parameters;
        }
        for (JsonNode node : function.get(field).get("parameters").values()) {
            parameters.add(new Parameter(text(node, "name"),
                text(node.get("typeDescriptions"), "typeString")));
        }
        return parameters;
    }
}
