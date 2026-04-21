/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.expressions;

import java.util.Objects;

import org.key_project.logic.SyntaxElement;
import org.key_project.solidity.program.ast.abstractions.Type;
import org.key_project.solidity.program.ast.visitor.Visitor;
import org.key_project.util.ExtList;

import org.jspecify.annotations.NonNull;

// This class is used for expressions like bool(true) where bool is an elementary expression
public class ElementaryExpression extends SolidityExpression {

    public ElementaryExpression(Type type) {
        super(type);
    }

    public ElementaryExpression(ExtList children) {
        super(Objects.requireNonNull(children.removeFirstOccurrence(Type.class)));

    }

    @Override
    public @NonNull SyntaxElement getChild(int n) {
        throw new IndexOutOfBoundsException("There is no children");
    }

    @Override
    public int getChildCount() {
        return 0;
    }

    @Override
    public String toString() {
        return type.toString();
    }

    public void visit(Visitor v) {
        v.performActionOnElementaryExpression(this);
    }
}
