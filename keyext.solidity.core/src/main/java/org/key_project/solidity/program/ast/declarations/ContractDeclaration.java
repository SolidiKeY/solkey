/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.declarations;

import java.util.List;
import java.util.stream.Collectors;

import org.jspecify.annotations.Nullable;
import org.key_project.logic.Name;
import org.key_project.logic.SyntaxElement;
import org.key_project.logic.sort.Sort;
import org.key_project.solidity.common.Services;
import org.key_project.solidity.program.ast.abstractions.Type;
import org.key_project.util.collection.ImmutableArray;

import org.jspecify.annotations.NonNull;

public class ContractDeclaration extends Declaration implements Type {

    private final ImmutableArray<StateVariableDeclaration> fields;
    private final List<StructDeclaration> structs;
    private final List<ModifierDeclaration> modifiers;
    private final List<FunctionDeclaration> functions;

    public ContractDeclaration(Name name, List<StateVariableDeclaration> fields, List<StructDeclaration> structs,
                               List<ModifierDeclaration> modifiers, List<FunctionDeclaration> functions) {
        super(name);
        this.fields = new ImmutableArray<>(fields.toArray(new StateVariableDeclaration[0]));
        this.structs = structs;
        this.modifiers = modifiers;
        this.functions = functions;
    }

    public ImmutableArray<StateVariableDeclaration> getFieldDeclarations() {
        return fields;
    }

    @Override
    public @NonNull String toString() {
        String contract = "contract ";
        contract += name() + " {\n";
        contract += structs.stream().map(it ->
            "struct " + it.name() + " {\n"
            + it.fields.stream().map(jt ->
                    jt.toString() + "\n"
                ).collect(Collectors.joining()) + "}\n").collect(Collectors.joining());
        for (int i = 0; i < fields.size(); i++) {
            contract += fields.get(i);
            contract += "\n";
        }
        contract += modifiers.stream().map(ModifierDeclaration::toString).collect(Collectors.joining("\n"));
        contract += getFunctions().stream().map(FunctionDeclaration::toString).collect(Collectors.joining("\n"));
        contract += "}";
        return contract;
    }

    @Override
    public SyntaxElement getChild(int n) {
        return fields.get(n);
    }

    @Override
    public int getChildCount() {
        return fields.size();
    }

    public List<StructDeclaration> getStructs() {
        return structs;
    }

    public List<FunctionDeclaration> getFunctions() {
        return functions;
    }

    @Override
    public @Nullable Sort getSort(Services services) {
        // TODO
        throw new UnsupportedOperationException("TO BE IMPLEMENTED");
    }
}
