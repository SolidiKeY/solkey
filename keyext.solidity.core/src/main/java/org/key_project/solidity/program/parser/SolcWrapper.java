/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.parser;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jspecify.annotations.Nullable;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/// The Solidity compiler, addressed through its Standard JSON interface.
///
/// The compiler runs inside this JVM ([WasmSolcCompiler]); nothing is forked and no
/// platform-specific executable is needed. The AST handed out is solc's own — the `ast` output of
/// Standard JSON is the node format `--ast-compact-json` prints — so [SolJSONParser] sees exactly
/// what it saw when solc was a child process.
public class SolcWrapper {

    /// A compilation request. The same source under the same unit name always yields the same
    /// AST, so it is compiled once: one obligation asks for a contract twice — to enumerate its
    /// functions and again while loading — and taclet loading re-reads the same snippets.
    private record CompilationUnit(String name, String source) {
    }

    private static final Map<CompilationUnit, String> AST_CACHE = new ConcurrentHashMap<>();

    private record Build(CompilationUnit unit, JsonNode output) {
    }

    private static volatile @Nullable Build lastBuild;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final String STDIN_UNIT = "<stdin>";

    private static final long UNSIGNED_32_BIT = 1L << 32;

    private static @Nullable SolcCompiler compiler;

    public static synchronized void useCompiler(SolcCompiler replacement) {
        compiler = replacement;
        AST_CACHE.clear();
        lastBuild = null;
    }

    private static synchronized SolcCompiler compiler() {
        if (compiler == null) {
            compiler = WasmSolcCompiler.get();
        }
        return compiler;
    }

    /// The solc AST JSON of `contractPath`.
    public static String getJsonSolidity(Path contractPath) throws IOException {
        return astOf(unitNameOf(contractPath), readSource(contractPath));
    }

    private static String unitNameOf(Path contractPath) {
        return contractPath.toAbsolutePath().normalize().toString();
    }

    private static String readSource(Path contractPath) throws IOException {
        return SoliditySources.read(contractPath);
    }

    /// The `SourceUnit` node of `source`, as a JSON string.
    private static String astOf(String unitName, String source) throws IOException {
        CompilationUnit unit = new CompilationUnit(unitName, source);
        String cached = AST_CACHE.get(unit);
        if (cached != null) {
            return cached;
        }
        ObjectNode outputSelection = MAPPER.createObjectNode();
        outputSelection.putArray("").add("ast");
        JsonNode ast =
            compile(unitName, source, outputSelection).path("sources").path(unitName).path("ast");
        if (ast.isMissingNode() || ast.isNull()) {
            throw new RuntimeException(
                "Not possible to compile solidity code:\nno AST produced for " + unitName);
        }
        restoreNegativeIds(ast);
        String json = ast.toString();
        AST_CACHE.put(unit, json);
        return json;
    }

    /// solc targets 32 bits when compiled to WebAssembly, and there the negative ids it gives
    /// Solidity's builtin declarations — `require` is -18, `msg` is -15 — are exported as their
    /// unsigned complement. Subtracting 2^32 back restores the ids a native solc prints, so the
    /// AST is the one the rest of the pipeline was written against.
    private static void restoreNegativeIds(JsonNode node) {
        if (node instanceof ObjectNode object) {
            for (Map.Entry<String, JsonNode> property : object.properties()) {
                if (isUnsignedNegative(property.getValue())) {
                    object.put(property.getKey(), signed(property.getValue()));
                } else {
                    restoreNegativeIds(property.getValue());
                }
            }
        } else if (node instanceof ArrayNode array) {
            for (int i = 0; i < array.size(); i++) {
                if (isUnsignedNegative(array.get(i))) {
                    array.set(i, array.numberNode(signed(array.get(i))));
                } else {
                    restoreNegativeIds(array.get(i));
                }
            }
        }
    }

    private static boolean isUnsignedNegative(JsonNode node) {
        return node.isIntegralNumber() && node.asLong() > Integer.MAX_VALUE
                && node.asLong() < UNSIGNED_32_BIT;
    }

    private static int signed(JsonNode node) {
        return (int) (node.asLong() - UNSIGNED_32_BIT);
    }

    /// The deployment and runtime bytecode of every contract in `contractPath`, as solc's
    /// Standard JSON output. Used by the runtime cross-check tests to execute the examples on a
    /// real EVM.
    public static String getBinJson(Path contractPath) throws IOException {
        JsonNode output = build(contractPath);
        failOnErrors(output);
        return output.toString();
    }

    public static String diagnose(Path contractPath) throws IOException {
        return build(contractPath).toString();
    }

    private static JsonNode build(Path contractPath) throws IOException {
        CompilationUnit unit =
            new CompilationUnit(unitNameOf(contractPath), readSource(contractPath));
        Build last = lastBuild;
        if (last != null && last.unit().equals(unit)) {
            return last.output();
        }
        ObjectNode outputSelection = MAPPER.createObjectNode();
        outputSelection.putArray("").add("ast");
        outputSelection.putArray("*")
                .add("evm.bytecode.object")
                .add("evm.deployedBytecode.object");
        JsonNode output = compileRaw(unit.name(), unit.source(), outputSelection);
        JsonNode ast = output.path("sources").path(unit.name()).path("ast");
        if (!ast.isMissingNode() && !ast.isNull()) {
            restoreNegativeIds(ast);
            AST_CACHE.putIfAbsent(unit, ast.toString());
        }
        lastBuild = new Build(unit, output);
        return output;
    }

    public static String version() {
        return compiler().version();
    }

    private static JsonNode compile(String unitName, String source, ObjectNode outputSelection)
            throws IOException {
        JsonNode output = compileRaw(unitName, source, outputSelection);
        failOnErrors(output);
        return output;
    }

    private static JsonNode compileRaw(String unitName, String source, ObjectNode outputSelection)
            throws IOException {
        ObjectNode input = MAPPER.createObjectNode();
        input.put("language", "Solidity");
        input.putObject("sources").putObject(unitName).put("content", source);
        input.putObject("settings").putObject("outputSelection").set("*", outputSelection);

        return MAPPER.readTree(compiler().compile(input.toString()));
    }

    private static void failOnErrors(JsonNode output) {
        StringBuilder errors = new StringBuilder();
        for (JsonNode error : output.path("errors").values()) {
            if ("error".equals(error.path("severity").asString(""))) {
                errors.append(error.path("formattedMessage").asString(
                    error.path("message").asString(""))).append('\n');
            }
        }
        if (!errors.isEmpty()) {
            throw new RuntimeException("Not possible to compile solidity code:\n" + errors);
        }
    }

    public static String readSol(String contract) throws IOException {
        return astOf(STDIN_UNIT, contract);
    }

    public static JsonNode readJson(String json) {
        return MAPPER.readTree(json);
    }
}
