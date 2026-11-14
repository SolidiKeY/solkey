/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.parser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

import org.key_project.logic.Name;
import org.key_project.solidity.common.Services;
import org.key_project.solidity.logic.op.ProgramVariable;
import org.key_project.solidity.program.ast.StaticTypes;
import org.key_project.solidity.program.ast.abstractions.KeYSolidityType;
import org.key_project.solidity.program.ast.abstractions.MappingType;
import org.key_project.solidity.program.ast.abstractions.MemoryReferenceTypes;
import org.key_project.solidity.program.ast.abstractions.PrimitiveType;
import org.key_project.solidity.program.ast.abstractions.Type;
import org.key_project.solidity.program.ast.declarations.EnumDeclaration;
import org.key_project.solidity.program.ast.declarations.FunctionDeclaration;
import org.key_project.solidity.program.ast.declarations.FunctionEnums.DataLocation;
import org.key_project.solidity.program.ast.declarations.StateVariableDeclaration;
import org.key_project.solidity.program.ast.declarations.StatementVariableDeclaration;
import org.key_project.solidity.program.ast.expressions.Expression;
import org.key_project.solidity.program.ast.expressions.FunctionCallExpression;
import org.key_project.solidity.program.ast.expressions.IndexExpression;
import org.key_project.solidity.program.ast.expressions.MemberExp;
import org.key_project.solidity.program.ast.expressions.TupleExpression;
import org.key_project.solidity.program.ast.expressions.literals.Literal;
import org.key_project.solidity.program.ast.expressions.operators.AssignExpression;
import org.key_project.solidity.program.ast.expressions.operators.Operator;
import org.key_project.solidity.program.ast.expressions.operators.TernaryExpression;
import org.key_project.solidity.program.ast.expressions.operators.UnaryExpression;
import org.key_project.solidity.program.ast.references.FieldReference;
import org.key_project.solidity.program.ast.references.FunctionReference;
import org.key_project.solidity.program.ast.statement.DeclarationStatement;
import org.key_project.solidity.program.ast.statement.ExpressionStatement;
import org.key_project.solidity.program.ast.statement.Statement;

import org.jspecify.annotations.Nullable;

public final class TupleAssignment {

    @FunctionalInterface
    public interface Temporaries {
        ProgramVariable declare(Expression typed, @Nullable DataLocation location);
    }

    private final Temporaries temporaries;
    private final List<Statement> statements = new ArrayList<>();
    private final Set<ProgramVariable> writtenLocals =
        Collections.newSetFromMap(new IdentityHashMap<>());
    private final Set<StateVariableDeclaration> writtenFields =
        Collections.newSetFromMap(new IdentityHashMap<>());
    private final Predicate<Expression> storagePointer;

    private TupleAssignment(Temporaries temporaries, Predicate<Expression> storagePointer) {
        this.temporaries = temporaries;
        this.storagePointer = storagePointer;
    }

    public static Temporaries temporaries(Function<Type, KeYSolidityType> types,
            Services services) {
        return (typed, location) -> {
            if (typed instanceof ProgramVariable pv
                    && (location == null || location == pv.getDataLocation())) {
                return new ProgramVariable(new Name(pv.name() + "_tuple"),
                    pv.getKeYSolidityType(), pv.getDataLocation());
            }
            Type type = Objects.requireNonNull(StaticTypes.typeOf(typed),
                () -> "no static type for the tuple component " + typed);
            DataLocation at = location != null ? location
                    : type instanceof PrimitiveType || type instanceof EnumDeclaration
                            ? DataLocation.Default
                            : DataLocation.Memory;
            return new ProgramVariable(new Name("tuple"),
                MemoryReferenceTypes.asLocalVariableType(types.apply(type), at, services), at);
        };
    }

    public static List<Statement> assign(List<@Nullable Expression> targets,
            List<Expression> values, boolean direct, Temporaries temporaries) {
        return assign(targets, values, expression -> false, direct, temporaries);
    }

