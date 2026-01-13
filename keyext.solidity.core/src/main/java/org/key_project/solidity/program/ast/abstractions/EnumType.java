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
import org.key_project.solidity.program.ast.SolidityProgramElement;
import org.key_project.solidity.program.ast.visitor.Visitor;
import org.key_project.util.ExtList;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class EnumType implements Type, SolidityProgramElement {

    private final Name name;

    public EnumType(Name name) {
        this.name = name;
    }

    public EnumType(ExtList children) {
        this.name = Objects.requireNonNull(children.removeFirstOccurrence(Name.class));
    }

    @Override
    public @Nullable Sort getSort(Services services) {
        return new SortImpl(name, false);
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

    public void visit(Visitor v) {
        v.performActionOnEnumType(this);
    }
}
