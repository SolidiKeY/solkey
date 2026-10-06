/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.rule.metaconstruct;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.key_project.logic.Name;
import org.key_project.solidity.common.Services;
import org.key_project.solidity.logic.op.ProgramVariable;
import org.key_project.solidity.program.ast.SolidityProgramElement;
import org.key_project.solidity.program.ast.declarations.Declaration;
import org.key_project.solidity.program.ast.declarations.FunctionDeclaration;
import org.key_project.solidity.program.ast.declarations.StatementVariableDeclaration;
import org.key_project.solidity.program.ast.expressions.Expression;
import org.key_project.solidity.program.ast.expressions.FunctionCallExpression;
import org.key_project.solidity.program.ast.expressions.operators.AssignExpression;
import org.key_project.solidity.program.ast.expressions.operators.Operator;
import org.key_project.solidity.program.ast.references.FunctionReference;
import org.key_project.solidity.program.ast.statement.Block;
import org.key_project.solidity.program.ast.statement.DeclarationStatement;
import org.key_project.solidity.program.ast.statement.ExpressionStatement;
import org.key_project.solidity.program.ast.statement.FunctionBodyStatement;
import org.key_project.solidity.program.ast.statement.FunctionFrame;
import org.key_project.solidity.program.ast.statement.Statement;
import org.key_project.solidity.program.ast.visitor.ProgVarReplaceVisitor;
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
/// T0 p0 = arg0; ... Tn pn = argn; R0 r0; ... Rm rm; function-frame { <body> } t0 = r0; ...
/// ```
///
/// where `ti` are the call site's targets; a discarded target gets no assignment.
///
/// This mirrors KeY-Java's `MethodCall` metaconstruct and KeY-Rust's `ExpandFnBody`.
public class ExpandFunctionBody extends ProgramTransformer {

    public ExpandFunctionBody(ProgramSV body) {
        super(new Name("expand_function_body"), body);
    }

    public static @Nullable FunctionBodyStatement asFunctionBody(SolidityProgramElement pe) {
        if (pe instanceof FunctionBodyStatement fbs) {
            return fbs;
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
        if (function == null || !function.hasBody() || !function.getModifiers().isEmpty()
                || target != null && function.getReturnParameters().size() != 1) {
            return null;
        }
        List<@Nullable Expression> targets = target == null ? List.of() : List.of(target);
        return new FunctionBodyStatement(targets, reference, call.getArguments(), null);
    }

    @Override
    public SolidityProgramElement[] transform(SolidityProgramElement pe, Services services,
            SVInstantiations svInst) {
        final FunctionBodyStatement fbs = Objects.requireNonNull(asFunctionBody(pe),
            () -> "not a call of a function with a body: " + pe);
        final FunctionDeclaration fn = fbs.getFunction();
        final ImmutableArray<ProgramVariable> formals = fn.getInputParameters();
        final ImmutableArray<ProgramVariable> returns = fn.getReturnParameters();
        final ImmutableArray<Expression> args = fbs.getArguments();
        final List<@Nullable Expression> targets = fbs.getTargets();
        if (!targets.isEmpty() && targets.size() != returns.size()) {
            throw new IllegalStateException(targets.size() + " targets for the "
                + returns.size() + " return values of " + fn.name());
        }

        final Map<ProgramVariable, ProgramVariable> replaceMap = new HashMap<>();
        final List<Statement> stmts = new ArrayList<>(formals.size() + 2 * returns.size() + 1);

        for (int i = 0; i < formals.size(); i++) {
            declareFresh(formals.get(i), formals.get(i).name(), args.get(i), replaceMap, stmts);
        }

        final List<ProgramVariable> freshReturns = new ArrayList<>(returns.size());
        for (int i = 0; i < returns.size(); i++) {
            ProgramVariable ret = returns.get(i);
            Name name = ret.name().toString().isEmpty() ? new Name("ret" + i) : ret.name();
            freshReturns.add(declareFresh(ret, name, null, replaceMap, stmts));
        }

        final ProgVarReplaceVisitor repl =
            new ProgVarReplaceVisitor(fbs.getBody(), replaceMap, true, services);
        repl.start();
        stmts.add(new FunctionFrame(
            ReturnLowering.lower((Block) repl.result(), freshReturns, services).getStatements()));

        for (int i = 0; i < targets.size(); i++) {
            Expression target = targets.get(i);
            if (target != null) {
                stmts.add(new ExpressionStatement(new AssignExpression(
                    Operator.COPY_ASSIGN, target, freshReturns.get(i))));
            }
        }

        return stmts.toArray(new SolidityProgramElement[0]);
    }

    private static ProgramVariable declareFresh(ProgramVariable original, Name name,
            @Nullable Expression initializer, Map<ProgramVariable, ProgramVariable> replaceMap,
            List<Statement> stmts) {
        final ProgramVariable fresh =
            new ProgramVariable(name, original.getKeYSolidityType(), original.getDataLocation());
        replaceMap.put(original, fresh);
        final Declaration decl = new StatementVariableDeclaration(fresh);
        stmts.add(new DeclarationStatement(List.of(decl), initializer));
        return fresh;
    }
}
