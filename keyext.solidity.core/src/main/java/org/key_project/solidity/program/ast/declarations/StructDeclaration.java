package org.key_project.solidity.program.ast.declarations;

import org.jspecify.annotations.NonNull;
import org.key_project.logic.Name;
import org.key_project.logic.SyntaxElement;

import java.lang.reflect.Field;
import java.util.List;

public class StructDeclaration extends Declaration {
    List<FieldDeclaration> fields;

    public StructDeclaration(@NonNull Name name, List<FieldDeclaration> fields) {
        super(name);
        this.fields = fields;
    }

    public List<FieldDeclaration> getFields() {
        return fields;
    }

    @Override
    public SyntaxElement getChild(int n) {
        if(0 < n && n < getChildCount())
            return fields.get(n);
        throw new RuntimeException("Child " + n + " out of bound");
    }

    @Override
    public int getChildCount() {
        return fields.size();
    }
}
