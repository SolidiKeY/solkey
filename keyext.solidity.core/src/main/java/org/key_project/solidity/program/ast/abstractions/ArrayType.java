/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.abstractions;

import org.key_project.logic.Name;
import org.key_project.logic.SyntaxElement;
import org.key_project.logic.sort.Sort;
import org.key_project.solidity.common.Services;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class ArrayType implements Type, SyntaxElement, ArrayInterface {
    Type type;
    int length;
    Name name;

    public ArrayType(Type type, int length) {
        this.type = type;
        this.length = length;
        this.name = new Name("Array " + type + " " + length);
    }

    @Override
    public @NonNull Name name() {
        return name;
    }

    @Override
    public @Nullable Sort getSort(Services services) {
        return services.getNamespaces().getArraySort(this);
    }

    @Override
    public SyntaxElement getChild(int n) {
        if (n == 0)
            return type;
        throw new IndexOutOfBoundsException(n + " should be 0");
    }

    @Override
    public int getChildCount() {
        return 1;
    }

    @Override
    public String toString() {
        return type + "[" + length + "]";
    }

    @Override
    public Type type() {
        return type;
    }

    @Override
    public int length() {
        return length;
    }
}
