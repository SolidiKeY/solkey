/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.rule.metaconstruct;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.key_project.logic.Name;
import org.key_project.solidity.common.Services;
import org.key_project.solidity.logic.op.ProgramVariable;
import org.key_project.solidity.program.ast.SolidityProgramElement;
import org.key_project.solidity.program.ast.abstractions.PrimitiveType;
import org.key_project.solidity.program.ast.declarations.ContractDeclaration;
import org.key_project.solidity.program.ast.declarations.FunctionDeclaration;
import org.key_project.solidity.program.ast.declarations.StateVariableDeclaration;
import org.key_project.solidity.program.ast.expressions.Expression;
import org.key_project.solidity.program.ast.expressions.FunctionCallExpression;
import org.key_project.solidity.program.ast.expressions.literals.BoolLiteral;
import org.key_project.solidity.program.ast.expressions.literals.Uint256Literal;
import org.key_project.solidity.program.ast.expressions.operators.AssignExpression;
import org.key_project.solidity.program.ast.expressions.operators.Operator;
import org.key_project.solidity.program.ast.references.FieldReference;
import org.key_project.solidity.program.ast.references.FunctionReference;
import org.key_project.solidity.program.ast.statement.ExpressionStatement;
import org.key_project.solidity.program.ast.statement.FunctionBodyStatement;
import org.key_project.solidity.program.ast.statement.FunctionFrame;
import org.key_project.solidity.program.ast.statement.Statement;
import org.key_project.solidity.rule.matching.inst.SVInstantiations;
import org.key_project.solidity.rule.sv.ProgramSV;
import org.key_project.util.collection.ImmutableArray;

import org.jspecify.annotations.Nullable;

/// Program transformer inlining the body of a [FunctionBodyStatement], or of an internal call
/// statement `f(args);` / `lhs = f(args);` (see [#asFunctionBody]).
///
/// For each formal input parameter of the target function a *fresh* program variable is
/// declared and initialised with the corresponding actual argument; the function body is then
/// rewritten so that every reference to a formal parameter becomes object-identical to the
/// freshly declared variable (via [ProgVarReplaceVisitor]), and each `return e;` becomes
/// `{ r0 = e; return; }` (via [ReturnLowering]). The body runs in a [FunctionFrame], at which a
/// `return;` completes. The result spliced into the modality is the sequence
///
/// ```
/// T0 p0 = arg0; ... Tn pn = argn; R0 r0 = 0; ... Rm rm = 0; function-frame { <body> } t0 = r0; ...
/// ```
///
/// where `ti` are the call site's targets; a discarded target gets no assignment. A return
/// variable of integer or `bool` type starts at its default, `0` or `false`. The function's
/// modifiers are wrapped around the frame (see [ModifierInlining]).
///
/// This mirrors KeY-Java's `MethodCall` metaconstruct and KeY-Rust's `ExpandFnBody`.
public class ExpandFunctionBody extends ProgramTransformer {

    public ExpandFunctionBody(ProgramSV body) {
        super(new Name("expand_function_body"), body);
    }

    public static @Nullable FunctionBodyStatement asFunctionBody(SolidityProgramElement pe) {
        if (pe instanceof FunctionBodyStatement fbs) {
            return inlinable(fbs.getFunction()) ? fbs : null;
        }
        if (!(pe instanceof ExpressionStatement statement)) {
            return null;
        }
        Expression expression = statement.getExpression();
        Expression target = null;
        if (expression instanceof AssignExpression assign
                && assign.getOperator() == Operator.COPY_ASSIGN) {
            target = assign.getLeft();
            expression = assign.getRight();
        }
        if (!(expression instanceof FunctionCallExpression call)
                || !(call.getFunctionExp() instanceof FunctionReference reference)) {
            return null;
        }
        FunctionDeclaration function = reference.getReferencedDeclaration();
        if (function == null || !inlinable(function)
                || target != null && function.getReturnParameters().size() != 1) {
            return null;
        }
        List<@Nullable Expression> targets = target == null ? List.of() : List.of(target);
        return new FunctionBodyStatement(targets, reference, call.getArguments(),
            reference.getDispatchContract());
    }

    public static boolean inlinable(FunctionDeclaration function) {
        return function.hasBody()
                && function.getModifiers().stream().allMatch(ModifierInlining::inlinable);
    }

