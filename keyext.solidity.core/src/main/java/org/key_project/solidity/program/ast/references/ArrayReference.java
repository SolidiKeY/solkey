/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.references;

import java.util.Objects;

import org.key_project.logic.Name;
import org.key_project.logic.SyntaxElement;
import org.key_project.solidity.program.ast.abstractions.Type;
import org.key_project.solidity.program.ast.declarations.ArrayDeclaration;
import org.key_project.solidity.program.ast.expressions.Expression;
import org.key_project.solidity.program.ast.visitor.Visitor;
import org.key_project.util.ExtList;

public class ArrayReference extends Expression implements VariableReference {
    private int id;
    private final Name name;
    private final ArrayDeclaration referencedDeclaration;

    public ArrayReference(int id, Name name, ArrayDeclaration referencedDeclaration,
            Type type) {
        super(type);
        this.id = id;
        this.name = name;
        this.referencedDeclaration = referencedDeclaration;
    }

    public ArrayReference(Name name, ArrayDeclaration referencedDeclaration,
            Name typeName, Type type) {
        super(type);
        this.name = name;
        this.referencedDeclaration = referencedDeclaration;
    }

    public ArrayReference(ExtList children) {
        super(Objects.requireNonNull(children.removeFirstOccurrence(Type.class)));
        this.id = Objects.requireNonNull(children.removeFirstOccurrence(int.class));
        this.name = Objects.requireNonNull(children.removeFirstOccurrence(Name.class));
        this.referencedDeclaration =
            Objects.requireNonNull(children.removeFirstOccurrence(ArrayDeclaration.class));
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
        v.performActionOnArrayReference(this);
    }
}
