/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.declarations;

import org.key_project.logic.Name;
import org.key_project.logic.SyntaxElement;

import org.jspecify.annotations.NonNull;

public class MemberEnumDeclaration extends Declaration {

    public MemberEnumDeclaration(@NonNull Name name) {
        super(name);
    }

    @Override
    public SyntaxElement getChild(int n) {
        return null;
    }

    @Override
    public int getChildCount() {
        return 0;
    }
}
