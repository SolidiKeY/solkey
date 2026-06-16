/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.statement;

import java.util.List;
import java.util.stream.Collectors;

import org.key_project.logic.SyntaxElement;
import org.key_project.solidity.logic.op.ProgramVariable;
import org.key_project.solidity.program.ast.declarations.FunctionDeclaration;
import org.key_project.solidity.program.ast.expressions.Expression;
import org.key_project.solidity.program.ast.visitor.Visitor;
import org.key_project.util.collection.ImmutableArray;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/// Placeholder statement standing for the (not yet inlined) body of a Solidity
/// function of a given name and signature. It is the Solidity analogue of KeY-Java's
/// `MethodBodyStatement` / KeY-Rust's `FunctionBodyExpression`.
///
/// It records the target [FunctionDeclaration] together with the actual argument
/// expressions of the call site (and, optionally, the program variable receiving the
/// result). The `functionBodyExpand` taclet matches such a statement and replaces it by
/// the inlined body via the
/// [org.key_project.solidity.rule.metaconstruct.ExpandFunctionBody] transformer.
///
/// Instances are observably immutable: all fields are `final` and only exposed through
/// getters returning immutable views.
public class FunctionBodyStatement implements Statement {

    /// the optional program variable receiving the function's result
    private final @Nullable ProgramVariable resultVar;
    /// the function whose body this statement stands for
    private final @NonNull FunctionDeclaration function;
    /// the actual arguments passed at the call site
    private final @NonNull ImmutableArray<Expression> arguments;

    public FunctionBodyStatement(@Nullable ProgramVariable resultVar,
            @NonNull FunctionDeclaration function, List<Expression> arguments) {
        this(resultVar, function, new ImmutableArray<>(arguments));
    }

    public FunctionBodyStatement(@Nullable ProgramVariable resultVar,
            @NonNull FunctionDeclaration function, @NonNull ImmutableArray<Expression> arguments) {
        this.resultVar = resultVar;
        this.function = function;
        this.arguments = arguments;
    }

    public @Nullable ProgramVariable getResultVar() {
        return resultVar;
    }

    public @NonNull FunctionDeclaration getFunction() {
        return function;
    }

    public @NonNull ImmutableArray<Expression> getArguments() {
        return arguments;
    }

    /// @return the body block of the function this statement stands for
    public @NonNull Block getBody() {
        return function.getBody();
    }

    // SyntaxElement / SolidityProgramElement -------------------------------------------------

    /// The children are the actual argument expressions (optionally preceded by the result
    /// variable). The target [FunctionDeclaration] is part of the surrounding context, not a
    /// traversable child.
    @Override
    public @NonNull SyntaxElement getChild(int n) {
        if (resultVar != null) {
            if (n == 0) {
                return resultVar;
            }
            --n;
        }
        if (0 <= n && n < arguments.size()) {
            return arguments.get(n);
        }
        throw new IndexOutOfBoundsException("Index should be 0 <= " + n + " < " + getChildCount());
    }

    @Override
    public int getChildCount() {
        return (resultVar == null ? 0 : 1) + arguments.size();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (resultVar != null) {
            sb.append(resultVar.name()).append(" = ");
        }
        sb.append(function.name()).append("(")
                .append(arguments.stream().map(Object::toString).collect(Collectors.joining(", ")))
                .append(")@;");
        return sb.toString();
    }

    @Override
    public void visit(Visitor v) {
        v.performActionOnFunctionBodyStatement(this);
    }
}
