/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.declarations;

import java.util.HashMap;
import java.util.List;

import org.key_project.logic.Name;
import org.key_project.logic.SyntaxElement;
import org.key_project.solidity.program.ast.Resolver;
import org.key_project.solidity.program.ast.abstractions.Type;
import org.key_project.util.collection.ImmutableArray;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class StructDeclaration implements Declaration, Type, Resolver {
    public final @NonNull Name name;
    private final @NonNull ImmutableArray<@NonNull FieldDeclaration> fields;
    private final int contractId;
    @Nullable
    ContractDeclaration contract;


    public StructDeclaration(@NonNull Name name, List<FieldDeclaration> fields, int contractId) {
        this.name = name;
        this.fields = new ImmutableArray<>(fields);
        this.contractId = contractId;
    }

    public ImmutableArray<FieldDeclaration> getFields() {
        return fields;
    }

    @Override
    public @NonNull SyntaxElement getChild(int n) {
        if (0 <= n && n < getChildCount())
            return fields.get(n);
        throw new RuntimeException("Child " + n + " out of bound");
    }

    @Override
    public int getChildCount() {
        return fields.size();
    }

    @Override
    public Name name() {
        return name;
    }

    @Override
    public String toString() {
        return name.toString();
    }

    @Override
    public void resolve(HashMap<Integer, SyntaxElement> id2Name) {
        contract = (ContractDeclaration) id2Name.get(contractId);
    }
}
