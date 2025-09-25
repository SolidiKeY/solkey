package org.key_project.solidity.program.ast.references;

import org.key_project.logic.Name;
import org.key_project.solidity.program.ast.abstractions.Type;
import org.key_project.solidity.program.ast.declarations.Declaration;
import org.key_project.solidity.program.ast.declarations.EnumDeclaration;

public class EnumReference extends VariableReference {
    private final int id;
    private final Name name;
    private final EnumDeclaration enumDeclaration;

    public EnumReference(int id, Name name, EnumDeclaration enumDeclaration, Type type) {
        super(type);
        this.id = id;
        this.name = name;
        this.enumDeclaration = enumDeclaration;
    }

    @Override
    public Name name() {
        return null;
    }

    @Override
    public Declaration getDeclaration() {
        return null;
    }

    @Override
    public String toString() {
        return name.toString();
    }
}
