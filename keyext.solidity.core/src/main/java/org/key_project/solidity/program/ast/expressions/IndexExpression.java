/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.expressions;

import org.key_project.logic.SyntaxElement;
import org.key_project.solidity.program.ast.abstractions.Type;
import org.key_project.solidity.program.ast.visitor.Visitor;

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

    public String toString() {
        return leftExp + "[" + indexExp + "]";
    }

    public void visit(Visitor v){
        v.performActionOnIndexExpression(this);
    }
}
