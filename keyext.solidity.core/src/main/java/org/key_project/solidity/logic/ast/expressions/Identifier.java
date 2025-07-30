package org.key_project.solidity.logic.ast.expressions;
import org.key_project.logic.SyntaxElement;
import org.key_project.solidity.logic.ast.SolidityProgramElement;
import org.key_project.solidity.logic.ast.abstractions.Type;

public class Identifier implements Expression {
    String name;
    int id;
    Type type;

    public Identifier(String name, int id, Type type) {
        this.name = name;
        this.id = id;
        this.type = type;
    }

    @Override
    public SyntaxElement getChild(int n) {
        return null;
    }

    @Override
    public int getChildCount() {
        return 0;
    }

    @Override
    public Type getType() {
        return null;
    }
}
