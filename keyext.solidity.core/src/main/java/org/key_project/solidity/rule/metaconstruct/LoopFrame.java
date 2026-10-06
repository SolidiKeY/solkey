/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.rule.metaconstruct;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.Set;

import org.key_project.logic.SyntaxElement;
import org.key_project.solidity.logic.op.ProgramVariable;
import org.key_project.solidity.program.ast.declarations.FunctionDeclaration;
import org.key_project.solidity.program.ast.declarations.FunctionEnums.DataLocation;
import org.key_project.solidity.program.ast.declarations.StatementVariableDeclaration;
import org.key_project.solidity.program.ast.expressions.Expression;
import org.key_project.solidity.program.ast.expressions.FunctionCallExpression;
import org.key_project.solidity.program.ast.expressions.IndexExpression;
import org.key_project.solidity.program.ast.expressions.MemberExp;
import org.key_project.solidity.program.ast.expressions.NewExpression;
import org.key_project.solidity.program.ast.expressions.TupleExpression;
import org.key_project.solidity.program.ast.expressions.operators.AssignExpression;
import org.key_project.solidity.program.ast.expressions.operators.Operator;
import org.key_project.solidity.program.ast.expressions.operators.UnaryExpression;
import org.key_project.solidity.program.ast.references.FunctionReference;
import org.key_project.solidity.program.ast.statement.FunctionBodyStatement;

import org.jspecify.annotations.Nullable;

public final class LoopFrame {
    private final Set<ProgramVariable> locals = new LinkedHashSet<>();
    private final Set<FunctionDeclaration> visited =
        Collections.newSetFromMap(new IdentityHashMap<>());
    private boolean storage;
    private boolean net;
    private boolean memory;

    private LoopFrame() {}

    public static LoopFrame of(SyntaxElement body) {
        LoopFrame frame = new LoopFrame();
        frame.walk(body);
        return frame;
    }

    public Set<ProgramVariable> locals() {
        return locals;
    }

    public boolean storage() {
        return storage;
    }

    public boolean net() {
        return net;
    }

    /// Whether the body allocates or writes memory, which the invariant rule cannot anonymise.
    public boolean memory() {
        return memory;
    }

    private void walk(SyntaxElement element) {
        switch (element) {
            case AssignExpression assign -> target(assign.getLeft());
            case UnaryExpression unary when isWrite(unary.getOperator()) -> target(unary.getExp());
            case StatementVariableDeclaration declaration -> local(
                declaration.getProgramVariable());
            case NewExpression ignored -> memory = true;
            case FunctionCallExpression call -> call(call);
            case FunctionBodyStatement call -> {
                storage = true;
                net = true;
                for (Expression target : call.getTargets()) {
                    if (target != null) {
                        target(target);
                    }
                }
                callee(call.getFunction());
            }
            default -> {
            }
        }
        for (int i = 0; i < element.getChildCount(); i++) {
            walk(element.getChild(i));
        }
    }

    private static boolean isWrite(Operator operator) {
        return switch (operator) {
            case POST_INC, POST_DEC, PRE_INC, PRE_DEC, DELETE -> true;
            default -> false;
        };
    }

    private void call(FunctionCallExpression call) {
        FunctionDeclaration function = call.getFunctionExp() instanceof FunctionReference reference
                ? reference.getReferencedDeclaration()
                : null;
        if (function != null) {
            switch (function.name().toString()) {
                case "require", "assert", "revert" -> {
                    return;
                }
                default -> callee(function);
            }
        }
        storage = true;
        net = true;
    }

    private void callee(FunctionDeclaration function) {
        if (function.hasBody() && visited.add(function)) {
            walk(function.getBody());
        }
    }

    private void target(Expression target) {
        if (target instanceof TupleExpression tuple) {
            for (Expression component : tuple.getExpressions()) {
                target(component);
            }
            return;
        }
        if (target instanceof ProgramVariable variable) {
            local(variable);
            return;
        }
        ProgramVariable root = root(target);
        if (root != null && root.getDataLocation() == DataLocation.Memory) {
            memory = true;
        } else {
            storage = true;
        }
    }

    private void local(ProgramVariable variable) {
        if (variable.getDataLocation() == DataLocation.Memory) {
            memory = true;
        } else {
            locals.add(variable);
        }
    }

    private static @Nullable ProgramVariable root(Expression path) {
        return switch (path) {
            case ProgramVariable variable -> variable;
            case MemberExp member -> root(member.getLeftExp());
            case IndexExpression index -> root(index.getLeftExp());
            default -> null;
        };
    }
}
