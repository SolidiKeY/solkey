package org.key_project.solidity.program.ast.statement;

import org.key_project.logic.SyntaxElement;
import org.key_project.solidity.program.ast.expressions.Expression;
import org.key_project.util.ExtList;

import java.util.Objects;

public abstract class LoopStatement implements Statement {
    protected final Expression condition;
    protected final Statement body;

    protected LoopStatement(Expression condition, Statement body) {
        this.condition = condition;
        this.body = body;
    }

    public LoopStatement(ExtList children) {
        this.condition = Objects.requireNonNull(children.removeFirstOccurrence(Expression.class));
        this.body = Objects.requireNonNull(children.removeFirstOccurrence(Statement.class));
    }

    @Override
    public SyntaxElement getChild(int n) {
        return switch (n) {
            case 0 -> condition;
            case 1 -> body;
            default -> throw new IndexOutOfBoundsException(
                    "Index should be 0 <= " + n + " < " + getChildCount());
        };
    }

    @Override
    public int getChildCount() {
        return 2;
    }

    public Expression getCondition() {
        return condition;
    }
}
