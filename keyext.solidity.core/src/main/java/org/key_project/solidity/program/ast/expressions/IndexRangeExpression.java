/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.expressions;

import org.key_project.logic.SyntaxElement;
import org.key_project.solidity.program.ast.abstractions.Type;
import org.key_project.solidity.program.ast.visitor.Visitor;

public class IndexRangeExpression extends Expression {

    private final Expression baseExp;
    private final Expression startExp;
    private final Expression endExp;

    public IndexRangeExpression(Expression baseExp, Expression startExp, Expression endExp,
            Type expType) {
        super(expType);
        this.baseExp = baseExp;
        this.startExp = startExp;
        this.endExp = endExp;
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
        return baseExp + "[" + startExp + ":" + endExp + "]";
    }

    public void visit(Visitor v){
        v.performActionOnIndexRangeExpression(this);
    }
}
