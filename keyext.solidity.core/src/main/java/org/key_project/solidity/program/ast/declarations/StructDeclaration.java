/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.declarations;

import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import org.jspecify.annotations.Nullable;
import org.key_project.logic.Name;
import org.key_project.logic.SyntaxElement;
import org.key_project.logic.sort.Sort;
import org.key_project.solidity.common.Services;
import org.key_project.solidity.logic.sort.SortImpl;
import org.key_project.solidity.program.ast.Resolver;
import org.key_project.solidity.program.ast.abstractions.Type;
import org.key_project.util.ExtList;
import org.key_project.util.collection.ImmutableArray;

import org.jspecify.annotations.NonNull;

public class StructDeclaration extends Declaration implements Type, Resolver {
    public final @NonNull Name name;
    List<FieldDeclaration> fields;
    private final int contractId;
    ContractDeclaration contract;


    public StructDeclaration(@NonNull Name name, List<FieldDeclaration> fields, int contractId) {
        super(new ImmutableArray<>());
        this.name = name;
        this.fields = fields;
        this.contractId  = contractId;
    }

    public StructDeclaration(ExtList children) {
        super(Objects.requireNonNull(children.removeFirstOccurrence(ImmutableArray.class)));
        this.name = Objects.requireNonNull(children.removeFirstOccurrence(Name.class));
        this.fields = Objects.requireNonNull(children.removeFirstOccurrence(List.class));
        this.contractId  = 0;
    }

    public List<FieldDeclaration> getFields() {
        return fields;
    }

    @Override
    public SyntaxElement getChild(int n) {
        if (0 <= n && n < getChildCount())
            return fields.get(n);
        throw new RuntimeException("Child " + n + " out of bound");
    }

    @Override
    public int getChildCount() {
        return fields.size();
    }

    @Override
    public @Nullable Sort getSort(Services services) {
        return new SortImpl(name, false);
    }

    @Override
    public Name name() {
        return new Name(contract.name() + "." + name);
    }

    @Override
    public String toString() {
        return name.toString();
    }

    @Override
    public void resolve(HashMap<Integer, Declaration> id2Name) {
        contract = (ContractDeclaration) id2Name.get(contractId);
    }
}
