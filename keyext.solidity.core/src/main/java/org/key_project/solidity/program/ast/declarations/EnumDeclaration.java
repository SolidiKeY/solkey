/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.declarations;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.key_project.logic.Name;
import org.key_project.logic.SyntaxElement;
import org.key_project.logic.sort.Sort;
import org.key_project.solidity.common.Services;
import org.key_project.solidity.logic.sort.SortImpl;
import org.key_project.solidity.program.ast.abstractions.Type;
import org.key_project.util.ExtList;
import org.key_project.util.collection.ImmutableArray;

import org.jspecify.annotations.Nullable;

public class EnumDeclaration extends Declaration implements Type {
    private final List<MemberEnumDeclaration> members;

    public Name getName() {
        return name;
    }

    private final Name name;

    public EnumDeclaration(Name name, List<MemberEnumDeclaration> members) {
        super(new ImmutableArray<>());
        this.name = name;
        this.members = members;
    }

    public EnumDeclaration(ExtList children) {
        super(Objects.requireNonNull(children.removeFirstOccurrence(ImmutableArray.class)));
        this.name = Objects.requireNonNull(children.removeFirstOccurrence(Name.class));
        this.members = Objects.requireNonNull(children.removeFirstOccurrence(List.class));
    }

    public MemberEnumDeclaration findMember(Name name) {
        return members.stream()
                .filter(x -> x.getName().equals(name))
                .findFirst().orElseThrow();
    }

    @Override
    public SyntaxElement getChild(int n) {
        return members.get(n);
    }

    @Override
    public int getChildCount() {
        return members.size();
    }

    @Override
    public String toString() {
        String s = "enum " + name + " {\n";
        s += members.stream().map(MemberEnumDeclaration::toString)
                .collect(Collectors.joining(", "));
        s += "\n}\n";
        return s;
    }

    @Override
    public @Nullable Sort getSort(Services services) {
        return new SortImpl(name(), false);
    }

    @Override
    public Name name() {
        return name;
    }
}