    @Override
    public SolidityProgramElement[] transform(SolidityProgramElement pe, Services services,
            SVInstantiations svInst) {
        final FunctionBodyStatement call = Objects.requireNonNull(asFunctionBody(pe),
            () -> "not a call of a function with a body: " + pe);
        final FunctionDeclaration fn = dispatch(call, services);
        if (!inlinable(fn)) {
            throw new IllegalStateException(fn.name() + " has a modifier that cannot be inlined");
        }
        final List<@Nullable Expression> targets = call.getTargets();
        if (!targets.isEmpty() && targets.size() != fn.getReturnParameters().size()) {
            throw new IllegalStateException(targets.size() + " targets for the "
                + fn.getReturnParameters().size() + " return values of " + fn.name());
        }

        final FreshVariables fresh = new FreshVariables();
        final List<Statement> stmts = new ArrayList<>();
        final ImmutableArray<ProgramVariable> formals = fn.getInputParameters();
        for (int i = 0; i < formals.size(); i++) {
            fresh.declare(formals.get(i), formals.get(i).name(), call.getArguments().get(i),
                stmts);
        }
        final List<ProgramVariable> results = declareReturns(fn, fresh, stmts);
        fresh.addLocalsOf(fn.getBody());

        if (fn.getKind().equals("constructor")) {
            stmts.addAll(stateVariableInitializers(fn, services));
        }
        stmts.add((Statement) fresh.rename(frame(fn, services), call.getContractName(),
            services));
        stmts.addAll(assignResults(targets, results));
        return stmts.toArray(new SolidityProgramElement[0]);
    }

    private static FunctionDeclaration dispatch(FunctionBodyStatement call, Services services) {
        final Name contractName = call.getContractName();
        final ContractDeclaration contract =
            contractName == null ? null : services.getSolidityInfo().getContract(contractName);
        return contract == null ? call.getFunction() : contract.dispatch(call.getFunction());
    }

    private static List<ProgramVariable> declareReturns(FunctionDeclaration fn,
            FreshVariables fresh, List<Statement> stmts) {
        final ImmutableArray<ProgramVariable> returns = fn.getReturnParameters();
        final List<ProgramVariable> results = new ArrayList<>(returns.size());
        for (int i = 0; i < returns.size(); i++) {
            ProgramVariable ret = returns.get(i);
            Name name = ret.name().toString().isEmpty() ? new Name("ret" + i) : ret.name();
            results.add(fresh.declare(ret, name, zero(ret), stmts));
        }
        return results;
    }

    private static Statement frame(FunctionDeclaration fn, Services services) {
        return ModifierInlining.wrap(fn.getModifiers(), new FunctionFrame(LoopLowering.lower(
            ReturnLowering.lower(fn.getBody(), fn.getReturnParameters().toList(), services),
            services).getStatements()), services);
    }

    private static List<Statement> assignResults(List<@Nullable Expression> targets,
            List<ProgramVariable> results) {
        final List<Statement> assignments = new ArrayList<>(targets.size());
        for (int i = 0; i < targets.size(); i++) {
            Expression target = targets.get(i);
            if (target != null) {
                assignments.add(new ExpressionStatement(new AssignExpression(
                    Operator.COPY_ASSIGN, target, results.get(i))));
            }
        }
        return assignments;
    }

    private static List<Statement> stateVariableInitializers(FunctionDeclaration constructor,
            Services services) {
        final List<Statement> initializers = new ArrayList<>();
        for (ContractDeclaration contract : services.getSolidityInfo().getContracts()) {
            if (!contract.getFunctions().contains(constructor)) {
                continue;
            }
            for (StateVariableDeclaration field : contract.getFieldDeclarations()) {
                Expression initializer = field.getInitializer();
                if (initializer != null) {
                    initializers.add(new ExpressionStatement(new AssignExpression(
                        Operator.COPY_ASSIGN, new FieldReference(field, field.getType()),
                        initializer)));
                }
            }
        }
        return initializers;
    }

    private static @Nullable Expression zero(ProgramVariable variable) {
        if (!(variable.getType() instanceof PrimitiveType type)) {
            return null;
        }
        return switch (type.kind()) {
            case INTEGER -> new Uint256Literal(BigInteger.ZERO);
            case BOOLEAN -> BoolLiteral.FALSE;
            default -> null;
        };
    }
}
