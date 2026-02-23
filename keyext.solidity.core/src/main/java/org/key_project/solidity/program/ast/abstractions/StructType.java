package org.key_project.solidity.program.ast.abstractions;

import org.jspecify.annotations.NonNull;
import org.key_project.logic.Name;
import org.key_project.logic.SyntaxElement;
import org.key_project.logic.sort.Sort;
import org.key_project.solidity.common.Services;
import org.key_project.solidity.logic.sort.SortImpl;

public class StructType implements Type {
    private final @NonNull Name contractName;
    private final @NonNull Name name;

    public StructType(@NonNull Name contractName, @NonNull Name name) {
        this.contractName = contractName;
        this.name = name;
    }

    @Override
    public @NonNull Sort getSort(Services services) {
        return new SortImpl(name, false);
    }

    @Override
    public Name name() {
        return name;
    }

    @Override
    public SyntaxElement getChild(int n) {
        throw new IndexOutOfBoundsException("Struct type has no children");
    }

    @Override
    public int getChildCount() {
        return 0;
    }

    @Override
    public String toString() {
        return contractName + "." + name;
    }
}
