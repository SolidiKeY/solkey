/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.parser;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

import org.key_project.logic.Name;
import org.key_project.solidity.logic.op.ProgramVariable;
import org.key_project.solidity.program.ast.SolidityInfo;
import org.key_project.solidity.program.ast.StaticTypes;
import org.key_project.solidity.program.ast.abstractions.ArrayType;
import org.key_project.solidity.program.ast.abstractions.DynamicArrayType;
import org.key_project.solidity.program.ast.abstractions.PrimitiveType;
import org.key_project.solidity.program.ast.abstractions.StorageReferenceTypes;
import org.key_project.solidity.program.ast.abstractions.Type;
import org.key_project.solidity.program.ast.declarations.FieldDeclaration;
import org.key_project.solidity.program.ast.declarations.FunctionDeclaration;
import org.key_project.solidity.program.ast.declarations.FunctionEnums.DataLocation;
import org.key_project.solidity.program.ast.declarations.StatementVariableDeclaration;
import org.key_project.solidity.program.ast.expressions.Expression;
import org.key_project.solidity.program.ast.expressions.FunctionCallExpression;
import org.key_project.solidity.program.ast.expressions.MemberExp;
import org.key_project.solidity.program.ast.expressions.literals.BoolLiteral;
import org.key_project.solidity.program.ast.expressions.literals.Uint256Literal;
import org.key_project.solidity.program.ast.expressions.operators.*;
import org.key_project.solidity.program.ast.references.TypeReference;
import org.key_project.solidity.program.ast.statement.DeclarationStatement;
import org.key_project.solidity.program.ast.statement.ExpressionStatement;
import org.key_project.solidity.program.ast.statement.Statement;

import org.jspecify.annotations.Nullable;

public class ParserUtils {

    public static final String MAPPING_COPY_ERROR =
        "Assignments that would copy a mapping (directly, or nested in a struct or array) "
            + "are rejected by solc >= 0.7 and are not supported";

    public static final String MEMORY_MAPPING_ERROR =
        "Memory values of a type containing a mapping cannot exist; solc rejects such "
            + "declarations and they are not supported";

    private static final Set<String> BUILTIN_MEMBERS = Set.of("push", "pop", "transfer", "send");

    public static @Nullable Expression defaultValue(Type type) {
        if (!(type instanceof PrimitiveType primitive)) {
            return null;
        }
        return switch (primitive.kind()) {
            case INTEGER, ADDRESS -> new Uint256Literal(BigInteger.ZERO);
            case BOOLEAN -> BoolLiteral.FALSE;
            default -> null;
        };
    }

    public static <T> List<T> inParameterOrder(List<String> parameters, List<String> names,
            List<T> arguments) {
        if (names.isEmpty()) {
            return arguments;
        }
        if (names.size() != parameters.size() || names.size() != arguments.size()) {
            throw new SolidityParseException("The named arguments " + names
                + " do not match the parameters " + parameters);
        }
        List<T> ordered = new ArrayList<>(arguments.size());
        for (String parameter : parameters) {
            int position = names.indexOf(parameter);
            if (position < 0) {
                throw new SolidityParseException(
                    "No argument is named " + parameter + " in " + names);
            }
            ordered.add(arguments.get(position));
        }
        return ordered;
    }

    public static @Nullable String msgMemberVariable(String member) {
        return switch (member) {
            case "sender" -> "msgSender";
            case "value" -> "msgValue";
            default -> null;
        };
    }

    public static String unsupportedMsgMember(String member) {
        return "Unsupported msg member '" + member
            + "' (only msg.sender and msg.value are modeled)";
    }

    public static @Nullable MemberExp builtinMemberAccess(Expression left, String member) {
        FunctionDeclaration builtin = SolidityInfo.getBuiltinFunctionDeclaration(new Name(member));
        if (builtin != null && BUILTIN_MEMBERS.contains(member)) {
            return new MemberExp(left, builtin, builtin.getType());
        }
        Type leftType = left.getType();
        if ("length".equals(member)
                && (leftType instanceof DynamicArrayType || leftType instanceof ArrayType)) {
            FieldDeclaration sizeField =
                new FieldDeclaration(new Name("size"), new TypeReference(new Name("uint256")));
            return new MemberExp(left, sizeField, PrimitiveType.UINT256);
        }
        return null;
    }

    public static @Nullable Type builtinCallType(MemberExp member, FunctionDeclaration function,
            boolean withArguments) {
        String name = function.name().toString();
        if ("pop".equals(name) || ("push".equals(name) && withArguments)) {
            return PrimitiveType.VOID;
        }
        if ("push".equals(name) && StaticTypes
                .unwrap(member.getLeftExp().getType()) instanceof DynamicArrayType array) {
            return array.getElementType();
        }
        return null;
    }

    private static Optional<Operator> findOperator(String symbol, Predicate<Operator> filter) {
        return Arrays.stream(Operator.values())
                .filter(op -> symbol.equals(op.symbol()) && filter.test(op)).findFirst();
    }

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
        return findOperator(operator, op -> !Operator.isAssignmentOperator(op))
                .map(op -> new BinaryExpression(op, left, right));
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
                && call.isNoArgPush()) {
            if (StorageReferenceTypes.containsMapping(StaticTypes.typeOf(right))) {
                throw new SolidityParseException(MAPPING_COPY_ERROR);
            }
            MemberExp member = (MemberExp) call.getFunctionExp();
            FunctionDeclaration push = (FunctionDeclaration) member.getRightExp();
            return Optional.of(new FunctionCallExpression(push.getType(), member,
                List.of(right)));
        }
        if ("=".equals(operator) && !isStoragePointerRebindTarget(left)
                && StorageReferenceTypes.containsMapping(StaticTypes.typeOf(left))) {
            throw new SolidityParseException(MAPPING_COPY_ERROR);
        }
        return findOperator(operator, Operator::isAssignmentOperator)
                .map(op -> new AssignExpression(op, left, right));
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

    public static List<Statement> tupleAssignment(List<@Nullable Expression> targets,
            List<Expression> values, boolean direct, Function<Expression, ProgramVariable> temp) {
        if (targets.size() != values.size()) {
            throw new SolidityParseException("Tuple assignment of " + values.size()
                + " values to " + targets.size() + " targets");
        }
        List<Statement> evaluations = new ArrayList<>(values.size());
        List<Statement> assignments = new ArrayList<>(values.size());
        for (int i = 0; i < values.size(); i++) {
            Expression target = targets.get(i);
            Expression value = values.get(i);
            if (target == null) {
                if (value instanceof FunctionCallExpression) {
                    evaluations.add(new ExpressionStatement(value));
                }
            } else if (direct) {
                evaluations.add(new ExpressionStatement(parseAssignment(target, value, "=")));
            } else {
                ProgramVariable t = temp.apply(target);
                evaluations.add(new DeclarationStatement(
                    List.of(new StatementVariableDeclaration(t)), value));
                assignments.add(new ExpressionStatement(parseAssignment(target, t, "=")));
            }
        }
        evaluations.addAll(assignments);
        return evaluations;
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
        return findOperator(operator, op -> prefix == op.isPrefix())
                .map(op -> new UnaryExpression(op, uExp));
    }

    static public Expression parseUnaryOperation(Expression uExp, String operator,
            boolean prefix) {
        return parseUnaryOperationMaybe(uExp, operator, prefix)
                .orElseThrow(
                    () -> new RuntimeException("Not yet supported unary operation: " + operator));
    }
}
