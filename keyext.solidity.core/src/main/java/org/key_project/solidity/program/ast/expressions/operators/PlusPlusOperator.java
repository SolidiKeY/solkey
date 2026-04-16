/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.expressions.operators;

import org.key_project.solidity.program.ast.expressions.Expression;
import org.key_project.solidity.program.ast.visitor.Visitor;
import org.key_project.util.ExtList;

public class PlusPlusOperator extends UnaryBothCases {

    public PlusPlusOperator(Expression exp, boolean prefix) {
        super(exp, exp.getType(), prefix);
    }

    public PlusPlusOperator(ExtList changeList) {
        super(changeList);
    }

    @Override
    public String getName() {
        return "++";
    }

    public void visit(Visitor v) {
        v.performActionOnPlusPlusOperator(this);
    }
}
