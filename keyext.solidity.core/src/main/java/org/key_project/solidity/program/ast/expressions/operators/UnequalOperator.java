/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.expressions.operators;

import org.key_project.solidity.program.ast.expressions.Expression;

public final class UnequalOperator extends BinaryOperator {
    public UnequalOperator(Expression left, Expression right) {
        super(left, right);
    }

    @Override
    public String getOperator() { return "!="; }
}
