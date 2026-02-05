/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.declarations;

import java.util.List;
import java.util.stream.Collectors;

import org.key_project.logic.Name;
import org.key_project.logic.SyntaxElement;
import org.key_project.logic.sort.Sort;
import org.key_project.solidity.common.Services;
import org.key_project.solidity.program.ast.abstractions.Type;
import org.key_project.util.collection.ImmutableArray;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class ContractDeclaration extends Declaration implements Type {

    private final ImmutableArray<StateVariableDeclaration> fields;
    private final ImmutableArray<StructDeclaration> structs;
    private final ImmutableArray<ModifierDeclaration> modifiers;
    private final ImmutableArray<FunctionDeclaration> functions;
    private final ImmutableArray<EnumDeclaration> enums;
    private final Name name;

    public ContractDeclaration(Name name, List<StateVariableDeclaration> fields,
            List<StructDeclaration> structs,
            List<ModifierDeclaration> modifiers, List<FunctionDeclaration> functions,
            List<EnumDeclaration> enums) {
        super(new ImmutableArray<>());
        this.name = name;
        this.fields = new ImmutableArray<>(fields.toArray(new StateVariableDeclaration[0]));
        this.structs = new ImmutableArray<>(structs);
        this.modifiers = new ImmutableArray<>(modifiers);
        this.functions = new ImmutableArray<>(functions);
        this.enums = new ImmutableArray<>(enums);
    }

    public ImmutableArray<StateVariableDeclaration> getFieldDeclarations() {
        return fields;
    }

    @Override
    public @NonNull String toString() {
        String contract = "contract ";
        contract += name + " {\n";
        contract += structs.stream().map(it -> "struct " + it.name + " {\n"
            + it.fields.stream().map(jt -> jt.toString() + "\n").collect(Collectors.joining())
            + "}\n").collect(Collectors.joining());
        for (int i = 0; i < fields.size(); i++) {
            contract += fields.get(i).toString();
            contract += "\n";
        }
        contract +=
            modifiers.stream().map(ModifierDeclaration::toString).collect(Collectors.joining("\n"));
        contract += enums.stream().map(EnumDeclaration::toString).collect(Collectors.joining("\n"));
        contract += getFunctions().stream().map(FunctionDeclaration::toString)
                .collect(Collectors.joining("\n"));
        contract += "}";
        return contract;
    }

    @Override
    public SyntaxElement getChild(int n) {
        if (n < 0)
            return null;
        if (n < fields.size())
            return fields.get(n);
        n -= fields.size();
        if (n < structs.size())
            return structs.get(n);
        n -= structs.size();
        if (n < modifiers.size())
            return modifiers.get(n);
        n -= modifiers.size();
        if (n < functions.size())
            return functions.get(n);
        n -= functions.size();;
        if (n < enums.size())
            return enums.get(n);
        return null;
    }

    @Override
    public int getChildCount() {
        return fields.size() + structs.size() + modifiers.size() + functions.size() + enums.size();
    }

    public List<StructDeclaration> getStructs() {
        return structs.toList();
    }

    public List<FunctionDeclaration> getFunctions() {
        return functions.toList();
    }

    @Override
    public @Nullable Sort getSort(Services services) {
        // TODO
        throw new UnsupportedOperationException("TO BE IMPLEMENTED");
    }

    @Override
    public Name name() {
        return name;
    }
}
