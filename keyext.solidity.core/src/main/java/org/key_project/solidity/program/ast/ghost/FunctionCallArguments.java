package org.key_project.solidity.program.ast.ghost;

import org.key_project.logic.SyntaxElement;
import org.key_project.solidity.program.ast.SolidityProgramElement;
import org.key_project.solidity.program.ast.expressions.Expression;
import org.key_project.solidity.program.ast.visitor.Visitor;

import java.util.List;

public class FunctionCallArguments implements SolidityProgramElement {
    List<Expression> expressions;

    public FunctionCallArguments(ExpressionList expList){
        this.expressions = expList.getExpressions();
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
