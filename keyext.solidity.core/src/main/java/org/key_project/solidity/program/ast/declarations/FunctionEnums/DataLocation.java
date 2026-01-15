/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.declarations.FunctionEnums;


import org.key_project.logic.SyntaxElement;
import org.key_project.solidity.program.ast.declarations.Modifier;
import org.key_project.solidity.program.ast.visitor.Visitor;

public enum DataLocation implements Modifier {
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

    @Override
    public SyntaxElement getChild(int n) {
        throw new IndexOutOfBoundsException("Data Location has no children");
    }

    @Override
    public int getChildCount() {
        return 0;
    }

    @Override
    public void visit(Visitor v) {
        v.performActionOnDataLocation(this);
    }
}
