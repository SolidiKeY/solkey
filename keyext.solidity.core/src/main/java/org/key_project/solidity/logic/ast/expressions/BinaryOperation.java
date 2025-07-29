package org.key_project.solidity.logic.ast.expressions;

import org.key_project.logic.SyntaxElement;
import org.key_project.solidity.logic.ast.abstractions.Type;

public abstract class BinaryOperation implements Expression {

    protected final Expression left;
    protected final Expression right;

    protected BinaryOperation(Expression left, Expression right) {
        this.left = left;
        this.right = right;
    }

    @Override
    public abstract Type getType();

    @Override
    public SyntaxElement getChild(int n) {
        switch (n) {
            case 0: return left;
            case 1: return right;
            default: throw new IndexOutOfBoundsException();
        }
    }

    @Override
    public int getChildCount() {
        return 2;
    }
}
