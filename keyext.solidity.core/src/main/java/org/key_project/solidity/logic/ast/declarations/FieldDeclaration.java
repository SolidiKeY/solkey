/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.logic.ast.declarations;

import org.key_project.logic.Name;

public class FieldDeclaration {

    private final Name name;
    // TODO: below should not be a string
    private final String type;

    public FieldDeclaration(Name name, String type) {
        this.name = name;
        this.type = type;
    }

}
