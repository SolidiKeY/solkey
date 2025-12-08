/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.declarations;

import java.util.Objects;

import org.key_project.logic.Name;
import org.key_project.logic.SyntaxElement;
import org.key_project.solidity.program.ast.declarations.FunctionEnums.DataLocation;
import org.key_project.solidity.program.ast.references.TypeReference;
import org.key_project.solidity.program.ast.visitor.Visitor;
import org.key_project.util.ExtList;

import org.jspecify.annotations.NonNull;

public class ParameterDeclaration extends Declaration {
    private final int id;
    private final @NonNull TypeReference typeReference;
    private final DataLocation dataLocation;

    public ParameterDeclaration(int id, @NonNull Name name, @NonNull TypeReference typeReference,
            DataLocation dataLocation) {
        super(name);
        this.id = id;
        this.typeReference = typeReference;
        this.dataLocation = dataLocation;
    }

    public ParameterDeclaration(ExtList children) {
        super(Objects.requireNonNull(children.removeFirstOccurrence(Name.class)));
        this.id = Objects.requireNonNull(children.removeFirstOccurrence(int.class));
        this.typeReference =
            Objects.requireNonNull(children.removeFirstOccurrence(TypeReference.class));
        this.dataLocation =
            Objects.requireNonNull(children.removeFirstOccurrence(DataLocation.class));
    }

    @Override
    public SyntaxElement getChild(int n) {
        if (n == 0) {
            return typeReference;
        }
        throw new IndexOutOfBoundsException("Parameter declarations have only one child");
    }

    @Override
    public int getChildCount() {
        return 1;
    }

    @Override
    public String toString() {
        return typeReference + " " + dataLocation + " " + name;
    }

    public @NonNull TypeReference getTypeReference() {
        return typeReference;
    }

    public void visit(Visitor v) {
        v.performActionOnParameterDeclaration(this);
    }
}
