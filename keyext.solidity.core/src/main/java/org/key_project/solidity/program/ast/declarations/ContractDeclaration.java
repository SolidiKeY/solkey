/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.declarations;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.key_project.logic.Name;
import org.key_project.logic.SyntaxElement;
import org.key_project.solidity.program.ast.abstractions.Type;
import org.key_project.util.collection.ImmutableArray;

import org.jspecify.annotations.NonNull;

public class ContractDeclaration implements Declaration, Type {

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

    public ImmutableArray<ModifierDeclaration> getModifiers() {
        return modifiers;
    }

    @Override
    public @NonNull String toString() {
        return Stream.of(
            "contract " + name + " {",
            structs.stream()
                    .map(it -> "struct " + it.name + " {\n"
                        + it.getFields().stream()
                                .map(jt -> jt.toString() + "\n")
                                .collect(Collectors.joining())
                        + "}")
                    .collect(Collectors.joining("\n")),
            fields.stream().map(StateVariableDeclaration::toString)
                    .collect(Collectors.joining("\n")),
            modifiers.stream().map(ModifierDeclaration::toString)
                    .collect(Collectors.joining("\n")),
            enums.stream().map(EnumDeclaration::toString).collect(Collectors.joining("\n")),
            getFunctions().stream().map(FunctionDeclaration::toString)
                    .collect(Collectors.joining("\n")),
            "}")
                .filter(s -> !s.isEmpty())
                .collect(Collectors.joining("\n"));
    }

    @Override
    public @NonNull SyntaxElement getChild(int n) {
        if (n < 0)
            throw new IndexOutOfBoundsException(
                "Index should be 0 <= " + n + " < " + getChildCount());
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
        throw new IndexOutOfBoundsException("Index should be 0 <= " + n + " < " + getChildCount());
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

    public List<EnumDeclaration> getEnumDeclarations() {
        return enums.toList();
    }

    @Override
    public Name name() {
        return name;
    }
}
