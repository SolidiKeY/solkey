/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.abstractions;

import java.util.Objects;

import org.key_project.logic.Name;
import org.key_project.logic.sort.Sort;
import org.key_project.solidity.common.Services;
import org.key_project.util.ExtList;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class KeYSolidityType implements Type {
    /// the AST type
    private @Nullable Type solidityType = null;
    /// the logic sort
    private @Nullable Sort sort = null;

    public KeYSolidityType() {
    }

    public KeYSolidityType(Type solidityType, Sort sort) {
        this.solidityType = solidityType;
        this.sort = sort;
    }

    public KeYSolidityType(ExtList children) {
        this.solidityType = Objects.requireNonNull(children.removeFirstOccurrence(Type.class));;
        this.sort = Objects.requireNonNull(children.removeFirstOccurrence(Sort.class));;
    }

    public KeYSolidityType(Type solidityType) {
        this.solidityType = solidityType;
    }

    public KeYSolidityType(Sort sort) {
        this.sort = sort;
    }

    @Override
    public @Nullable Sort getSort(Services services) {
        return sort;
    }

    public @Nullable Sort getSort() {
        return sort;
    }

    public void setSort(Sort sort) {
        this.sort = sort;
    }

    public @Nullable Type getSolidityType() {
        return solidityType;
    }

    public void setSolidityType(Type solidityType) {
        this.solidityType = solidityType;
    }

    @Override
    public @NonNull Name name() {
        return solidityType == null ? Objects.requireNonNull(sort).name() : solidityType.name();
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (o == this) {
            return true;
        }
        if (o == null || o.getClass() != this.getClass()) {
            return false;
        }
        try {
            return Objects.equals(solidityType, ((KeYSolidityType) o).solidityType)
                    && Objects.equals(sort, ((KeYSolidityType) o).sort);
        } catch (Exception e) {
            return false;
        }
    }

}
