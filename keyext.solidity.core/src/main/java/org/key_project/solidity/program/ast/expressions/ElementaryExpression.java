/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.expressions;

import org.key_project.solidity.program.ast.LeafProgramElement;
import org.key_project.solidity.program.ast.abstractions.Type;
import org.key_project.solidity.program.ast.visitor.Visitor;
import org.key_project.util.ExtList;

import static org.key_project.solidity.program.ast.SolidityProgramElement.takeChild;


// This class is used for expressions like bool(true) where bool is an elementary expression
public class ElementaryExpression extends SolidityExpression implements LeafProgramElement {

    public ElementaryExpression(Type type) {
        super(type);
    }

    public ElementaryExpression(ExtList children) {
        super(takeChild(children, Type.class));

    }

    @Override
    public String toString() {
        return type.toString();
    }

    public void visit(Visitor v) {
        v.performActionOnElementaryExpression(this);
    }

    @Override
    public int computeHashCode() {
        return 37 * super.computeHashCode() + type.hashCode();
    }
}
