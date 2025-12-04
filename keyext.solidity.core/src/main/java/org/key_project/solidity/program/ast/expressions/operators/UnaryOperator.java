/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.expressions.operators;

import org.key_project.logic.SyntaxElement;
import org.key_project.solidity.program.ast.TypeResolver;
import org.key_project.solidity.program.ast.abstractions.Type;
import org.key_project.solidity.program.ast.expressions.Expression;
import org.key_project.solidity.program.ast.expressions.UnresolvedTypeException;
import org.key_project.util.ExtList;

import java.util.Objects;

public abstract class UnaryOperator extends Expression {

    protected final Expression exp;
    protected Type type;

    protected UnaryOperator(Expression exp, Type type) {
        super(type);
        this.exp = exp;
    }

    public UnaryOperator(ExtList children) {
        super(Objects.requireNonNull(children.removeFirstOccurrence(Type.class)));
        this.exp = Objects.requireNonNull(children.removeFirstOccurrence(Expression.class));
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

    public abstract String getOperator();

    public abstract boolean isPrefix();

    public String toString() {
        if (isPrefix())
            return getOperator() + " " + exp;
        else
            return exp + " " + getOperator();
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
