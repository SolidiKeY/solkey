/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.logic.ast.declarations;

import org.key_project.logic.Name;
import org.key_project.solidity.logic.ast.SolidityProgramElement;
import org.key_project.solidity.logic.ast.expressions.Expression;
import org.key_project.solidity.logic.ast.references.TypeReference;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class FieldDeclaration extends Declaration {

    private final @NonNull TypeReference typeReference;
    private final @Nullable Expression initializer;


    public FieldDeclaration(@NonNull Name name, @NonNull TypeReference type) {
        super(name);
        this.typeReference = type;
        this.initializer = null;
    }

    public FieldDeclaration(@NonNull Name name, @NonNull TypeReference typeReference,
            @Nullable Expression initializer) {
        super(name);
        this.typeReference = typeReference;
        this.initializer = initializer;
    }

    public TypeReference getTypeReference() {
        return typeReference;
    }

    public @Nullable Expression getInitializer() {
        return initializer;
    }

    // Syntax Element interface
    @Override
    public int getChildCount() {
        return initializer == null ? 1 : 2;
    }

    @Override
    public SolidityProgramElement getChild(int i) {
        if (i < 0 || i >= getChildCount()) {
            throw new IndexOutOfBoundsException("No child at index " + i + " in " + getName());
        }
        if (i == 0) {
            return typeReference;
        }
        if (i == 1) {
            return initializer;
        }
    }


    // common interface
    public String toString() {
        return typeReference + " " + getName() + (initializer != null ? " = " + initializer : "")
            + ";";
    }

}
