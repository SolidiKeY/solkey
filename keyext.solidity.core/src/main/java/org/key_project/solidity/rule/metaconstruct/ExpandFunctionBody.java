/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.rule.metaconstruct;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.key_project.logic.Name;
import org.key_project.solidity.common.Services;
import org.key_project.solidity.logic.op.ProgramVariable;
import org.key_project.solidity.program.ast.SolidityProgramElement;
import org.key_project.solidity.program.ast.declarations.Declaration;
import org.key_project.solidity.program.ast.declarations.FunctionDeclaration;
import org.key_project.solidity.program.ast.declarations.StatementVariableDeclaration;
import org.key_project.solidity.program.ast.expressions.Expression;
import org.key_project.solidity.program.ast.expressions.operators.AssignExpression;
import org.key_project.solidity.program.ast.expressions.operators.Operator;
import org.key_project.solidity.program.ast.statement.Block;
import org.key_project.solidity.program.ast.statement.DeclarationStatement;
import org.key_project.solidity.program.ast.statement.ExpressionStatement;
import org.key_project.solidity.program.ast.statement.FunctionBodyStatement;
import org.key_project.solidity.program.ast.statement.Statement;
import org.key_project.solidity.program.ast.visitor.ProgVarReplaceVisitor;
import org.key_project.solidity.rule.matching.inst.SVInstantiations;
import org.key_project.solidity.rule.sv.ProgramSV;
import org.key_project.util.collection.ImmutableArray;

/// Program transformer inlining the body of a [FunctionBodyStatement].
///
/// For each formal input parameter of the target function a *fresh* program variable is
/// declared and initialised with the corresponding actual argument; the function body is then
/// rewritten so that every reference to a formal parameter becomes object-identical to the
/// freshly declared variable (via [ProgVarReplaceVisitor]). The result spliced into the
/// modality is the sequence
///
/// ```
/// T0 p0 = arg0; ... Tn pn = argn; { <body> }
/// ```
///
/// This mirrors KeY-Java's `MethodCall` metaconstruct and KeY-Rust's `ExpandFnBody`.
public class ExpandFunctionBody extends ProgramTransformer {

    public ExpandFunctionBody(ProgramSV body) {
        super(new Name("expand_function_body"), body);
    }

    @Override
    public SolidityProgramElement[] transform(SolidityProgramElement pe, Services services,
            SVInstantiations svInst) {
        final var fbs = (FunctionBodyStatement) pe;
        final FunctionDeclaration fn = fbs.getFunction();
        final ImmutableArray<ProgramVariable> formals = fn.getInputParameters();
        final ImmutableArray<ProgramVariable> returns = fn.getReturnParameters();
        final ImmutableArray<Expression> args = fbs.getArguments();

        final Map<ProgramVariable, ProgramVariable> replaceMap = new HashMap<>();
        final List<Statement> stmts = new ArrayList<>(formals.size() + returns.size() + 2);

        for (int i = 0; i < formals.size(); i++) {
            final ProgramVariable formal = formals.get(i);
            // a fresh variable, identical in name/type/location to the formal parameter
            final ProgramVariable fresh = new ProgramVariable(formal.name(),
                formal.getKeYSolidityType(), formal.getDataLocation());
            replaceMap.put(formal, fresh);

            final Declaration decl = new StatementVariableDeclaration(fresh);
            final Expression arg = args.get(i);
            stmts.add(new DeclarationStatement(List.of(decl), arg));
        }

        // declare a fresh, uninitialised variable for each (named) return parameter so the body
        // can assign to it; remember the first one to connect to the call's result variable.
        final List<ProgramVariable> freshReturns = new ArrayList<>(returns.size());
        for (int i = 0; i < returns.size(); i++) {
            final ProgramVariable ret = returns.get(i);
            final ProgramVariable fresh = new ProgramVariable(ret.name(),
                ret.getKeYSolidityType(), ret.getDataLocation());
            replaceMap.put(ret, fresh);
            freshReturns.add(fresh);

            final Declaration decl = new StatementVariableDeclaration(fresh);
            stmts.add(new DeclarationStatement(List.of(decl), null));
        }

        // rewrite the body so its parameter and named-return references point at the fresh
        // variables
        final ProgVarReplaceVisitor repl =
            new ProgVarReplaceVisitor(fbs.getBody(), replaceMap, true, services);
        repl.start();
        final Block newBody = (Block) repl.result();
        stmts.add(newBody);

        // connect the (single, named) return value to the call's result variable, if any:
        //   <resultVar> = <freshReturn0>;
        final ProgramVariable resultVar = fbs.getResultVar();
        if (resultVar != null && !freshReturns.isEmpty()) {
            stmts.add(new ExpressionStatement(new AssignExpression(
                Operator.COPY_ASSIGN, resultVar, freshReturns.get(0))));
        }

        return stmts.toArray(new SolidityProgramElement[0]);
    }
}
