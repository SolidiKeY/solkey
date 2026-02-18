/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.ghost;


import java.util.List;

import org.key_project.logic.SyntaxElement;
import org.key_project.solidity.program.ast.SolidityProgramElement;
import org.key_project.solidity.program.ast.expressions.Expression;
import org.key_project.solidity.program.ast.visitor.Visitor;

public class ExpressionList implements SolidityProgramElement {
    public List<Expression> getExpressions() {
        return expressions;
    }

    List<Expression> expressions;

    public ExpressionList(List<Expression> expressions) {
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
