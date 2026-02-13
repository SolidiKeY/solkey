package org.key_project.solidity.program.ast.ghost;


import org.key_project.logic.SyntaxElement;
import org.key_project.solidity.program.ast.SolidityProgramElement;
import org.key_project.solidity.program.ast.expressions.Expression;
import org.key_project.solidity.program.ast.visitor.Visitor;

import java.util.List;

public class ExpressionList implements SolidityProgramElement {
    public List<Expression> getExpressions() {
        return expressions;
    }

    List<Expression> expressions;

    public ExpressionList(List<Expression> expressions){
        this.expressions = expressions;
    }

    @Override
    public SyntaxElement getChild(int n) {
        return expressions.get(n);
    }

    @Override
    public int getChildCount() {
        return expressions.size();
    }

    @Override
    public void visit(Visitor v) {
    }


}
