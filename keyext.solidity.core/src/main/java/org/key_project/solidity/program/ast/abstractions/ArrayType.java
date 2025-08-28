package org.key_project.solidity.program.ast.abstractions;

import org.jspecify.annotations.NonNull;
import org.key_project.logic.Name;

public class ArrayType implements Type {

    PrimitiveType primitiveType;
    int length;

    public ArrayType(PrimitiveType primitiveType, int length) {
        this.primitiveType = primitiveType;
        this.length = length;
    }


    @Override
    public @NonNull Name getName() {
        return primitiveType.getName();
    }
}
