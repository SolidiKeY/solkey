package org.key_project.solidity.logic.ast.expressions;

import org.key_project.logic.SyntaxElement;
import org.key_project.solidity.logic.ast.abstractions.Type;

public class Identifier implements Expression {
    private Type type;

    public Identifier(Type type) { this.type = type; }

    @Override
    public Type getType() { return type; }

    @Override
    public SyntaxElement getChild(int n) {
        throw new IndexOutOfBoundsException("Identifier has no children");
    }

    @Override
    public int getChildCount() { return 0; }
}
