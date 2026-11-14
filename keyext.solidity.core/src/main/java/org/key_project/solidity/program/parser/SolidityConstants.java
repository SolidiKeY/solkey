/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.parser;

import java.math.BigInteger;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jspecify.annotations.Nullable;
import tools.jackson.databind.JsonNode;

import static org.key_project.solidity.program.parser.SolcAst.text;

public final class SolidityConstants {

    private static final Pattern RATIONAL = Pattern.compile("t_rational_(minus_)?(\\d+)_by_1");
    private static final Pattern INTEGER_TYPE = Pattern.compile("(u?)int(\\d*)");
    private static final BigInteger MAX_EXPONENT = BigInteger.valueOf(4096);

    private SolidityConstants() {
    }

    public static @Nullable BigInteger intConst(JsonNode node) {
        JsonNode descriptions = node.get("typeDescriptions");
        if (descriptions == null || !text(descriptions, "typeString").startsWith("int_const ")) {
            return null;
        }
        Matcher rational = RATIONAL.matcher(text(descriptions, "typeIdentifier"));
        if (!rational.matches()) {
            return null;
        }
        BigInteger value = new BigInteger(rational.group(2));
        return rational.group(1) == null ? value : value.negate();
    }

    public static @Nullable String valueOf(JsonNode initializer, Map<Integer, JsonNode> constants) {
        Object value = new Evaluator(constants).eval(initializer);
        return value == null ? null : value.toString();
    }

    private static final class Evaluator {
        private final Map<Integer, JsonNode> constants;
        private final Set<Integer> visiting = new HashSet<>();

        Evaluator(Map<Integer, JsonNode> constants) {
            this.constants = constants;
        }

        @Nullable
        Object eval(JsonNode node) {
            BigInteger folded = intConst(node);
            if (folded != null) {
                return folded;
            }
            return switch (text(node, "nodeType")) {
                case "Literal" -> literal(node);
                case "TupleExpression" -> parenthesized(node);
                case "Identifier", "MemberAccess" -> reference(node);
                case "FunctionCall" -> conversion(node);
                case "UnaryOperation" -> unary(node);
                case "BinaryOperation" -> binary(node);
                default -> null;
            };
        }

        private @Nullable Object literal(JsonNode node) {
            return switch (text(node, "kind")) {
                case "bool" -> Boolean.valueOf(text(node, "value"));
                case "number" -> inRange(node, ParserUtils.parseNumberLiteral(text(node, "value"),
                    node.has("subdenomination") && !node.get("subdenomination").isNull()
                            ? text(node, "subdenomination")
                            : null));
                default -> null;
            };
        }

        private @Nullable Object parenthesized(JsonNode node) {
            JsonNode components = node.get("components");
            return node.path("isInlineArray").asBoolean(false) || components == null
                    || components.size() != 1 || components.get(0).isNull() ? null
                            : eval(components.get(0));
        }

        private @Nullable Object reference(JsonNode node) {
            if ("MemberAccess".equals(text(node, "nodeType"))) {
                Object bound = typeBound(node);
                if (bound != null) {
                    return bound;
                }
            }
            JsonNode declaration = node.get("referencedDeclaration");
            if (declaration == null || declaration.isNull()) {
                return null;
            }
            int id = declaration.asInt();
            JsonNode constant = constants.get(id);
            if (constant == null || !visiting.add(id)) {
                return null;
            }
            try {
                return eval(constant.get("value"));
            } finally {
                visiting.remove(id);
            }
        }

        private @Nullable Object typeBound(JsonNode node) {
            JsonNode call = node.get("expression");
            if (!"FunctionCall".equals(text(call, "nodeType"))
                    || !"type".equals(text(call.get("expression"), "name"))
                    || call.get("arguments").size() != 1) {
                return null;
            }
            JsonNode typeName = call.get("arguments").get(0).get("typeName");
            BigInteger[] range = range(typeName == null ? "" : text(typeName, "name"));
            if (range == null) {
                return null;
            }
            return switch (text(node, "memberName")) {
                case "min" -> range[0];
                case "max" -> range[1];
                default -> null;
            };
        }

        private @Nullable Object conversion(JsonNode node) {
            if (!"typeConversion".equals(text(node, "kind")) || node.get("arguments").size() != 1) {
                return null;
            }
            return eval(node.get("arguments").get(0)) instanceof BigInteger value
                    ? inRange(node, value)
                    : null;
        }

        private @Nullable Object unary(JsonNode node) {
            Object operand = eval(node.get("subExpression"));
            return switch (text(node, "operator")) {
                case "-" -> operand instanceof BigInteger value ? inRange(node, value.negate())
                        : null;
                case "!" -> operand instanceof Boolean value ? !value : null;
                default -> null;
            };
        }

        private @Nullable Object binary(JsonNode node) {
            Object left = eval(node.get("leftExpression"));
            Object right = eval(node.get("rightExpression"));
            String operator = text(node, "operator");
            if (left instanceof Boolean l && right instanceof Boolean r) {
                return switch (operator) {
                    case "&&" -> l && r;
                    case "||" -> l || r;
                    case "==" -> l.equals(r);
                    case "!=" -> !l.equals(r);
                    default -> null;
                };
            }
            if (!(left instanceof BigInteger l) || !(right instanceof BigInteger r)) {
                return null;
            }
            return switch (operator) {
                case "+" -> inRange(node, l.add(r));
                case "-" -> inRange(node, l.subtract(r));
                case "*" -> inRange(node, l.multiply(r));
                case "/" -> r.signum() == 0 ? null : inRange(node, l.divide(r));
                case "%" -> r.signum() == 0 ? null : inRange(node, l.remainder(r));
                case "**" -> r.signum() < 0 || r.compareTo(MAX_EXPONENT) > 0 ? null
                        : inRange(node, l.pow(r.intValueExact()));
                case "==" -> l.equals(r);
                case "!=" -> !l.equals(r);
                case "<" -> l.compareTo(r) < 0;
                case "<=" -> l.compareTo(r) <= 0;
                case ">" -> l.compareTo(r) > 0;
                case ">=" -> l.compareTo(r) >= 0;
                default -> null;
            };
        }

        private static @Nullable BigInteger inRange(JsonNode node, BigInteger value) {
            BigInteger[] range =
                range(text(node.get("typeDescriptions"), "typeString").replace(" payable", ""));
            return range == null || value.compareTo(range[0]) < 0 || value.compareTo(range[1]) > 0
                    ? null
                    : value;
        }

        private static BigInteger @Nullable [] range(String type) {
            if (type.equals("address")) {
                return new BigInteger[] { BigInteger.ZERO,
                    BigInteger.TWO.pow(160).subtract(BigInteger.ONE) };
            }
            Matcher integer = INTEGER_TYPE.matcher(type);
            if (!integer.matches()) {
                return null;
            }
            boolean signed = integer.group(1).isEmpty();
            int bits = integer.group(2).isEmpty() ? 256 : Integer.parseInt(integer.group(2));
            BigInteger max = BigInteger.TWO.pow(signed ? bits - 1 : bits).subtract(BigInteger.ONE);
            return new BigInteger[] { signed ? max.add(BigInteger.ONE).negate() : BigInteger.ZERO,
                max };
        }
    }
}
