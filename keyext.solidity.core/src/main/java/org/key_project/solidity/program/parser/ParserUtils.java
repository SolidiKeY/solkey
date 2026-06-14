/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.parser;

import java.util.Optional;

import org.key_project.solidity.program.ast.expressions.Expression;
import org.key_project.solidity.program.ast.expressions.operators.*;

public class ParserUtils {

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
