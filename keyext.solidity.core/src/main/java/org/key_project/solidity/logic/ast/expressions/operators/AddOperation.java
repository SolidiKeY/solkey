/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.logic.ast.expressions.operators;

import org.key_project.solidity.logic.ast.TypeResolver;
import org.key_project.solidity.logic.ast.abstractions.Type;
import org.key_project.solidity.logic.ast.expressions.Expression;
import org.key_project.solidity.logic.ast.expressions.UnresolvedTypeException;

public class AddOperation extends BinaryOperation {

    private Type type;

    public AddOperation(Expression left, Expression right) {
        super(left, right);
    }

    public void resolve(TypeResolver resolver) {
        if (type == null) {
            type = resolver.resolve(this);
        }
    }

    @Override
    public Type getType() {
        if (type == null) {
            throw new UnresolvedTypeException("Could not determine type of " + this);
        }
        return type;
    }

    @Override
    public String toString() {
        return left + " + " + right;
    }
}
