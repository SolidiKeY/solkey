/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.logic.ast.expressions.operators;

import org.key_project.logic.SyntaxElement;
import org.key_project.solidity.logic.ast.TypeResolver;
import org.key_project.solidity.logic.ast.abstractions.Type;
import org.key_project.solidity.logic.ast.expressions.Expression;
import org.key_project.solidity.logic.ast.expressions.UnresolvedTypeException;

public abstract class BinaryOperation implements Expression {

    protected final Expression left;
    protected final Expression right;
    protected Type type;

    protected BinaryOperation(Expression left, Expression right) {
        this.left = left;
        this.right = right;
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

    public abstract char getOperator();

    public String toString() {
        return left + " " + getOperator() + " " + right;
    }

    protected abstract Type resolving(TypeResolver resolver);

    public void resolve(TypeResolver resolver) {
        if (type == null) {
            type = resolving(resolver);
        }
    }

}
