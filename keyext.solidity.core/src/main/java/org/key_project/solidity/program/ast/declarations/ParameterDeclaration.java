/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.declarations;

import org.key_project.logic.Name;
import org.key_project.logic.SyntaxElement;
import org.key_project.solidity.program.ast.references.TypeReference;

import org.jspecify.annotations.NonNull;

public class ParameterDeclaration extends Declaration {
    private final @NonNull TypeReference typeReference;

    public ParameterDeclaration(@NonNull Name name, @NonNull TypeReference typeReference) {
        super(name);
        this.typeReference = typeReference;
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

    public @NonNull TypeReference getTypeReference() {
        return typeReference;
    }
}
