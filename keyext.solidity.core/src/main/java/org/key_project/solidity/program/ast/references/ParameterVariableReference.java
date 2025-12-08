/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.references;

import java.util.Objects;

import org.key_project.logic.Name;
import org.key_project.solidity.program.ast.abstractions.Type;
import org.key_project.solidity.program.ast.declarations.ParameterDeclaration;
import org.key_project.solidity.program.ast.visitor.Visitor;
import org.key_project.util.ExtList;

public class ParameterVariableReference extends VariableReference {
    private int id;
    private final Name name;
    private final ParameterDeclaration referencedDeclaration;

    public ParameterVariableReference(int id, Name name, ParameterDeclaration referencedDeclaration,
            Type type) {
        super(type);
        this.id = id;
        this.name = name;
        this.referencedDeclaration = referencedDeclaration;
    }

    public ParameterVariableReference(Name name, ParameterDeclaration referencedDeclaration,
            Name typeName, Type type) {
        super(type);
        this.name = name;
        this.referencedDeclaration = referencedDeclaration;
    }

    public ParameterVariableReference(ExtList children) {
        super(Objects.requireNonNull(children.removeFirstOccurrence(Type.class)));
        this.id = Objects.requireNonNull(children.removeFirstOccurrence(int.class));
        this.name = Objects.requireNonNull(children.removeFirstOccurrence(Name.class));
        this.referencedDeclaration =
            Objects.requireNonNull(children.removeFirstOccurrence(ParameterDeclaration.class));
    }

    @Override
    public ParameterDeclaration getDeclaration() {
        return referencedDeclaration;
    }

    @Override
    public Name name() {
        return name;
    }

    @Override
    public String toString() {
        return name.toString();
    }

    public void visit(Visitor v) {
        v.performActionOnParameterVariableReference(this);
    }
}
