/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.abstractions;

import org.key_project.logic.Name;
import org.key_project.logic.SyntaxElement;

import org.jspecify.annotations.NonNull;

/// Pseudo types are types that do not exist in Solidity but in our logic
/// and for which some program variables exist. Like memory or storage (that is why we need
/// struct)
public class PseudoType implements Type {
    public final static PseudoType MEMORY = new PseudoType("Memory");
    public final static PseudoType IDENTITY = new PseudoType("Identity");
    public final static PseudoType STRUCT = new PseudoType("Struct");

    private final Name name;

    private PseudoType(String name) {
        this.name = new Name(name);
    }

    @Override
    public @NonNull Name name() {
        return name;
    }

    @Override
    public @NonNull SyntaxElement getChild(int n) {
        throw new IndexOutOfBoundsException("Types do not have children");
    }

    @Override
    public int getChildCount() {
        return 0;
    }
}
