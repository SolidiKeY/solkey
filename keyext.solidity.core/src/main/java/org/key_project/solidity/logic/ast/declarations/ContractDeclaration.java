/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.logic.ast.declarations;

import java.util.List;

import org.key_project.logic.Name;
import org.key_project.logic.SyntaxElement;
import org.key_project.solidity.logic.ast.abstractions.Type;

import org.jspecify.annotations.NonNull;

public class ContractDeclaration extends Declaration implements Type {

    private final FieldDeclaration[] field;

    public ContractDeclaration(Name name, List<FieldDeclaration> fields) {
        super(name);
        this.field = fields.toArray(new FieldDeclaration[0]);
    }

    @Override
    public @NonNull String toString() {
        String contract = "contract ";
        contract += getName() + " {";
        for (int i = 0; i < field.length; i++) {
            contract += field[i].toString();
            contract += "\n";
        }
        return contract + "\n}";
    }

    @Override
    public SyntaxElement getChild(int n) {
        return field[n];
    }

    @Override
    public int getChildCount() {
        return field.length;
    }
}
