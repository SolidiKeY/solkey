/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.references;

import org.key_project.logic.Name;
import org.key_project.solidity.program.ast.abstractions.Type;
import org.key_project.solidity.program.ast.declarations.ArrayDeclaration;

public class ArrayReference extends VariableReference {
    private final Name name;
    private final ArrayDeclaration referencedDeclaration;

    public ArrayReference(Name name, ArrayDeclaration referencedDeclaration,
                          Type type) {
        super(type);
        this.name = name;
        this.referencedDeclaration = referencedDeclaration;
    }

    public ArrayReference(Name name, ArrayDeclaration referencedDeclaration,
                          Name typeName, Type type) {
        super(type);
        this.name = name;
        this.referencedDeclaration = referencedDeclaration;
    }

    @Override
    public ArrayDeclaration getDeclaration() {
        return referencedDeclaration;
    }

    @Override
    public Name getName() {
        return name;
    }

    @Override
    public String toString() {
        return name.toString();
    }
}
