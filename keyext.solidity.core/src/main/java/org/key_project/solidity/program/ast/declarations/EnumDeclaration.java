package org.key_project.solidity.program.ast.declarations;

import org.key_project.logic.Name;
import org.key_project.logic.SyntaxElement;

import java.util.List;
import java.util.stream.Collectors;

public class EnumDeclaration extends Declaration {
    private final List<MemberEnumDeclaration> members;

    public EnumDeclaration(Name name, List<MemberEnumDeclaration> members) {
        super(name);
        this.members = members;
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
        s += members.stream().map(MemberEnumDeclaration::toString).collect(Collectors.joining(", "));
        s += "\n}\n";
        return s;
    }
}
