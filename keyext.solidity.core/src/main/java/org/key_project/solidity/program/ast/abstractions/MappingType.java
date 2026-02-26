/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.abstractions;

import org.key_project.logic.Name;
import org.key_project.logic.SyntaxElement;
import org.key_project.logic.sort.Sort;
import org.key_project.solidity.common.Services;
import org.key_project.solidity.logic.sort.SortImpl;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class MappingType implements Type, SyntaxElement {

    private final Type keyType;
    private final Type valueType;

    public MappingType(Type keyType, Type valueType) {
        this.keyType = keyType;
        this.valueType = valueType;
    }

    @Override
    public @NonNull Name name() {
        return new Name("mapping(" + keyType + " => " + valueType.name() + ")");
    }

    @Override
    public @Nullable Sort getSort(Services services) {
        return new SortImpl(name(), false);
    }

    @Override
    public String toString() {
        return name().toString();
    }

    @Override
    public SyntaxElement getChild(int n) {
        throw new IndexOutOfBoundsException("Mapping type has 0 children");
    }

    @Override
    public int getChildCount() {
        return 0;
    }
}
