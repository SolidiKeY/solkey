package org.key_project.solidity.program.ast.statement;

import org.key_project.logic.SyntaxElement;
import org.key_project.solidity.program.ast.declarations.Declaration;

import java.util.List;
import java.util.stream.Collectors;

public class DeclarationStatement implements Statement {
    private List<Declaration> declarations;

    public DeclarationStatement(List<Declaration> declarations) {
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

    public List<Declaration> getDeclarations() {
        return declarations;
    }

    @Override
    public String toString() {
        return declarations.stream().map(Declaration::toString).collect(Collectors.joining(""));
    }
}
