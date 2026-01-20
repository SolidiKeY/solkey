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
import org.key_project.util.ExtList;

public class TypeReference implements SolidityProgramElement {

    public Type referencedType;
    public final Name typeName;

    public TypeReference(Name typeName) {
        this.typeName = typeName;
        this.referencedType = null;
    }

    public TypeReference(Type referencedType) {
        this.referencedType = referencedType;
        this.typeName = referencedType.name();
    }

    public Name getTypeName() {
        return typeName;
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

    public void visit(Visitor v) {
        v.performActionOnTypeReference(this);
    }
}
