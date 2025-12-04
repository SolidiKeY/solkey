/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.expressions.operators;

import org.key_project.util.ExtList;
import org.key_project.solidity.program.ast.abstractions.Type;
import org.key_project.solidity.program.ast.expressions.Expression;
import org.key_project.solidity.program.ast.visitor.Visitor;

public class NotOperator extends UnaryOperator {
    public NotOperator(Expression exp, Type type) {
        super(exp, type);
    }

    @Override
    public String getOperator() {
        return "!";
    }

    @Override
    public boolean isPrefix() {
        return true;
    }

    public void visit(Visitor v){
        v.performActionOnNotOperator(this);
    }

    public NotOperator(ExtList changeList) {
        super(changeList);
    }
}
