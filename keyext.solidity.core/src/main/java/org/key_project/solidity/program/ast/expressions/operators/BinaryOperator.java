/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.expressions.operators;

import java.util.Objects;

import org.key_project.logic.SyntaxElement;
import org.key_project.solidity.program.ast.abstractions.Type;
import org.key_project.solidity.program.ast.expressions.Expression;
import org.key_project.solidity.program.ast.expressions.SolidityExpression;
import org.key_project.solidity.program.ast.expressions.UnresolvedTypeException;
import org.key_project.util.ExtList;

public abstract class BinaryOperator extends SolidityExpression {

    protected final Expression left;
    protected final Expression right;

    protected BinaryOperator(Expression left, Expression right, Type type) {
        super(type);
        this.left = left;
        this.right = right;
    }

    public BinaryOperator(ExtList children, Type type) {
        super(type);
        this.left = Objects.requireNonNull(children.removeFirstOccurrence(Expression.class));
        this.right = Objects.requireNonNull(children.removeFirstOccurrence(Expression.class));
    }

    @Override
    public Type getType() {
        if (type == null) {
            throw new UnresolvedTypeException("Could not determine type of " + this);
        }
        return type;
    }

    @Override
    public SyntaxElement getChild(int n) {
        switch (n) {
            case 0:
                return left;
            case 1:
                return right;
            default:
                throw new IndexOutOfBoundsException();
        }
    }

    @Override
    public int getChildCount() {
        return 2;
    }

    public Expression getLeft()  { return left; }
    public Expression getRight() { return right; }

    public abstract String getOperator();

    public String toString() {
        return left + " " + getOperator() + " " + right;
    }
}
