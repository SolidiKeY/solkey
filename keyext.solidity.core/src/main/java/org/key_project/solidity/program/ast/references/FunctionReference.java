package org.key_project.solidity.program.ast.references;

import org.key_project.logic.Name;
import org.key_project.solidity.program.ast.abstractions.Type;
import org.key_project.solidity.program.ast.declarations.Declaration;
import org.key_project.solidity.program.ast.declarations.FunctionDeclaration;

public class FunctionReference extends VariableReference {

    private final Name name;
    private final FunctionDeclaration referencedDeclaration;

    public FunctionReference(Name name, FunctionDeclaration referencedDeclaration, Type type) {
        super(type);
        this.name = name;
        this.referencedDeclaration = referencedDeclaration;
    }

    @Override
    public Name getName() {
        return name;
    }

    @Override
    public Declaration getDeclaration() {
        return referencedDeclaration;
    }

    @Override
    public String toString() {
        return name.toString();
    }
}
