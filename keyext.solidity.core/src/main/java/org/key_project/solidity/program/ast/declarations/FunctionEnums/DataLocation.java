/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.declarations.FunctionEnums;

public enum DataLocation {
    Memory("memory"), Storage("storage"), Calldata("calldata"), Default("default");

    private final String label;

    DataLocation(String label) {
        this.label = label;
    }

    public static DataLocation fromString(String text) {
        for (DataLocation level : DataLocation.values()) {
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
