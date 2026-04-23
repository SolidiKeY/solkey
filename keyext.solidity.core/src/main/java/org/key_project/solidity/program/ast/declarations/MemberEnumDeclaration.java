/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.declarations;

import org.key_project.logic.Name;
import org.key_project.logic.SyntaxElement;
import org.jspecify.annotations.NonNull;

public class MemberEnumDeclaration implements Declaration {

    public @NonNull Name getName() {
        return name;
    }

    private final @NonNull Name name;

    public MemberEnumDeclaration(@NonNull Name name) {
        this.name = name;
    }

    @Override
    public @NonNull SyntaxElement getChild(int n) {
        throw new IndexOutOfBoundsException("Index should be 0 <= " + n + " < " + getChildCount());
    }

    @Override
    public int getChildCount() {
        return 0;
    }

    @Override
    public String toString() {
        return name.toString();
    }
}
