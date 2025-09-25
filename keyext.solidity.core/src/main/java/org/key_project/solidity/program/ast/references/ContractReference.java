package org.key_project.solidity.program.ast.references;

import org.key_project.logic.Name;
import org.key_project.logic.SyntaxElement;
import org.key_project.solidity.program.ast.abstractions.Type;
import org.key_project.solidity.program.ast.declarations.ContractDeclaration;
import org.key_project.solidity.program.ast.declarations.Declaration;

public class ContractReference extends VariableReference {

    private int id;
    private final Name name;
    private ContractDeclaration contractDeclaration;

    public ContractReference(int id, Name name, Type type, ContractDeclaration contractDeclaration) {
        super(type);
        this.id = id;
        this.name = name;
        this.contractDeclaration = contractDeclaration;
    }

    public ContractReference(int id, Name name, Type type) {
        super(type);
        this.id = id;
        this.name = name;
    }

    @Override
    public Name name() {
        return name;
    }

    @Override
    public Declaration getDeclaration() {
        return contractDeclaration;
    }

    @Override
    public SyntaxElement getChild(int n) {
        return switch (n){
            case 0 -> contractDeclaration;
            default -> throw new RuntimeException("Element " + n + " is different than 0");
        };
    }

    @Override
    public int getChildCount() {
        return 1;
    }

    @Override
    public String toString() {
        return name.toString();
    }
}
