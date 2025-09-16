package org.key_project.solidity.program.ast.abstractions;

import org.checkerframework.checker.units.qual.N;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.key_project.logic.Name;
import org.key_project.logic.sort.Sort;
import org.key_project.solidity.common.Services;

public class MappingType implements Type {

    private final Type keyType;
    private final Type valueType;

    public MappingType(Type keyType, Type valueType) {
        this.keyType = keyType;
        this.valueType = valueType;
    }

    @Override
    public @NonNull Name name() {
        return new Name("mapping(" + keyType + " => " + valueType.name() + ")");
    }

    @Override
    public @Nullable Sort getSort(Services services) {
        // TODO
        throw new UnsupportedOperationException("To be implemented");
    }
}
