/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.references;

import java.util.Objects;

import org.key_project.logic.Name;
import org.key_project.logic.SyntaxElement;
import org.key_project.solidity.program.ast.abstractions.Type;
import org.key_project.solidity.program.ast.declarations.Declaration;
import org.key_project.solidity.program.ast.declarations.EnumDeclaration;
import org.key_project.solidity.program.ast.expressions.Expression;
import org.key_project.solidity.program.ast.visitor.Visitor;
import org.key_project.util.ExtList;

public class EnumReference extends Expression implements VariableReference {
    private final Name name;
    private final EnumDeclaration enumDeclaration;

    public EnumReference(Name name, EnumDeclaration enumDeclaration, Type type) {
        super(type);
        this.name = name;
        this.enumDeclaration = enumDeclaration;
    }

    public EnumReference(ExtList children) {
        super(Objects.requireNonNull(children.removeFirstOccurrence(Type.class)));
        this.name = Objects.requireNonNull(children.removeFirstOccurrence(Name.class));
        this.enumDeclaration =
            Objects.requireNonNull(children.removeFirstOccurrence(EnumDeclaration.class));
    }

    @Override
    public String toString() {
        return name.toString();
    }

    @Override
    public SyntaxElement getChild(int n) {
        return null;
    }

    @Override
    public int getChildCount() {
        return 0;
    }

    public void visit(Visitor v) {
        v.performActionOnEnumReference(this);
    }
}
