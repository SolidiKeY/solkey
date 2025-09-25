package org.key_project.solidity.program.ast.references;

import org.key_project.logic.Name;
import org.key_project.solidity.program.ast.Resolver;
import org.key_project.solidity.program.ast.abstractions.Type;
import org.key_project.solidity.program.ast.declarations.Declaration;
import org.key_project.solidity.program.ast.declarations.FunctionDeclaration;

import java.util.HashMap;

public class FunctionReference extends VariableReference implements Resolver {

    private final int id;
    private final Name name;
    public FunctionDeclaration referencedDeclaration;

    public FunctionReference(int id, Name name, FunctionDeclaration referencedDeclaration, Type type) {
        super(type);
        this.id = id;
        this.name = name;
        this.referencedDeclaration = referencedDeclaration;
    }

    public FunctionReference(int id, Name name, Type type) {
        super(type);
        this.id = id;
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

    @Override
    public void resolve(HashMap<Integer, Declaration> id2Name) {
        this.referencedDeclaration = (FunctionDeclaration) id2Name.get(id);
    }
}
