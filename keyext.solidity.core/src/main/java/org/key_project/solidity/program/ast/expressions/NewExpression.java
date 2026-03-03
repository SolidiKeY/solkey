/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.expressions;

import org.key_project.logic.SyntaxElement;
import org.key_project.solidity.program.ast.abstractions.Type;
import org.key_project.solidity.program.ast.visitor.Visitor;

public class NewExpression extends SolidityExpression {
    static String function;

    public NewExpression(String function, Type type) {
        super(type);
        this.function = function;
    }

    @Override
    public SyntaxElement getChild(int n) {
        throw new IndexOutOfBoundsException("New Expression has no children");
    }

    @Override
    public int getChildCount() {
        return 0;
    }

    @Override
    public void visit(Visitor v) {

    }

    @Override
    public String toString() {
        return function;
    }

    public String getFunction() {
        return function;
    }
}
