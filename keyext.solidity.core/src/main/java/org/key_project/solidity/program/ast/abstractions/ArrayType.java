package org.key_project.solidity.program.ast.abstractions;

import org.jspecify.annotations.NonNull;
import org.key_project.logic.Name;

public class ArrayType implements Type {

    Type type;
    int length;

    public ArrayType(Type type, int length) {
        this.type = type;
        this.length = length;
    }


    @Override
    public @NonNull Name getName() {
        return type.getName();
    }
}
