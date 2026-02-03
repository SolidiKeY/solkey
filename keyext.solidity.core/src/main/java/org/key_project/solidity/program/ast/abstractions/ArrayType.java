/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.abstractions;

import java.util.Objects;

import org.key_project.logic.Name;
import org.key_project.logic.SyntaxElement;
import org.key_project.logic.sort.Sort;
import org.key_project.solidity.common.Services;
import org.key_project.solidity.logic.sort.SortImpl;
import org.key_project.util.ExtList;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class ArrayType implements Type, SyntaxElement {

    Type type;
    int length;

    public ArrayType(Type type, int length) {
        this.type = type;
        this.length = length;
    }

    public ArrayType(ExtList children) {
        this.type = Objects.requireNonNull(children.removeFirstOccurrence(Type.class));
        this.length = Objects.requireNonNull(children.removeFirstOccurrence(int.class));;
    }

    @Override
    public @NonNull Name name() {
        return type.name();
    }

    @Override
    public @Nullable Sort getSort(Services services) {
        return new SortImpl(name(), false);
    }

    @Override
    public SyntaxElement getChild(int n) {
        return null;
    }

    @Override
    public int getChildCount() {
        return 0;
    }
}
