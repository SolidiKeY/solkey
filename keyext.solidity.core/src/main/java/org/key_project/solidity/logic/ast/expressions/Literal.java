package org.key_project.solidity.logic.ast.expressions;

import org.key_project.logic.SyntaxElement;
import org.key_project.solidity.logic.ast.abstractions.Type;

public abstract class Literal implements Expression {

    private Type type;

    protected Literal(Type type) {
        this.type = type;
    }

    @Override
    public Type getType() {
        return type;
    }

    @Override
    public SyntaxElement getChild(int n) {
        throw new IndexOutOfBoundsException("Literal has no children");
    }

    @Override
    public int getChildCount() {
        return 0;
    }
}
