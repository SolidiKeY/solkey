package org.key_project.solidity.program.ast.references;

import org.key_project.logic.Name;
import org.key_project.solidity.program.ast.abstractions.Type;
import org.key_project.solidity.program.ast.declarations.Declaration;
import org.key_project.solidity.program.ast.declarations.FunctionDeclaration;

public class FunctionReference extends VariableReference {

    private final Name name;
    private FunctionDeclaration referencedDeclaration;

    public FunctionReference(Name name, FunctionDeclaration referencedDeclaration, Type type) {
        super(type);
        this.name = name;
        this.referencedDeclaration = referencedDeclaration;
    }

    public FunctionReference(Name name, Type type) {
        super(type);
        this.name = name;
        this.referencedDeclaration = null;
    }

    @Override
    public Name name() {
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
