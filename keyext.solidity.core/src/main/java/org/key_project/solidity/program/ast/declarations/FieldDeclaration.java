/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.declarations;


import org.key_project.logic.Name;
import org.key_project.solidity.program.ast.SolidityProgramElement;
import org.key_project.solidity.program.ast.SourceData;
import org.key_project.solidity.program.ast.expressions.Expression;
import org.key_project.solidity.program.ast.references.TypeReference;
import org.key_project.solidity.program.ast.visitor.Visitor;
import org.key_project.solidity.rule.matching.inst.MatchConditions;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class FieldDeclaration implements Declaration, SolidityProgramElement {

    private final @NonNull TypeReference typeReference;
    private final @Nullable Expression initializer;
    private final @NonNull Name name;
    private @Nullable StructDeclaration containingStruct;

    public FieldDeclaration(@NonNull Name name, @NonNull TypeReference type) {
        this.name = name;
        this.typeReference = type;
        this.initializer = null;
    }

    public @NonNull Name name() {
        return name;
    }

    public @NonNull TypeReference getTypeReference() {
        return typeReference;
    }

    public @Nullable Expression getInitializer() {
        return initializer;
    }

    public @Nullable StructDeclaration getContainingStruct() {
        return containingStruct;
    }

    public void setContainingStruct(@NonNull StructDeclaration containingStruct) {
        this.containingStruct = containingStruct;
    }

    @Override
    public @Nullable MatchConditions match(SourceData sourceData, @Nullable MatchConditions mc) {
        if (!(sourceData.getSource() instanceof FieldDeclaration source)
                || !name.equals(source.name())) {
            return null;
        }

        SourceData newSource = new SourceData(source, 0, sourceData.getServices());
        mc = matchChildren(newSource, mc, 0);
        if (mc == null) {
            return null;
        }

        sourceData.next();
        return mc;
    }

    @Override
    public void visit(Visitor v) {
        v.performActionOnFieldDeclaration(this);
    }

    // Syntax Element interface
    @Override
    public int getChildCount() {
        return initializer == null ? 1 : 2;
    }

    @Override
    public SolidityProgramElement getChild(int i) {
        if (i < 0 || i >= getChildCount()) {
            throw new IndexOutOfBoundsException(
                "Index should be 0 <= " + i + " < " + getChildCount());
        }
        if (i == 0) {
            return typeReference;
        }
        if (initializer != null) {
            return initializer;
        }
        throw new IndexOutOfBoundsException("Index should be 0 <= " + i + " < " + getChildCount());
    }


    // common interface
    public String toString() {
        return typeReference + " " + name + (initializer != null ? " = " + initializer : "")
            + ";";
    }
}
