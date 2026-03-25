package org.key_project.solidity.logic.sort;

import org.key_project.logic.Name;
import org.key_project.logic.sort.Sort;
import org.key_project.solidity.program.ast.abstractions.ArrayInterface;
import org.key_project.solidity.program.ast.abstractions.Type;
import org.key_project.util.collection.ImmutableSet;

public class DynamicArraySort implements Sort {
    private final Type type;
    private final Name name;

    public DynamicArraySort(Type type) {
        this.type = type;
        name = new Name("Dynamic Array " + type);
    }

    @Override
    public ImmutableSet<Sort> extendsSorts() {
        return null;
    }

    @Override
    public boolean extendsTrans(Sort s) {
        return false;
    }

    @Override
    public boolean isAbstract() {
        return false;
    }

    @Override
    public String declarationString() {
        return "";
    }

    @Override
    public Name name() {
        return name;
    }
}
