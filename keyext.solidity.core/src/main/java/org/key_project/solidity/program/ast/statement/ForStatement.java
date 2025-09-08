package org.key_project.solidity.program.ast.statement;

import org.jetbrains.annotations.NotNull;
import org.key_project.logic.SyntaxElement;
import org.key_project.solidity.program.ast.expressions.Expression;

public class ForStatement implements Statement {
    private final Expression initializationExpression;
    private final Expression condition;
    private final Expression loopExpression;
    private final Statement body;

    public ForStatement(Expression initializationExpression, Expression condition, Expression loopExpression, Statement body) {
        this.initializationExpression = initializationExpression;
        this.condition = condition;
        this.loopExpression = loopExpression;
        this.body = body;
    }

    @Override
    public @NotNull SyntaxElement getChild(int n) {
        return switch (n){
            case 0 -> initializationExpression;
            case 1 -> condition;
            case 2 -> loopExpression;
            case 3 -> body;
            default -> throw new IndexOutOfBoundsException("Index should be 0 <= " + n + " < " + getChildCount());
        };
    }

    @Override
    public int getChildCount() {
        return 4;
    }

    @Override
    public String toString() {
        return "for(" + initializationExpression + "; " + condition + "; " + loopExpression + ")\n" + body;
    }
}
