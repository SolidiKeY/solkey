/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.expressions;

import java.util.Objects;

import org.key_project.logic.SyntaxElement;
import org.key_project.solidity.program.ast.abstractions.Type;
import org.key_project.solidity.program.ast.visitor.Visitor;
import org.key_project.util.ExtList;


// uint a; a = 4; // ProgramVariable
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

    public IndexRangeExpression(ExtList children) {
        super(Objects.requireNonNull(children.removeFirstOccurrence(Type.class)));
        this.baseExp = Objects.requireNonNull(children.removeFirstOccurrence(Expression.class));
        this.startExp = Objects.requireNonNull(children.removeFirstOccurrence(Expression.class));
        this.endExp = Objects.requireNonNull(children.removeFirstOccurrence(Expression.class));
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

    public void visit(Visitor v) {
        v.performActionOnIndexRangeExpression(this);
    }
}
