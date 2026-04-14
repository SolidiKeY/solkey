/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.statement;

import org.key_project.logic.SyntaxElement;
import org.key_project.solidity.program.ast.SolidityProgramElement;
import org.key_project.solidity.program.ast.expressions.Expression;
import org.key_project.solidity.program.ast.visitor.Visitor;
import org.key_project.util.ExtList;

import org.jspecify.annotations.NonNull;

public class ForInit implements SolidityProgramElement {
    private final @NonNull Expression init;

    public ForInit(@NonNull Expression init) {
        this.init = init;
    }

    public ForInit(@NonNull ExtList children) {
        this.init = children.get(Expression.class);
    }

    public Expression getInit() {
        return init;
    }

    @Override
    public @NonNull SyntaxElement getChild(int n) {
        if (n == 0) {
            return init;
        }
        throw new IndexOutOfBoundsException();
    }

    @Override
    public int getChildCount() {
        return 1;
    }

    @Override
    public void visit(Visitor v) {
        v.performActionOnForInit(this);
    }

    @Override
    public String toString() {
        return init.toString();
    }
}
