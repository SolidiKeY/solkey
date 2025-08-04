package org.key_project.solidity.program.ast.expressions;

import org.key_project.logic.SyntaxElement;
import org.key_project.solidity.program.ast.abstractions.Type;

public class MemberExp extends Expression {
    final Expression leftExp;
    final String rightName;

    public MemberExp(Expression leftExp, String name, Type type) {
        super(type);
        this.leftExp = leftExp;
        this.rightName = name;
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
    public String toString() {
        return leftExp.toString() + "." + rightName;
    }
}
