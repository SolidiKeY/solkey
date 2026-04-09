/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.expressions.operators;

import org.key_project.solidity.program.ast.abstractions.Type;
import org.key_project.solidity.program.ast.expressions.Expression;
import org.key_project.util.ExtList;

public abstract class AbstractOperator extends BinaryOperator {

    protected AbstractOperator(Expression left, Expression right) {
        super(left, right, left.getType());
    }

    public AbstractOperator(ExtList children, Type type) {
        super(children, type);
    }
}
