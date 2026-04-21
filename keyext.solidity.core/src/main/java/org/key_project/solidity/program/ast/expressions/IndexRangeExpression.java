/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.expressions;

import java.util.Objects;

import org.key_project.logic.SyntaxElement;
import org.key_project.solidity.program.ast.abstractions.Type;
import org.key_project.solidity.program.ast.visitor.Visitor;
import org.key_project.util.ExtList;

import org.jspecify.annotations.Nullable;

// v[0:2]
public class IndexRangeExpression extends SolidityExpression {

    private final Expression baseExp;
    private final @Nullable Expression startExp;
    private final @Nullable Expression endExp;

    public IndexRangeExpression(Expression baseExp, Expression startExp, Expression endExp,
            Type expType) {
        super(expType);
        this.baseExp = baseExp;
        this.startExp = startExp;
        this.endExp = endExp;
    }

    public IndexRangeExpression(ExtList children, Type type) {
        super(type);
        this.baseExp = Objects.requireNonNull(children.removeFirstOccurrence(Expression.class));
        this.startExp = children.removeFirstOccurrence(Expression.class);
        this.endExp = children.removeFirstOccurrence(Expression.class);
    }

    @Override
    public SyntaxElement getChild(int n) {
        if (n == 0)
            return baseExp;
        n--;
        if (startExp != null) {
            if (n == 0)
                return startExp;
            n--;
        }
        if (endExp != null) {
            if (n == 0)
                return endExp;
            n--;
        }
        throw new IndexOutOfBoundsException(
            " n should be (0 <= " + n + " < " + getChildCount() + ")");
    }

    @Override
    public int getChildCount() {
        int size = 3;
        if (startExp == null)
            size--;
        if (endExp == null)
            size--;
        return size;
    }

    public Expression getBaseExp() { return baseExp; }

    public @Nullable Expression getStartExp() { return startExp; }

    public @Nullable Expression getEndExp() { return endExp; }

    public String toString() {
        return baseExp + "[" + startExp + ":" + endExp + "]";
    }

    public void visit(Visitor v) {
        v.performActionOnIndexRangeExpression(this);
    }
}
