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

public class ForUpdate implements SolidityProgramElement {
    private final @NonNull Expression update;

    public ForUpdate(@NonNull Expression update) {
        this.update = update;
    }

    public ForUpdate(@NonNull ExtList children) {
        this.update = children.get(Expression.class);
    }


    @Override
    public @NonNull SyntaxElement getChild(int n) {
        if (n == 0) {
            return update;
        }
        throw new IndexOutOfBoundsException();
    }

    @Override
    public int getChildCount() {
        return 1;
    }

    @Override
    public void visit(Visitor v) {
        v.performActionOnForUpdate(this);
    }

    @Override
    public String toString() {
        return update.toString();
    }

}
