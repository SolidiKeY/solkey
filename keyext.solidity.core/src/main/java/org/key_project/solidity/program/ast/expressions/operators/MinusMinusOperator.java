/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.expressions.operators;

import org.key_project.solidity.program.ast.expressions.Expression;
import org.key_project.solidity.program.ast.visitor.Visitor;
import org.key_project.util.ExtList;

public class MinusMinusOperator extends UnaryBothCases {
    public MinusMinusOperator(Expression exp, boolean prefix) {
        super(exp, exp.getType(), prefix);
    }

    public MinusMinusOperator(ExtList changeList) {
        super(changeList);
    }

    @Override
    public String getOperator() {
        return "--";
    }

    public void visit(Visitor v) {
        v.performActionOnMinusMinusOperator(this);
    }
}
