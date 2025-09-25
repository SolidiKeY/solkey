package org.key_project.solidity.program.ast.expressions;

import org.key_project.logic.SyntaxElement;
import org.key_project.solidity.program.ast.abstractions.Type;

import java.util.List;
import java.util.stream.Collectors;

public class FunctionCallExpression extends Expression {

    private final List<Expression> arguments;
    public final Expression functionExp;

    public FunctionCallExpression(Type type, Expression functionExp, List<Expression> arguments) {
        super(type);
        this.functionExp = functionExp;
        this.arguments = arguments;
    }

    @Override
    public SyntaxElement getChild(int n) {
        if(0 <= n && n < arguments.size()){
            return arguments.get(n);
        }
        if(n == arguments.size())
            return functionExp;
        throw new IndexOutOfBoundsException("Not 0 <= " + n + " < " + getChildCount());
    }

    @Override
    public int getChildCount() {
        return arguments.size() + 1;
    }

    @Override
    public String toString() {
        return functionExp + "(" + arguments.stream().map(Object::toString).collect(Collectors.joining(", ")) + ")";
    }
}
