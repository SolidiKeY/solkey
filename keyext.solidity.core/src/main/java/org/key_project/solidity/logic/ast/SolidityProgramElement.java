package org.key_project.solidity.logic.ast;

import org.key_project.logic.SyntaxElement;
import org.key_project.logic.SyntaxElementCursor;

public interface SolidityProgramElement extends SyntaxElement {

    @Override
    public abstract SyntaxElement getChild(int n);

    @Override
    public int getChildCount();

}
