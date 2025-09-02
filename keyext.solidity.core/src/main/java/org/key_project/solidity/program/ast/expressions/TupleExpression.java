package org.key_project.solidity.program.ast.expressions;

import org.key_project.logic.SyntaxElement;
import org.key_project.solidity.program.ast.abstractions.Type;

import java.util.List;

public class TupleExpression extends Expression {
    List<Expression> expressions;

    public TupleExpression(Type type, List<Expression> expressions) {
        super(type);
        this.expressions = expressions;
    }

    @Override
    public SyntaxElement getChild(int n) {
        return null;
    }

    @Override
    public int getChildCount() {
        return 0;
    }
}
