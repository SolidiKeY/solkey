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

import org.jspecify.annotations.NonNull;
import org.key_project.solidity.logic.sort.MappingSort;

public class MappingType implements Type {
    private final Name name;
    private final Type keyType;
    private final Type valueType;

    public MappingType(Type keyType, Type valueType) {
        this.keyType = keyType;
        this.valueType = valueType;
        this.name = new Name("mapping(" + keyType + " => " + valueType + ")");
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
            Sort keySort = Objects.requireNonNull(keyType.getSort(services));
            Sort valueSort = Objects.requireNonNull(valueType.getSort(services));
            sort = new MappingSort(keySort, valueSort);
            sorts.add(sort);
        }
        return sort;
    }

    @Override
    public String toString() {
        return name().toString();
    }

    @Override
    public SyntaxElement getChild(int n) {
        if(n == 0)
            return keyType;
        else if (n == 1) {
            return valueType;
        }
        throw new IndexOutOfBoundsException(n + " should be 0 or 1");
    }

    @Override
    public int getChildCount() {
        return 2;
    }

    public Type keyType() {
        return keyType;
    }

    public Type valueType() {
        return valueType;
    }
}
