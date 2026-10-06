/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.statement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.key_project.logic.Name;
import org.key_project.logic.SyntaxElement;
import org.key_project.solidity.program.ast.declarations.FunctionDeclaration;
import org.key_project.solidity.program.ast.expressions.Expression;
import org.key_project.solidity.program.ast.references.FunctionReference;
import org.key_project.solidity.program.ast.visitor.Visitor;
import org.key_project.util.ExtList;
import org.key_project.util.collection.ImmutableArray;

import org.jspecify.annotations.Nullable;

/// Placeholder statement standing for the (not yet inlined) body of a Solidity
/// function of a given name and signature. It is the Solidity analogue of KeY-Java's
/// `MethodBodyStatement` / KeY-Rust's `FunctionBodyExpression`.
///
/// It records the target function together with the actual argument expressions of the call
/// site and the targets receiving the function's return values: one per return value, `null`
/// for a value the call site discards, as in `(a, , c) = f(x);`. The `functionBodyExpand`
/// taclet matches such a statement and replaces it by the inlined body via the
/// [org.key_project.solidity.rule.metaconstruct.ExpandFunctionBody] transformer.
///
/// The function is held as a [FunctionReference], so a call to a function declared later in
/// the contract is resolved once the whole contract is parsed.
///
/// Instances are observably immutable: all fields are `final` and only exposed through
/// getters returning immutable views.
public class FunctionBodyStatement implements Statement {

    /// the targets receiving the function's return values, `null` for a discarded one
    private final List<@Nullable Expression> targets;
    /// the function whose body this statement stands for
    private final FunctionReference function;
    /// the name of the contract declaring the function (for display), or `null` if unknown
    private final @Nullable Name contractName;
    /// the actual arguments passed at the call site
    private final ImmutableArray<Expression> arguments;

    public FunctionBodyStatement(List<@Nullable Expression> targets, FunctionReference function,
            ImmutableArray<Expression> arguments, @Nullable Name contractName) {
        this.targets = Collections.unmodifiableList(new ArrayList<>(targets));
        this.function = function;
        this.arguments = arguments;
        this.contractName = contractName;
    }

    public FunctionBodyStatement(List<@Nullable Expression> targets, FunctionDeclaration function,
            ImmutableArray<Expression> arguments, @Nullable Name contractName) {
        this(targets, new FunctionReference(function, function.getType()), arguments,
            contractName);
    }

    public FunctionBodyStatement(ExtList children, FunctionBodyStatement template) {
        List<Expression> expressions = new ArrayList<>();
        Expression expression;
        while ((expression = children.removeFirstOccurrence(Expression.class)) != null) {
            expressions.add(expression);
        }
        this.function = (FunctionReference) expressions.removeFirst();
        List<@Nullable Expression> newTargets = new ArrayList<>(template.targets.size());
        for (Expression target : template.targets) {
            newTargets.add(target == null ? null : expressions.removeFirst());
        }
        this.targets = Collections.unmodifiableList(newTargets);
        this.arguments = new ImmutableArray<>(expressions);
        this.contractName = template.contractName;
    }

    public List<@Nullable Expression> getTargets() {
        return targets;
    }

    public FunctionDeclaration getFunction() {
        return function.mainProgramElement();
    }

    public ImmutableArray<Expression> getArguments() {
        return arguments;
    }

    /// @return the body block of the function this statement stands for
    public Block getBody() {
        return getFunction().getBody();
    }

    private List<Expression> presentTargets() {
        List<Expression> present = new ArrayList<>(targets.size());
        for (Expression target : targets) {
            if (target != null) {
                present.add(target);
            }
        }
        return present;
    }

    // SyntaxElement / SolidityProgramElement -------------------------------------------------

    /// The children are the function reference, the targets that are not discarded and the
    /// actual argument expressions, in this order.
    @Override
    public SyntaxElement getChild(int n) {
        if (n == 0) {
            return function;
        }
        List<Expression> present = presentTargets();
        int i = n - 1;
        if (0 <= i && i < present.size()) {
            return present.get(i);
        }
        i -= present.size();
        if (0 <= i && i < arguments.size()) {
            return arguments.get(i);
        }
        throw outOfBounds(n);
    }

    @Override
    public int getChildCount() {
        return 1 + presentTargets().size() + arguments.size();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (targets.size() == 1 && targets.get(0) != null) {
            sb.append(targets.get(0)).append(" = ");
        } else if (targets.size() > 1) {
            sb.append(targets.stream().map(t -> t == null ? "" : t.toString())
                    .collect(Collectors.joining(", ", "(", ") = ")));
        }
        sb.append(function).append("(")
                .append(arguments.stream().map(Object::toString).collect(Collectors.joining(", ")))
                .append(")@").append(contractName == null ? "" : contractName).append(";");
        return sb.toString();
    }

    @Override
    public void visit(Visitor v) {
        v.performActionOnFunctionBodyStatement(this);
    }
}
