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

public abstract class UnaryOperator extends SolidityExpression {
    protected final Expression exp;

    protected UnaryOperator(Expression exp, Type type) {
        super(type);
        this.exp = exp;
    }

    public UnaryOperator(ExtList children, Type type) {
        super(type);
        this.exp = Objects.requireNonNull(children.removeFirstOccurrence(Expression.class));
    }

    public UnaryOperator(ExtList children) {
        super(getTypeFromExpression(children));
        this.exp = Objects.requireNonNull(children.removeFirstOccurrence(Expression.class));
    }

    private static Type getTypeFromExpression(ExtList children) {
        Expression exp = children.get(Expression.class);
        if (exp == null) {
            throw new IllegalArgumentException("ExtList must contain an Expression");
        }
        return exp.getType();
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
                return exp;
            default:
                throw new IndexOutOfBoundsException();
        }
    }

    @Override
    public int getChildCount() {
        return 1;
    }

    public Expression getExp() { return exp; }

    public abstract String getOperator();

    public abstract boolean isPrefix();

    public String toString() {
        if (isPrefix())
            return getOperator() + " " + exp;
        else
            return exp + " " + getOperator();
    }
}
