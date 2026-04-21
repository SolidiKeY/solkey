/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.abstractions;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.key_project.logic.Name;
import org.key_project.logic.Namespace;
import org.key_project.logic.SyntaxElement;
import org.key_project.logic.sort.Sort;
import org.key_project.solidity.common.Services;
import org.key_project.solidity.logic.sort.TupleSort;
import org.key_project.util.collection.ImmutableArray;

import org.jspecify.annotations.NonNull;

public class TupleType implements Type {
    private final Name name;
    private final ImmutableArray<Type> types;

    public TupleType(List<Type> types) {
        this.types = new ImmutableArray<>(types);
        this.name = new Name(
            "(" + types.stream().map(Object::toString).collect(Collectors.joining(", ")) + ")");
    }

    @Override
    public @NonNull Sort getSort(Services services) {
        Namespace<@NonNull Sort> sortsSet = services.getNamespaces().sorts();
        Sort sort = sortsSet.lookup(name());
        if (sort == null) {
            List<Sort> sorts = types.stream()
                    .map(type -> Objects.requireNonNull(type.getSort(services)))
                    .toList();
            sort = new TupleSort(sorts);
            sortsSet.add(sort);
        }
        return sort;
    }

    @Override
    public Name name() {
        return name;
    }

    public ImmutableArray<Type> getTypes() { return types; }

    @Override
    public SyntaxElement getChild(int n) {
        if (0 <= n && n < getChildCount())
            return types.get(n);
        throw new IndexOutOfBoundsException("!(0 < " + n + " <= " + getChildCount() + ")");
    }

    @Override
    public int getChildCount() {
        return types.size();
    }
}
