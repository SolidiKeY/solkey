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
import org.key_project.solidity.program.ast.SolidityProgramElement;
import org.key_project.solidity.program.ast.visitor.Visitor;
import org.key_project.util.ExtList;

import java.util.Objects;

public class MappingType implements Type, SolidityProgramElement {

    private final Type keyType;
    private final Type valueType;

    public MappingType(Type keyType, Type valueType) {
        this.keyType = keyType;
        this.valueType = valueType;
    }

    public MappingType(ExtList children) {
        this.keyType = Objects.requireNonNull(children.removeFirstOccurrence(Type.class));;
        this.valueType = Objects.requireNonNull(children.removeFirstOccurrence(Type.class));;
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

    @Override
    public SyntaxElement getChild(int n) {
        return null;
    }

    @Override
    public int getChildCount() {
        return 0;
    }

    public void visit(Visitor v){
        v.performActionOnMappingType(this);
    }
}
