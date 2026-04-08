/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.parser;

import java.util.Optional;

import org.key_project.solidity.program.ast.abstractions.Type;
import org.key_project.solidity.program.ast.expressions.Expression;
import org.key_project.solidity.program.ast.expressions.operators.*;

public class ParserUtils {

    static public Optional<Expression> parseBinaryOperationMaybe(Expression left, Expression right,
            String operator, Type expType) {
        Expression exp = switch (operator) {
            case "+" -> new AddOperator(left, right, expType);
            case "-" -> new SubtractionOperator(left, right, expType);
            case "*" -> new MultiplicationOperator(left, right, expType);
            case "/" -> new DivOperator(left, right, expType);
            case "%" -> new ModOperator(left, right, expType);
            case "^" -> new ExponentialOperator(left, right, expType);
            case "&&" -> new AndOperator(left, right, expType);
            case "&" -> new BitwiseAndOperator(left, right, expType);
            case "||" -> new OrOperator(left, right, expType);
            case "|" -> new BitwiseOrOperator(left, right, expType);
            case "!=" -> new UnequalOperator(left, right, expType);
            case "==" -> new EqualOperator(left, right, expType);
            case ">=" -> new GreaterEqualOperator(left, right, expType);
            case ">" -> new GreaterOperator(left, right, expType);
            case "<=" -> new LessEqualOperator(left, right, expType);
            case "<" -> new LessOperator(left, right, expType);
            case "<<" -> new LeftShiftOperator(left, right);
            case ">>" -> new RightShiftOperator(left, right);
            case ">>>" -> new LogicalRightShiftOperator(left, right);
            default -> null;
        };
        return exp == null ? Optional.empty() : Optional.of(exp);
    }

    static public Expression parseBinaryOperation(Expression left, Expression right,
            String operator, Type expType) {
        return parseBinaryOperationMaybe(left, right, operator, expType)
                .orElseThrow(
                    () -> new RuntimeException("Not yet supported binary operation: " + operator));
    }

    static public Optional<Expression> parseAssignmentMaybe(Expression left, Expression right,
            String operator, Type expType) {
        Expression exp = switch (operator) {
            case "=" -> new AssignmentExpression(left, right);
            case "|=" -> new OrEqualOperator(left, right);
            case "^=" -> new XorEqualOperator(left, right);
            case "&=" -> new AndEqualOperator(left, right);
            case "<<=" -> new LeftShiftEqualOperator(left, right);
            case ">>=" -> new RightShiftEqualOperator(left, right);
            case ">>>=" -> new LogicalRightShiftEqualOperator(left, right);
            case "+=" -> new PlusEqualOperator(left, right);
            case "-=" -> new MinusEqualOperator(left, right);
            case "*=" -> new MultiplicationEqualOperator(left, right);
            case "/=" -> new DivisionEqualOperator(left, right);
            case "%=" -> new ModEqualOperator(left, right);
            default -> null;
        };
        return exp == null ? Optional.empty() : Optional.of(exp);
    }

    static public Expression parseAssignment(Expression left, Expression right, String operator,
            Type expType) {
        return parseAssignmentMaybe(left, right, operator, expType)
                .orElseThrow(
                    () -> new RuntimeException("Assignment: " + operator + " not supported"));
    }

    static public Optional<Expression> parseAllBinaryMaybe(Expression left, Expression right,
            String operator, Type expType) {
        return parseBinaryOperationMaybe(left, right, operator, expType)
                .or(() -> parseAssignmentMaybe(left, right, operator, expType));
    }

    static public Expression parseAllBinary(Expression left, Expression right, String operator,
            Type expType) {
        return parseAllBinaryMaybe(left, right, operator, expType)
                .orElseThrow(
                    () -> new RuntimeException("Not yet supported binary operation: " + operator));
    }

    static public Optional<Expression> parseUnaryOperationMaybe(Expression uExp, String operator,
            Type expType, boolean prefix) {
        Expression exp = switch (operator) {
            case "++" -> new PlusPlusOperator(uExp, expType, prefix);
            case "--" -> new MinusMinusOperator(uExp, expType, prefix);
            case "~" -> new BitwiseNotOperator(uExp, expType);
            case "!" -> new NotOperator(uExp, expType);
            case "-" -> new NegateOperator(uExp, expType);
            case "delete" -> new DeleteOperator(uExp, expType);
            default -> null;
        };
        return exp == null ? Optional.empty() : Optional.of(exp);
    }

    static public Expression parseUnaryOperation(Expression uExp, String operator, Type expType,
            boolean prefix) {
        return parseUnaryOperationMaybe(uExp, operator, expType, prefix)
                .orElseThrow(
                    () -> new RuntimeException("Not yet supported binary operation: " + operator));
    }
}
