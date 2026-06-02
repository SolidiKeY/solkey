/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.abstractions;


import org.key_project.logic.Name;
import org.key_project.logic.SyntaxElement;

import org.jspecify.annotations.NonNull;

public class DynamicArrayType implements Type, SyntaxElement {
    private final Name name;
    private final Type type;

    public DynamicArrayType(Type type) {
        this.type = type;
        this.name = new Name(type + "[]");
    }

    @Override
    public @NonNull Name name() {
        return name;
    }

    @Override
    public @NonNull SyntaxElement getChild(int n) {
        if (n == 0)
            return type;
        throw new IndexOutOfBoundsException("Index should be 0 <= " + n + " < " + getChildCount());
    }

    @Override
    public int getChildCount() {
        return 1;
    }

    public Type getElementType() { return type; }

    @Override
    public String toString() {
        return type.toString() + "[]";
    }
}
