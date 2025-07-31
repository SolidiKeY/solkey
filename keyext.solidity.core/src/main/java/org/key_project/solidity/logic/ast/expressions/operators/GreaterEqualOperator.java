/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.logic.ast.expressions.operators;

import org.key_project.solidity.logic.ast.TypeResolver;
import org.key_project.solidity.logic.ast.abstractions.Type;
import org.key_project.solidity.logic.ast.expressions.Expression;

public class GreaterEqualOperator extends BinaryOperator {
    public GreaterEqualOperator(Expression left, Expression right) {
        super(left, right);
    }

    @Override
    public String getOperator() { return ">="; }

    @Override
    public Type resolving(TypeResolver resolver) {
        return resolver.resolve(this);
    }
}
