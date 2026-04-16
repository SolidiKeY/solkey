/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.expressions.operators;

import org.key_project.solidity.program.ast.expressions.Expression;
import org.key_project.solidity.program.ast.visitor.Visitor;
import org.key_project.util.ExtList;

public class BitwiseNotOperator extends UnaryOperator {

    public BitwiseNotOperator(Expression exp) {
        super(exp, exp.getType());
    }

    public BitwiseNotOperator(ExtList changeList) {
        super(changeList);
    }

    @Override
    public String getName() {
        return "~";
    }

    @Override
    public boolean isPrefix() {
        return true;
    }

    public void visit(Visitor v) {
        v.performActionOnBitwiseNotOperator(this);
    }
}
