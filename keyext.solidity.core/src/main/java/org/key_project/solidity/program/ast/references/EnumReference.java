/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.references;

import java.util.Objects;

import org.key_project.logic.SyntaxElement;
import org.key_project.solidity.program.ast.abstractions.Type;
import org.key_project.solidity.program.ast.declarations.EnumDeclaration;
import org.key_project.solidity.program.ast.expressions.SolidityExpression;
import org.key_project.solidity.program.ast.visitor.Visitor;
import org.key_project.util.ExtList;

import org.jspecify.annotations.NonNull;

public class EnumReference extends SolidityExpression implements VariableReference {
    private final EnumDeclaration enumDeclaration;

    public EnumReference(EnumDeclaration enumDeclaration, Type type) {
        super(type);
        this.enumDeclaration = enumDeclaration;
    }

    public EnumReference(ExtList children, Type type) {
        super(type);
        this.enumDeclaration =
            Objects.requireNonNull(children.removeFirstOccurrence(EnumDeclaration.class));
    }

    @Override
    public String toString() {
        return enumDeclaration.getName().toString();
    }

    @Override
    public @NonNull SyntaxElement getChild(int n) {
        throw new IndexOutOfBoundsException("Index should be 0 <= " + n + " < " + getChildCount());
    }

    @Override
    public int getChildCount() {
        return 0;
    }

    @Override
    public EnumDeclaration mainProgramElement() {
        return enumDeclaration;
    }

    public void visit(Visitor v) {
        v.performActionOnEnumReference(this);
    }
}
