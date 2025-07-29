/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.logic.ast.declarations;

import java.util.List;

import org.key_project.logic.Name;

public class ContractDeclaration {

    private final Name name;
    private final FieldDeclaration[] field;

    public ContractDeclaration(Name name, List<FieldDeclaration> fields) {
        this.name = name;
        this.field = fields.toArray(new FieldDeclaration[0]);
    }

    public String toString() {
        String contract = "contract ";
        contract += name.toString() + " {";
        for (int i = 0; i < field.length; i++) {
            contract += field[i].toString();
            contract += "\n";
        }
        return contract + "\n}";
    }
}
