/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.references;


import java.util.Objects;

import org.key_project.logic.Name;
import org.key_project.logic.SyntaxElement;
import org.key_project.solidity.program.ast.SolidityInfo;
import org.key_project.solidity.program.ast.SolidityProgramElement;
import org.key_project.solidity.program.ast.abstractions.Type;
import org.key_project.solidity.program.ast.visitor.Visitor;

import org.jspecify.annotations.Nullable;

// what is the difference between a TypeReference and an ElemnaryExpression?
public class TypeReference implements SolidityProgramElement {
    private final @Nullable Type referencedType;
    private final @Nullable Name typeName;

    public TypeReference(Name typeName) {
        this.typeName = typeName;
        this.referencedType = null;
    }

    public TypeReference(@Nullable Type referencedType) {
        this.referencedType = referencedType;
        this.typeName = referencedType != null ? referencedType.name() : null;
    }

    public @Nullable Name getTypeName() {
        return typeName;
    }

    public @Nullable Type getReferencedType() {
        return referencedType;
    }

    /// the referenced type if resolved, otherwise the predefined primitive type named by
    /// [#getTypeName] (or `null` if neither is available)
    public @Nullable Type resolvedType() {
        if (referencedType != null) {
            return referencedType;
        }
        return typeName != null ? SolidityInfo.getPrimitiveType(typeName.toString()) : null;
    }

    public String toString() {
        if (typeName != null) {
            return typeName.toString();
        }
        return referencedType == null ? "<unknown type>" : referencedType.name().toString();
    }

    @Override
    public SyntaxElement getChild(int n) {
        throw outOfBounds(n);
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
