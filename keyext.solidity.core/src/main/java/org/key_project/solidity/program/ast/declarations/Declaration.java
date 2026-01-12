/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.declarations;

import org.key_project.logic.Name;
import org.key_project.solidity.program.ast.SolidityProgramElement;

import org.jspecify.annotations.NonNull;
import org.key_project.util.collection.ImmutableArray;

public abstract class Declaration implements SolidityProgramElement {
    private final ImmutableArray<Modifier> modifiers;

    protected Declaration(ImmutableArray<Modifier> modifiers) {
        this.modifiers = modifiers;
    }
}
