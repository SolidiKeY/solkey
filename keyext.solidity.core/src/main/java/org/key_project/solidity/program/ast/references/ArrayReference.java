/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.references;

import java.util.Objects;

import org.key_project.logic.SyntaxElement;
import org.key_project.solidity.program.ast.abstractions.Type;
import org.key_project.solidity.program.ast.declarations.ArrayDeclaration;
import org.key_project.solidity.program.ast.expressions.SolidityExpression;
import org.key_project.solidity.program.ast.visitor.Visitor;
import org.key_project.util.ExtList;

public class ArrayReference extends SolidityExpression implements VariableReference {
    private final ArrayDeclaration referencedDeclaration;

    public ArrayReference(ArrayDeclaration referencedDeclaration,
            Type type) {
        super(type);
        this.referencedDeclaration = referencedDeclaration;
    }

    public ArrayReference(ExtList children, Type type) {
        super(type);
        this.referencedDeclaration =
            Objects.requireNonNull(children.removeFirstOccurrence(ArrayDeclaration.class));
    }

    @Override
    public String toString() {
        return referencedDeclaration.programVariable.toString();
    }

    @Override
    public SyntaxElement getChild(int n) {
        if (n == 0)
            return referencedDeclaration;
        throw new IndexOutOfBoundsException();
    }

    @Override
    public int getChildCount() {
        return 1;
    }

    public void visit(Visitor v) {
        v.performActionOnArrayReference(this);
    }
}
