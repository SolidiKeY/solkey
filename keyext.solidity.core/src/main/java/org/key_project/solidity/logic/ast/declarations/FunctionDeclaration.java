package org.key_project.solidity.logic.ast.declarations;

import org.key_project.logic.Name;
import org.key_project.logic.SyntaxElement;
import org.key_project.solidity.logic.ast.abstractions.Type;

public class FunctionDeclaration extends Declaration {
//    private final Type type;

    public FunctionDeclaration(Name name) {
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
