package org.key_project.solidity.program.ast.references;

import org.key_project.logic.Name;
import org.key_project.solidity.program.ast.abstractions.Type;
import org.key_project.solidity.program.ast.declarations.Declaration;
import org.key_project.solidity.program.ast.declarations.EnumDeclaration;

import javax.naming.Reference;

public class EnumReference extends VariableReference {
    private final Name name;
    private final EnumDeclaration enumDeclaration;

    public EnumReference(Name name, EnumDeclaration enumDeclaration, Type type) {
        super(type);
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
