/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.abstractions;

import org.key_project.logic.Name;
import org.key_project.logic.Namespace;
import org.key_project.logic.SyntaxElement;
import org.key_project.logic.sort.Sort;
import org.key_project.solidity.common.Services;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.key_project.solidity.logic.sort.SortImpl;

public class EnumType implements Type, SyntaxElement {

    private final Name name;

    public EnumType(Name name) {
        this.name = name;
    }

    @Override
    public @Nullable Sort getSort(Services services) {
        Namespace<@NonNull Sort> sorts = services.getNamespaces().sorts();
        Sort sort = sorts.lookup(name);
        if(sort == null)
            sorts.add(new SortImpl(name));
        return sorts.lookup(name);
    }

    @Override
    public @NonNull Name name() {
        return name;
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
