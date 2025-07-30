package org.key_project.solidity.logic.ast.declarations;

import org.jspecify.annotations.NonNull;
import org.key_project.logic.Name;
import org.key_project.logic.SyntaxElement;
import org.key_project.solidity.logic.ast.references.TypeReference;

public class ParameterDeclaration extends Declaration {
    private final @NonNull TypeReference typeReference;

    public ParameterDeclaration(@NonNull Name name, @NonNull TypeReference typeReference) {
        super(name);
        this.typeReference = typeReference;
    }

    @Override
    public SyntaxElement getChild(int n) {
        return null;
    }

    @Override
    public int getChildCount() {
        return 0;
    }

    public @NonNull TypeReference getTypeReference() {
        return typeReference;
    }
}
