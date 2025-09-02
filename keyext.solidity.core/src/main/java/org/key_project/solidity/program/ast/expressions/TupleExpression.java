package org.key_project.solidity.program.ast.expressions;

import org.key_project.logic.SyntaxElement;
import org.key_project.solidity.program.ast.abstractions.Type;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class TupleExpression extends Expression {
    List<Expression> expressions;

    public TupleExpression(Type type, List<Expression> expressions) {
        super(type);
        this.expressions = expressions;
    }

    @Override
    public SyntaxElement getChild(int n) {
        if(0 <= n && n < expressions.size())
            return expressions.get(n);
        throw new IndexOutOfBoundsException("Not 0 <= " + n + " < " + expressions.size());
    }

    @Override
    public int getChildCount() {
        return expressions.size();
    }

    @Override
    public String toString() {
        return "[" + expressions.stream().map(Objects::toString).collect(Collectors.joining(", ")) + "]";
    }
}
