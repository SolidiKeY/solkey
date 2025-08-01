package org.key_project.solidity.program.ast.declarations;

import org.jspecify.annotations.NonNull;
import org.key_project.logic.Name;
import org.key_project.logic.SyntaxElement;

import java.lang.reflect.Field;
import java.util.List;

public class StructDeclaration extends Declaration {
    List<StateVariableDeclaration> fields;

    public StructDeclaration(@NonNull Name name, List<StateVariableDeclaration> fields) {
        super(name);
        this.fields = fields;
    }

    @Override
    public SyntaxElement getChild(int n) {
        return null;
    }

    @Override
    public int getChildCount() {
        return 0;
    }
}
