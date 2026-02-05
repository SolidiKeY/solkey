/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.declarations;

import org.key_project.logic.Name;
import org.key_project.logic.SyntaxElement;
import org.key_project.util.collection.ImmutableArray;

import org.jspecify.annotations.NonNull;

public class MemberEnumDeclaration extends Declaration {

    public @NonNull Name getName() {
        return name;
    }

    private final @NonNull Name name;

    public MemberEnumDeclaration(@NonNull Name name) {
        super(new ImmutableArray<>());
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
        return name.toString();
    }
}
