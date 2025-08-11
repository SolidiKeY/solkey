/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.declarations;

import org.key_project.logic.Name;
import org.key_project.solidity.program.ast.SolidityProgramElement;

import org.jspecify.annotations.NonNull;

public abstract class Declaration implements SolidityProgramElement {
    protected final @NonNull Name name;

    protected Declaration(@NonNull Name name) {
        this.name = name;
    }

    public @NonNull Name getName() {
        return name;
    }

    @Override
    public String toString() {
        return name.toString();
    }
}
