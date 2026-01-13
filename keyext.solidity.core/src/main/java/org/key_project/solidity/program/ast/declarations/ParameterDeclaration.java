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
import org.key_project.util.collection.ImmutableArray;

public class ParameterDeclaration extends Declaration {
    private final @NonNull TypeReference typeReference;
    private final DataLocation dataLocation;
    private final @NonNull Name name;

    public ParameterDeclaration(@NonNull Name name, @NonNull TypeReference typeReference,
            DataLocation dataLocation) {
        super(new ImmutableArray<>());
        this.name = name;
        this.typeReference = typeReference;
        this.dataLocation = dataLocation;
    }

    public ParameterDeclaration(ExtList children) {
        super(Objects.requireNonNull(children.removeFirstOccurrence(ImmutableArray.class)));
        this.name = Objects.requireNonNull(children.removeFirstOccurrence(Name.class));
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
