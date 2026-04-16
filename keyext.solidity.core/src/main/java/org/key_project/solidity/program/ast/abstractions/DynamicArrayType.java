/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.abstractions;

import java.util.Objects;

import org.key_project.logic.Name;
import org.key_project.logic.Namespace;
import org.key_project.logic.SyntaxElement;
import org.key_project.logic.sort.Sort;
import org.key_project.solidity.common.Services;
import org.key_project.solidity.logic.sort.DynamicArraySort;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

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
    public @NonNull Sort getSort(Services services) {
        Namespace<@NonNull Sort> sorts = services.getNamespaces().sorts();
        Sort sort = sorts.lookup(name);
        if (sort == null) {
            Sort sortPrim = Objects.requireNonNull(type.getSort(services));
            sort = new DynamicArraySort(sortPrim);
            sorts.add(sort);
        }
        return sort;
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

    public Type getElementType() { return type; }

    @Override
    public String toString() {
        return type.toString() + "[]";
    }
}
