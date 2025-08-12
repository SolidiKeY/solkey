package org.key_project.solidity.program.ast.statement;

import org.key_project.logic.SyntaxElement;
import org.key_project.solidity.program.ast.declarations.Declaration;
import org.key_project.solidity.program.ast.declarations.MemoryDeclaration;

import java.util.List;

public class DeclarationStatement implements Statement {
    private List<MemoryDeclaration> declarations;

    public DeclarationStatement(List<MemoryDeclaration> declarations) {
        this.declarations = declarations;
    }

    @Override
    public SyntaxElement getChild(int n) {
        return null;
    }

    @Override
    public int getChildCount() {
        return 0;
    }

    public List<MemoryDeclaration> getDeclarations() {
        return declarations;
    }
}
