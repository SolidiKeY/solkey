/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.abstractions;

import org.key_project.logic.Name;
import org.key_project.logic.sort.Sort;
import org.key_project.solidity.common.Services;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class MappingType implements Type {

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
        // TODO
        throw new UnsupportedOperationException("To be implemented");
    }
}
