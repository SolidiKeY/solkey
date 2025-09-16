package org.key_project.solidity.program.ast.references;

import org.key_project.logic.Name;
import org.key_project.solidity.program.ast.abstractions.Type;
import org.key_project.solidity.program.ast.declarations.Declaration;
import org.key_project.solidity.program.ast.declarations.StatementVariableDeclaration;

public class StatementVariableReference extends VariableReference {
    private final Name name;
    private final StatementVariableDeclaration stmVarDecl;

    public StatementVariableReference(Name name, StatementVariableDeclaration stmVarDeclaration, Type type) {
        super(type);
        this.name = name;
        this.stmVarDecl = stmVarDeclaration;
    }

    @Override
    public Name name() {
        return name;
    }

    @Override
    public Declaration getDeclaration() {
        return stmVarDecl;
    }

    @Override
    public String toString() {
        return name.toString();
    }
}