    public static List<Statement> assign(List<@Nullable Expression> targets,
            List<Expression> values, Predicate<Expression> storagePointer, boolean direct,
            Temporaries temporaries) {
        if (targets.size() != values.size()) {
            throw new SolidityParseException("Tuple assignment of " + values.size()
                + " values to " + targets.size() + " targets");
        }
        TupleAssignment lowering = new TupleAssignment(temporaries, storagePointer);
        if (direct && !lowering.copiesStorageReference(targets, values)) {
            for (int i = 0; i < values.size(); i++) {
                Expression target = targets.get(i);
                if (target == null) {
                    lowering.discard(values.get(i));
                } else {
                    lowering.statements.add(store(target, values.get(i)));
                }
            }
            return lowering.statements;
        }
        List<@Nullable Expression> sources = new ArrayList<>(values.size());
        for (int i = 0; i < values.size(); i++) {
            Expression target = targets.get(i);
            Expression value = values.get(i);
            if (target == null) {
                lowering.discard(value);
                sources.add(null);
                continue;
            }
            ProgramVariable temporary = lowering.storageReference(value)
                    ? temporaries.declare(value, DataLocation.Storage)
                    : temporaries.declare(target, null);
            lowering.declare(temporary, value);
            sources.add(temporary);
        }
        lowering.storeAll(targets, sources);
        return lowering.statements;
    }

    public static List<Statement> store(List<@Nullable Expression> targets,
            List<? extends @Nullable Expression> sources, Temporaries temporaries) {
        TupleAssignment lowering = new TupleAssignment(temporaries, expression -> false);
        lowering.storeAll(targets, sources);
        return lowering.statements;
    }

    private boolean copiesStorageReference(List<@Nullable Expression> targets,
            List<Expression> values) {
        for (int i = 0; i < values.size(); i++) {
            Expression target = targets.get(i);
            if (target != null && rootLocation(target) != DataLocation.Storage
                    && storageReference(values.get(i))) {
                return true;
            }
        }
        return false;
    }

    private void storeAll(List<@Nullable Expression> targets,
            List<? extends @Nullable Expression> sources) {
        for (Expression target : targets) {
            if (target instanceof ProgramVariable pv) {
                writtenLocals.add(pv);
            } else if (target instanceof FieldReference field) {
                writtenFields.add(field.mainProgramElement());
            }
        }
        List<@Nullable Expression> converted = new ArrayList<>(targets.size());
        for (int i = 0; i < targets.size(); i++) {
            Expression target = targets.get(i);
            Expression source = sources.get(i);
            converted.add(target != null && source != null && storageReference(source)
                    && rootLocation(target) != DataLocation.Storage
                            ? hoist(source, DataLocation.Memory)
                            : source);
        }
        List<@Nullable Expression> locations = new ArrayList<>(targets.size());
        for (int i = 0; i < targets.size(); i++) {
            Expression target = targets.get(i);
            Expression location = target == null ? null : location(target);
            if (location != null && rootLocation(location) == DataLocation.Storage
                    && indexesArray(location) && overwritesStorageReference(targets, i)) {
                hoist(location, storageReference(location) ? DataLocation.Storage : null);
            }
            locations.add(location);
        }
        for (int i = targets.size() - 1; i >= 0; i--) {
            Expression location = locations.get(i);
            Expression source = converted.get(i);
            if (location != null && source != null) {
                statements.add(store(location, source));
            }
        }
    }

    private boolean overwritesStorageReference(List<@Nullable Expression> targets, int index) {
        for (int j = 0; j < targets.size(); j++) {
            Expression target = targets.get(j);
            if (j != index && target != null && !(target instanceof ProgramVariable)
                    && storageReference(target)) {
                return true;
            }
        }
        return false;
    }

    private static boolean indexesArray(Expression path) {
        return switch (path) {
            case IndexExpression index -> !(StaticTypes
                    .typeOf(index.getLeftExp()) instanceof MappingType)
                    || indexesArray(index.getLeftExp());
            case MemberExp member -> indexesArray(member.getLeftExp());
            default -> false;
        };
    }

