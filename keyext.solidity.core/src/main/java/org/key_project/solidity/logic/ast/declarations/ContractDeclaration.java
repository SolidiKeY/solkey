/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.logic.ast.declarations;

import java.util.List;

import org.key_project.logic.Name;
import org.key_project.logic.SyntaxElement;
import org.key_project.solidity.logic.ast.abstractions.Type;
import org.key_project.util.collection.ImmutableArray;

import org.jspecify.annotations.NonNull;

public class ContractDeclaration extends Declaration implements Type {

    private final ImmutableArray<FieldDeclaration> fields;

    public ContractDeclaration(Name name, List<FieldDeclaration> fields) {
        super(name);
        this.fields = new ImmutableArray<>(fields.toArray(new FieldDeclaration[0]));
    }

    public ImmutableArray<FieldDeclaration> getFieldDeclarations() {
        return fields;
    }

    @Override
    public @NonNull String toString() {
        String contract = "contract ";
        contract += getName() + " {";
        for (int i = 0; i < fields.size(); i++) {
            contract += fields.get(i);
            contract += "\n";
        }
        return contract + "\n}";
    }

    @Override
    public SyntaxElement getChild(int n) {
        return fields.get(n);
    }

    @Override
    public int getChildCount() {
        return fields.size();
    }

}
