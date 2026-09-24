/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.parser;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.key_project.solidity.logic.op.ProgramVariable;
import org.key_project.solidity.program.ast.StaticTypes;
import org.key_project.solidity.program.ast.abstractions.StorageReferenceTypes;
import org.key_project.solidity.program.ast.declarations.FunctionDeclaration;
import org.key_project.solidity.program.ast.declarations.FunctionEnums.DataLocation;
import org.key_project.solidity.program.ast.expressions.Expression;
import org.key_project.solidity.program.ast.expressions.FunctionCallExpression;
import org.key_project.solidity.program.ast.expressions.MemberExp;
import org.key_project.solidity.program.ast.expressions.operators.*;

import org.jspecify.annotations.Nullable;

public class ParserUtils {

    public static final String MAPPING_COPY_ERROR =
        "Assignments that would copy a mapping (directly, or nested in a struct or array) "
            + "are rejected by solc >= 0.7 and are not supported";

    public static final String MEMORY_MAPPING_ERROR =
        "Memory values of a type containing a mapping cannot exist; solc rejects such "
            + "declarations and they are not supported";

    private static final Map<String, BigInteger> NUMBER_UNITS = Map.ofEntries(
        Map.entry("wei", BigInteger.ONE),
        Map.entry("gwei", BigInteger.TEN.pow(9)),
        Map.entry("szabo", BigInteger.TEN.pow(12)),
        Map.entry("finney", BigInteger.TEN.pow(15)),
        Map.entry("ether", BigInteger.TEN.pow(18)),
        Map.entry("seconds", BigInteger.ONE),
        Map.entry("minutes", BigInteger.valueOf(60)),
        Map.entry("hours", BigInteger.valueOf(3_600)),
        Map.entry("days", BigInteger.valueOf(86_400)),
        Map.entry("weeks", BigInteger.valueOf(604_800)));

    public static BigInteger parseNumberLiteral(String text, @Nullable String unit) {
        String digits = text.replace("_", "");
        BigDecimal value = digits.startsWith("0x") || digits.startsWith("0X")
                ? new BigDecimal(new BigInteger(digits.substring(2), 16))
                : new BigDecimal(digits);
        if (unit != null && !unit.isEmpty()) {
            BigInteger factor = NUMBER_UNITS.get(unit);
            if (factor == null) {
                throw new SolidityParseException("Unsupported number unit " + unit);
            }
            value = value.multiply(new BigDecimal(factor));
        }
        try {
            return value.toBigIntegerExact();
        } catch (ArithmeticException e) {
            throw new SolidityParseException(
                "Number literal " + text + (unit == null ? "" : " " + unit)
                    + " is not an integer");
        }
    }

    static public Optional<Expression> parseBinaryOperationMaybe(Expression left, Expression right,
            String operator) {
        Operator op = null;
        for (var val : Operator.values()) {
            if (operator.equals(val.symbol()) && !Operator.isAssignmentOperator(val)) {
                op = val;
                break;
            }
        }
        return op == null ? Optional.empty()
                : Optional.of(new BinaryExpression(op, left, right));
    }

    static public Expression parseBinaryOperation(Expression left, Expression right,
            String operator) {
        return parseBinaryOperationMaybe(left, right, operator)
                .orElseThrow(
                    () -> new RuntimeException("Not yet supported binary operation: " + operator));
    }

    static public Optional<Expression> parseAssignmentMaybe(Expression left, Expression right,
            String operator) {
        if ("=".equals(operator) && left instanceof FunctionCallExpression call
                && call.getArguments().isEmpty()
                && call.getFunctionExp() instanceof MemberExp member
                && member.getRightExp() instanceof FunctionDeclaration function
                && "push".equals(function.name().toString())) {
            if (StorageReferenceTypes.containsMapping(StaticTypes.typeOf(right))) {
                throw new SolidityParseException(MAPPING_COPY_ERROR);
            }
            return Optional.of(new FunctionCallExpression(function.getType(), member,
                List.of(right)));
        }
        if ("=".equals(operator) && !isStoragePointerRebindTarget(left)
                && StorageReferenceTypes.containsMapping(StaticTypes.typeOf(left))) {
            throw new SolidityParseException(MAPPING_COPY_ERROR);
        }
        Operator op = null;
        for (var val : Operator.values()) {
            if (operator.equals(val.symbol()) && Operator.isAssignmentOperator(val)) {
                op = val;
                break;
            }
        }
        return op == null ? Optional.empty()
                : Optional.of(new AssignExpression(op, left, right));
    }

    private static boolean isStoragePointerRebindTarget(Expression left) {
        return left instanceof ProgramVariable pv
                && pv.getDataLocation() == DataLocation.Storage;
    }

    static public Expression parseAssignment(Expression left, Expression right, String operator) {
        return parseAssignmentMaybe(left, right, operator)
                .orElseThrow(
                    () -> new RuntimeException("Assignment: " + operator + " not supported"));
    }

    static public Optional<Expression> parseAllBinaryMaybe(Expression left, Expression right,
            String operator) {
        return parseBinaryOperationMaybe(left, right, operator)
                .or(() -> parseAssignmentMaybe(left, right, operator));
    }

    static public Expression parseAllBinary(Expression left, Expression right, String operator) {
        return parseAllBinaryMaybe(left, right, operator)
                .orElseThrow(
                    () -> new RuntimeException("Not yet supported binary operation: " + operator));
    }

    static public Optional<Expression> parseUnaryOperationMaybe(Expression uExp, String operator,
            boolean prefix) {
        Operator op = null;
        for (var val : Operator.values()) {
            if (operator.equals(val.symbol()) && prefix == val.isPrefix()) {
                op = val;
                break;
            }
        }
        return op == null ? Optional.empty()
                : Optional.of(new UnaryExpression(op, uExp));
    }

    static public Expression parseUnaryOperation(Expression uExp, String operator,
            boolean prefix) {
        return parseUnaryOperationMaybe(uExp, operator, prefix)
                .orElseThrow(
                    () -> new RuntimeException("Not yet supported binary operation: " + operator));
    }
}
