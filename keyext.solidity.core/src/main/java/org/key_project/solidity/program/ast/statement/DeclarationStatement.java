package org.key_project.solidity.program.ast.statement;

import org.key_project.logic.SyntaxElement;
import org.key_project.solidity.program.ast.declarations.Declaration;
import org.key_project.solidity.program.ast.expressions.Expression;

import java.util.List;
import java.util.stream.Collectors;

public class DeclarationStatement implements Statement {
    private List<Declaration> declarations;
    private final Expression initialValue;

    public DeclarationStatement(List<Declaration> declarations, Expression initialValue) {
        this.declarations = declarations;
        this.initialValue = initialValue;
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
        String s = declarations.stream().map(Declaration::toString).collect(Collectors.joining(""));
        if(initialValue != null)
            s += " = " + initialValue;
        return s;
    }
}
