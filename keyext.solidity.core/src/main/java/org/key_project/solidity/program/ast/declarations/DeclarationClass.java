/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.declarations;

import org.key_project.logic.SyntaxElement;
import org.key_project.util.ExtList;
import org.key_project.util.collection.ImmutableArray;

public abstract class DeclarationClass implements Declaration, SyntaxElement {
    final ImmutableArray<Modifier> modifiers;

    protected DeclarationClass(ImmutableArray<Modifier> modifiers) {
        this.modifiers = modifiers;
    }

    public DeclarationClass(ExtList children) {
        modifiers = new ImmutableArray<>(children.collect(Modifier.class));
    }
}
