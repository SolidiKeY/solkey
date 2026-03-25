/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.abstractions;

import java.util.HashMap;

import org.key_project.logic.Name;
import org.key_project.logic.SyntaxElement;
import org.key_project.logic.sort.Sort;
import org.key_project.solidity.common.Services;
import org.key_project.solidity.program.ast.Resolver;

import org.jspecify.annotations.NonNull;

public class StructType implements Type, Resolver {
    private final @NonNull Name contractName;
    private final @NonNull Name name;

    public StructType(@NonNull Name contractName, @NonNull Name name) {
        this.contractName = contractName;
        this.name = name;
    }

    @Override
    public @NonNull Sort getSort(Services services) {
        return services.getNamespaces().sorts().lookup(name);
    }

    @Override
    public Name name() {
        return name;
    }

    @Override
    public SyntaxElement getChild(int n) {
        throw new IndexOutOfBoundsException("Struct type has no children");
    }

    @Override
    public int getChildCount() {
        return 0;
    }

    @Override
    public String toString() {
        return contractName + "." + name;
    }

    @Override
    public void resolve(HashMap<Integer, SyntaxElement> id2Name) {
        return;
    }
}
