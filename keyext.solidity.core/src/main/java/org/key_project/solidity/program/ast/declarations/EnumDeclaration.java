/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.declarations;

import java.util.List;
import java.util.stream.Collectors;

import org.key_project.logic.Name;
import org.key_project.logic.SyntaxElement;
import org.key_project.solidity.program.ast.abstractions.Type;
import org.key_project.util.collection.ImmutableArray;


public class EnumDeclaration implements Declaration, Type {
    private final ImmutableArray<MemberEnumDeclaration> members;

    public Name getName() {
        return name;
    }

    private final Name name;

    public EnumDeclaration(Name name, List<MemberEnumDeclaration> members) {
        this.name = name;
        this.members = new ImmutableArray<>(members);
    }

    public ImmutableArray<MemberEnumDeclaration> getMembers() {
        return members;
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
    public Name name() {
        return name;
    }
}
