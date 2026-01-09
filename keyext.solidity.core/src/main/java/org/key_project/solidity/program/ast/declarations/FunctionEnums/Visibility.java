/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.declarations.FunctionEnums;

import org.key_project.solidity.program.ast.declarations.Modifier;

public enum Visibility implements Modifier {
    internal("internal"), external("external"), Private("private"), Public("public");

    private final String label;

    Visibility(String label) {
        this.label = label;
    }


    public static Visibility fromString(String text) {
        for (Visibility level : Visibility.values()) {
            if (level.label.equalsIgnoreCase(text)) {
                return level;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return label;
    }

}
