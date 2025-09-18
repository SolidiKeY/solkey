package org.key_project.solidity.program.ast.declarations;

import org.jspecify.annotations.NonNull;
import org.key_project.logic.Name;
import org.key_project.logic.SyntaxElement;

public class MemberEnumDeclaration extends Declaration {

    public MemberEnumDeclaration(@NonNull Name name) {
        super(name);
    }

    @Override
    public SyntaxElement getChild(int n) {
        return null;
    }

    @Override
    public int getChildCount() {
        return 0;
    }
}
