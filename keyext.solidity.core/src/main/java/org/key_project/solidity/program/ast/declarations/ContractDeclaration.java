/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.declarations;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.key_project.logic.Name;
import org.key_project.logic.SyntaxElement;
import org.key_project.solidity.program.ast.SolidityProgramElement;
import org.key_project.solidity.program.ast.abstractions.Type;
import org.key_project.util.collection.ImmutableArray;


public class ContractDeclaration implements Declaration, Type {

    private final ImmutableArray<StateVariableDeclaration> fields;
    private final ImmutableArray<StructDeclaration> structs;
    private final ImmutableArray<ModifierDeclaration> modifiers;
    private final ImmutableArray<FunctionDeclaration> functions;
    private final ImmutableArray<EnumDeclaration> enums;
    private final Name name;
    private final Map<FunctionDeclaration, FunctionDeclaration> overrides = new HashMap<>();

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

    public void addOverride(FunctionDeclaration base, FunctionDeclaration implementation) {
        overrides.putIfAbsent(base, implementation);
    }

    public FunctionDeclaration dispatch(FunctionDeclaration function) {
        return overrides.getOrDefault(function, function);
    }

    public ImmutableArray<StateVariableDeclaration> getFieldDeclarations() {
        return fields;
    }

    public ImmutableArray<ModifierDeclaration> getModifiers() {
        return modifiers;
    }

    @Override
    public String toString() {
        return Stream.of(
            "contract " + name + " {",
            structs.stream()
                    .map(it -> "struct " + it.name() + " {\n"
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
    public SyntaxElement getChild(int n) {
        if (n < 0)
            throw SolidityProgramElement.outOfBounds(n, getChildCount());
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
        throw SolidityProgramElement.outOfBounds(n, getChildCount());
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
