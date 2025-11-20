/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.references;

import org.key_project.logic.Name;
import org.key_project.logic.SyntaxElement;
import org.key_project.solidity.program.ast.SolidityProgramElement;
import org.key_project.solidity.program.ast.TypeResolver;
import org.key_project.solidity.program.ast.abstractions.Type;
import org.key_project.solidity.program.ast.visitor.Visitor;

public class TypeReference implements SolidityProgramElement {

    private Type referencedType;
    private final Name typeName;

    public TypeReference(Type referencedType) {
        this.referencedType = referencedType;
        this.typeName = referencedType.name();
    }

    public TypeReference(Name typeName) {
        this.typeName = typeName;
        this.referencedType = null;
    }

    public Name getTypeName() {
        return typeName;
    }

    public Type getReferencedType() {
        if (!isResolved()) {
            throw new UnresolvedReferenceException(typeName);
        }
        return referencedType;
    }

    private boolean isResolved() {
        return referencedType != null;
    }

    public void resolve(TypeResolver resolver) {
        final Type type = resolver.resolveTypeByName(typeName);
        if (type == null) {
            throw new UnresolvedReferenceException(typeName);
        }
        referencedType = type;
    }

    public String toString() {
        return typeName.toString();
    }

    @Override
    public SyntaxElement getChild(int n) {
        throw new IndexOutOfBoundsException();
    }

    @Override
    public int getChildCount() {
        return 0;
    }

    public void visit(Visitor v){
        v.performActionOnTypeReference(this);
    }
}
