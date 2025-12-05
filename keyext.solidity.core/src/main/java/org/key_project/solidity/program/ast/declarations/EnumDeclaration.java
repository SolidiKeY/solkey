/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.declarations;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.key_project.logic.Name;
import org.key_project.logic.SyntaxElement;
import org.key_project.solidity.program.ast.visitor.Visitor;
import org.key_project.util.ExtList;

public class EnumDeclaration extends Declaration {
    private final List<MemberEnumDeclaration> members;

    public EnumDeclaration(Name name, List<MemberEnumDeclaration> members) {
        super(name);
        this.members = members;
    }

    public EnumDeclaration(ExtList children) {
        super(Objects.requireNonNull(children.removeFirstOccurrence(Name.class)));
        this.members = Objects.requireNonNull(children.removeFirstOccurrence(List.class));
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

    public void visit(Visitor v){
        v.performActionOnEnumDeclaration(this);
    }
}
