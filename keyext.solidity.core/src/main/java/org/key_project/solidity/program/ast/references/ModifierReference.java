/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.references;

import org.key_project.logic.SyntaxElement;

public class ModifierReference implements SyntaxElement {

    private final String name;

    public ModifierReference(String name) {
        this.name = name;
    }

    @Override
    public SyntaxElement getChild(int n) {
        return null;
    }

    @Override
    public int getChildCount() {
        return 0;
    }

    @Override
    public String toString() {
        return name;
    }
}
