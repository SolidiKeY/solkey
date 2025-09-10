package org.key_project.solidity.program.ast.statement;

import org.key_project.logic.SyntaxElement;

public class PlaceholdStatement implements Statement {

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
        return "_;";
    }
}
