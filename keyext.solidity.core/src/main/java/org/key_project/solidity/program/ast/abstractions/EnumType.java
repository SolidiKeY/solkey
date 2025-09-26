package org.key_project.solidity.program.ast.abstractions;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.key_project.logic.Name;
import org.key_project.logic.sort.Sort;
import org.key_project.solidity.common.Services;

public class EnumType implements Type {

    private final Name name;

    public EnumType(Name name) {
        this.name = name;
    }

    @Override
    public @Nullable Sort getSort(Services services) {
        return null;
    }

    @Override
    public @NonNull Name name() {
        return name;
    }
}
