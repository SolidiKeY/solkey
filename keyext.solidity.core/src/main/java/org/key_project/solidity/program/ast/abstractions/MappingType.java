package org.key_project.solidity.program.ast.abstractions;

import org.checkerframework.checker.units.qual.N;
import org.jspecify.annotations.NonNull;
import org.key_project.logic.Name;

public class MappingType implements Type {

    private final Type keyType;
    private final Type valueType;

    public MappingType(Type keyType, Type valueType) {
        this.keyType = keyType;
        this.valueType = valueType;
    }

    @Override
    public @NonNull Name getName() {
        return new Name("mapping(" + keyType + " => " + valueType + ")");
    }
}
