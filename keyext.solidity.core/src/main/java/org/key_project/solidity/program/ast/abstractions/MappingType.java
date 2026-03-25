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

public class MappingType implements Type {

    private final Name name;
    private final Type keyType;
    private final Type valueType;

    public MappingType(Type keyType, Type valueType) {
        this.keyType = keyType;
        this.valueType = valueType;
        this.name = new Name("mapping(" + keyType + " => " + valueType.name() + ")");
    }

    @Override
    public @NonNull Name name() {
        return name;
    }

    @Override
    public @Nullable Sort getSort(Services services) {
        throw new RuntimeException("Not implemented yet");
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