    private Expression location(Expression target) {
        return switch (target) {
            case IndexExpression index -> {
                Expression base = base(index.getLeftExp());
                Expression key = key(index.getIndexExp());
                yield base == index.getLeftExp() && key == index.getIndexExp() ? index
                        : new IndexExpression(base, key);
            }
            case MemberExp member when !(member.getRightExp() instanceof FunctionDeclaration) -> {
                Expression base = base(member.getLeftExp());
                yield base == member.getLeftExp() ? member : member.withLeftExp(base);
            }
            default -> target;
        };
    }

    private Expression base(Expression base) {
        if (base instanceof ProgramVariable pv) {
            return writtenLocals.contains(pv) ? hoist(pv, pv.getDataLocation()) : pv;
        }
        if (base instanceof FunctionCallExpression || base instanceof TernaryExpression) {
            return hoist(base, storageReference(base) ? DataLocation.Storage : null);
        }
        Expression location = location(base);
        return rootLocation(base) == DataLocation.Memory ? hoist(location, DataLocation.Memory)
                : location;
    }

    private Expression key(Expression key) {
        if (key instanceof Literal
                || key instanceof ProgramVariable pv && !writtenLocals.contains(pv)
                || key instanceof FieldReference field
                        && !writtenFields.contains(field.mainProgramElement())) {
            return key;
        }
        return hoist(key, storageReference(key) ? DataLocation.Storage : null);
    }

    private ProgramVariable hoist(Expression value, @Nullable DataLocation location) {
        ProgramVariable temporary = temporaries.declare(value, location);
        declare(temporary, value);
        return temporary;
    }

    private void discard(Expression value) {
        switch (value) {
            case TupleExpression tuple -> tuple.getExpressions().forEach(this::discard);
            case Literal ignored -> {
            }
            case ProgramVariable ignored -> {
            }
            case FieldReference ignored -> {
            }
            case FunctionCallExpression ignored -> statements.add(new ExpressionStatement(value));
            case AssignExpression ignored -> statements.add(new ExpressionStatement(value));
            case UnaryExpression unary when Operator.isAssignmentOperator(unary.getOperator()) ->
                statements.add(new ExpressionStatement(value));
            default -> {
                if (StaticTypes.typeOf(value) == null) {
                    statements.add(new ExpressionStatement(value));
                } else {
                    hoist(value, storageReference(value) ? DataLocation.Storage : null);
                }
            }
        }
    }

    private void declare(ProgramVariable temporary, Expression value) {
        statements.add(new DeclarationStatement(
            List.of(new StatementVariableDeclaration(temporary)), value));
    }

    private static Statement store(Expression target, Expression value) {
        return new ExpressionStatement(ParserUtils.parseAssignment(target, value, "="));
    }

    private boolean storageReference(Expression value) {
        Type type = StaticTypes.typeOf(value);
        return type != null && MemoryReferenceTypes.isReferenceType(type)
                && rootLocation(value) == DataLocation.Storage;
    }

    private @Nullable DataLocation rootLocation(Expression path) {
        if (storagePointer.test(path)) {
            return DataLocation.Storage;
        }
        return switch (path) {
            case ProgramVariable variable -> variable.getDataLocation();
            case FieldReference ignored -> DataLocation.Storage;
            case MemberExp member -> rootLocation(member.getLeftExp());
            case IndexExpression index -> rootLocation(index.getLeftExp());
            case FunctionCallExpression call when call
                    .getFunctionExp() instanceof FunctionReference function
                    && function.getReferencedDeclaration() instanceof FunctionDeclaration callee
                    && callee.getReturnParameters().size() == 1 ->
                callee.getReturnParameters().get(0).getDataLocation();
            case TernaryExpression conditional -> {
                DataLocation location = rootLocation(conditional.getTrueExpression());
                yield location == rootLocation(conditional.getFalseExpression()) ? location
                        : null;
            }
            default -> null;
        };
    }
}
