package org.key_project.solidity.program.ast.references;

import org.key_project.logic.SyntaxElement;
import org.key_project.solidity.program.ast.abstractions.Type;
import org.key_project.solidity.program.ast.expressions.Expression;

public class ModifierReference implements SyntaxElement {

    private final String name;

    public ModifierReference(String name) {
        this.name = name;
    }

    @Override
    public SyntaxElement getChild(int n) {
        return null;
    }

    @Override
    public int getChildCount() {
        return 0;
    }

    @Override
    public String toString() {
        return name;
    }
}
