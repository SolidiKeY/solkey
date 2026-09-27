/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.declarations.FunctionEnums;


import org.key_project.solidity.program.ast.LeafProgramElement;
import org.key_project.solidity.program.ast.declarations.Modifier;
import org.key_project.solidity.program.ast.visitor.Visitor;


public enum DataLocation implements Modifier, LeafProgramElement {
    Memory("memory"), Storage("storage"), Calldata("calldata"), Default("default");

    private final String label;

    DataLocation(String label) {
        this.label = label;
    }

    public static DataLocation fromString(String text) {
        for (DataLocation level : DataLocation.values()) {
            if (level.label.equalsIgnoreCase(text))
                return level;
        }
        throw new RuntimeException("Datalocation " + text + " does not exists");
    }

    public String getLabel() {
        return label;
    }

    @Override
    public String toString() {
        return label;
    }

    public String noDefaultSpaceRightString() {
        return this.equals(Default) ? "" : this + " ";
    }

    @Override
    public void visit(Visitor v) {
        v.performActionOnDataLocation(this);
    }
}
