package org.key_project.solidity.program.ast.expressions;

import org.key_project.logic.SyntaxElement;
import org.key_project.solidity.program.ast.abstractions.Type;

public class IndexExpression extends Expression {

    String leftExp;
    Expression indexExp;

    public IndexExpression(String leftExp, Expression indexExp, Type expType) {
        super(expType);
        this.leftExp = leftExp;
        this.indexExp = indexExp;
    }

    @Override
    public SyntaxElement getChild(int n) {
        return null;
    }

    @Override
    public int getChildCount() {
        return 0;
    }

    public String toString(){
        return leftExp + "[" + indexExp + "]";
    }
}
