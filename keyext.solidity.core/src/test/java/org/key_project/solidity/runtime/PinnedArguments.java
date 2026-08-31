/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.runtime;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.key_project.solidity.program.parser.SolcWrapper;
import org.key_project.solidity.program.parser.SolidityOutline;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/// Recovers concrete argument values for a parameterized example function from its leading
/// `require` pins, so the function can be executed as well as proved.
///
/// The example conventions (`keyext.solidity.examples/README.md`) demand that a parameterized
/// function pin every parameter with leading requires — an equality pin, either direct
/// (`require(x == 5 && y == 7);`) or through a literal-bound local
/// (`int minusTwo = -2; require(b == minusTwo);`), or a range pin
/// (`require(x >= 1 && x <= 100);`). Walking the body's leading statements over exactly those
/// shapes recovers an argument vector the proof talks about: the pinned value, or the tightest
/// bound of the pinned range as a witness.
final class PinnedArguments {

    private PinnedArguments() {}

    /// The lower and upper bound the leading requires establish; an equality pin sets both.
    private static final class Bounds {
        BigInteger low;
        BigInteger high;

        BigInteger witness() {
            return low != null ? low : high;
        }

        boolean satisfiable() {
            return low == null || high == null || low.compareTo(high) <= 0;
        }
    }

    /// A value for each of `function`'s parameters in declaration order, or empty if any
    /// parameter is left unpinned by the leading declarations and requires.
    static Optional<List<BigInteger>> of(Path solFile, String contract,
            SolidityOutline.Function function) throws IOException {
        Map<String, Bounds> pins = new HashMap<>();
        Map<String, BigInteger> locals = new HashMap<>();
        for (JsonNode statement : bodyOf(solFile, contract, function.name())) {
            if (!collectPins(statement, locals, pins)) {
                break;
            }
        }
        List<BigInteger> values = new ArrayList<>();
        for (SolidityOutline.Parameter parameter : function.parameters()) {
            Bounds bounds = pins.get(parameter.name());
            if (bounds == null || !bounds.satisfiable()) {
                return Optional.empty();
            }
            values.add(bounds.witness());
        }
        return Optional.of(values);
    }

    private static JsonNode bodyOf(Path solFile, String contract, String function)
            throws IOException {
        JsonNode root = new ObjectMapper().readTree(SolcWrapper.getJsonSolidity(solFile));
        for (JsonNode contractNode : root.get("nodes").values()) {
            if (!contract.equals(text(contractNode, "name"))) {
                continue;
            }
            for (JsonNode member : contractNode.get("nodes").values()) {
                if ("FunctionDefinition".equals(text(member, "nodeType"))
                        && function.equals(text(member, "name"))) {
                    return member.get("body").get("statements");
                }
            }
        }
        throw new IllegalArgumentException(contract + "." + function + " not in " + solFile);
    }

    /// Records what `statement` pins and returns whether the walk may continue: local literal
    /// bindings extend `locals`, requires extend `pins`, anything else ends the leading prefix.
    private static boolean collectPins(JsonNode statement, Map<String, BigInteger> locals,
            Map<String, Bounds> pins) {
        String nodeType = text(statement, "nodeType");
        if ("VariableDeclarationStatement".equals(nodeType)) {
            JsonNode declarations = statement.get("declarations");
            BigInteger value = literalValue(statement.get("initialValue"));
            if (declarations.size() == 1 && value != null) {
                locals.put(text(declarations.get(0), "name"), value);
            }
            return true;
        }
        JsonNode call = "ExpressionStatement".equals(nodeType) ? statement.get("expression") : null;
        if (call != null && "FunctionCall".equals(text(call, "nodeType"))
                && "require".equals(text(call.get("expression"), "name"))) {
            collectConjuncts(call.get("arguments").get(0), locals, pins);
            return true;
        }
        return false;
    }

    private static void collectConjuncts(JsonNode condition, Map<String, BigInteger> locals,
            Map<String, Bounds> pins) {
        if (!"BinaryOperation".equals(text(condition, "nodeType"))) {
            return;
        }
        JsonNode left = condition.get("leftExpression");
        JsonNode right = condition.get("rightExpression");
        String operator = text(condition, "operator");
        if ("&&".equals(operator)) {
            collectConjuncts(left, locals, pins);
            collectConjuncts(right, locals, pins);
        } else if (!recordPin(left, operator, right, locals, pins)) {
            recordPin(right, mirrored(operator), left, locals, pins);
        }
    }

    private static String mirrored(String operator) {
        return switch (operator) {
            case "<" -> ">";
            case "<=" -> ">=";
            case ">" -> "<";
            case ">=" -> "<=";
            default -> operator;
        };
    }

    private static boolean recordPin(JsonNode identifier, String operator, JsonNode valueNode,
            Map<String, BigInteger> locals, Map<String, Bounds> pins) {
        if (!"Identifier".equals(text(identifier, "nodeType"))) {
            return false;
        }
        BigInteger value = literalValue(valueNode);
        if (value == null && "Identifier".equals(text(valueNode, "nodeType"))) {
            value = locals.get(text(valueNode, "name"));
        }
        if (value == null) {
            return false;
        }
        Bounds bounds = pins.computeIfAbsent(text(identifier, "name"), name -> new Bounds());
        switch (operator) {
            case "==" -> {
                bounds.low = value;
                bounds.high = value;
            }
            case ">=" -> bounds.low = max(bounds.low, value);
            case ">" -> bounds.low = max(bounds.low, value.add(BigInteger.ONE));
            case "<=" -> bounds.high = min(bounds.high, value);
            case "<" -> bounds.high = min(bounds.high, value.subtract(BigInteger.ONE));
            default -> {
                return false;
            }
        }
        return true;
    }

    private static BigInteger max(BigInteger current, BigInteger candidate) {
        return current == null ? candidate : current.max(candidate);
    }

    private static BigInteger min(BigInteger current, BigInteger candidate) {
        return current == null ? candidate : current.min(candidate);
    }

    private static BigInteger literalValue(JsonNode expression) {
        if (expression == null) {
            return null;
        }
        if ("UnaryOperation".equals(text(expression, "nodeType"))
                && "-".equals(text(expression, "operator"))) {
            BigInteger operand = literalValue(expression.get("subExpression"));
            return operand == null ? null : operand.negate();
        }
        if (!"Literal".equals(text(expression, "nodeType"))
                || !"number".equals(text(expression, "kind"))) {
            return null;
        }
        String value = text(expression, "value");
        return value.startsWith("0x") ? new BigInteger(value.substring(2), 16)
                : new BigInteger(value);
    }

    private static String text(JsonNode node, String field) {
        return node != null && node.has(field) ? node.get(field).asString() : "";
    }
}
