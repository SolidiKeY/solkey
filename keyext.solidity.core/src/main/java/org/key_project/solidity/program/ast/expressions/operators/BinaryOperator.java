/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.expressions.operators;

import org.key_project.logic.SyntaxElement;
import org.key_project.solidity.program.ast.TypeResolver;
import org.key_project.solidity.program.ast.abstractions.Type;
import org.key_project.solidity.program.ast.expressions.Expression;
import org.key_project.solidity.program.ast.expressions.UnresolvedTypeException;

public abstract class BinaryOperator implements Expression {

    protected final Expression left;
    protected final Expression right;
    protected Type type;

    protected BinaryOperator(Expression left, Expression right) {
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

    public abstract String getOperator();

    public String toString() {
        return left + " " + getOperator() + " " + right;
    }

    protected Type resolving(TypeResolver resolver) {
        return resolver.resolve(this);
    }

    public void resolve(TypeResolver resolver) {
        if (type == null) {
            type = resolving(resolver);
        }
    }

}
