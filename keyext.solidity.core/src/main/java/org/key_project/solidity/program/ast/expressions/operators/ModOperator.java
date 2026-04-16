/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.expressions.operators;

import org.key_project.solidity.program.ast.abstractions.Type;
import org.key_project.solidity.program.ast.expressions.Expression;
import org.key_project.solidity.program.ast.visitor.Visitor;
import org.key_project.util.ExtList;

public final class ModOperator extends BinaryOperator {

    public ModOperator(Expression left, Expression right, Type type) {
        super(left, right, type);
    }

    public ModOperator(ExtList changeList, Type type) {
        super(changeList, type);
    }

    @Override
    public String getName() { return "%"; }

    public void visit(Visitor v) {
        v.performActionOnModOperator(this);
    }
}
