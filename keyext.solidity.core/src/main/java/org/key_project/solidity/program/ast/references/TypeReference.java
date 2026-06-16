/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.references;


import java.util.Objects;

import org.key_project.logic.Name;
import org.key_project.logic.SyntaxElement;
import org.key_project.solidity.program.ast.SolidityProgramElement;
import org.key_project.solidity.program.ast.abstractions.Type;
import org.key_project.solidity.program.ast.visitor.Visitor;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

// what is the difference between a TypeReference and an ElemnaryExpression?
public class TypeReference implements SolidityProgramElement {
    // TODO: Fix. No public fields.
    public @Nullable Type referencedType;
    public final @Nullable Name typeName;

    public TypeReference(Name typeName) {
        this.typeName = typeName;
        this.referencedType = null;
    }

    // TODO: Look at this
    public TypeReference(Type referencedType) {
        this.referencedType = referencedType;
        this.typeName = null;
    }

    public @Nullable Name getTypeName() {
        return typeName;
    }

    public String toString() {
        return typeName == null ? Objects.requireNonNull(referencedType).name().toString()
                : typeName.toString();
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
    public int computeHashCode() {
        return 7 * Objects.hashCode(typeName);
    }

    public void visit(Visitor v) {
        v.performActionOnTypeReference(this);
    }
}
