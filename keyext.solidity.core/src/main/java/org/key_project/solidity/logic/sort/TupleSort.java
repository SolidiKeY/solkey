package org.key_project.solidity.logic.sort;

import org.jspecify.annotations.NonNull;
import org.key_project.logic.Name;
import org.key_project.logic.sort.Sort;
import org.key_project.util.collection.ImmutableArray;
import org.key_project.util.collection.ImmutableSet;

import java.util.List;
import java.util.stream.Collectors;

public class TupleSort implements Sort {
    ImmutableArray<Sort> sorts;
    private final Name name;

    public TupleSort(List<Sort> sorts){
        this.sorts = new ImmutableArray<>(sorts);
        this.name = new Name("(" + sorts.stream().map(s -> s.name().toString()).collect(Collectors.joining(", ")) + ")");
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
    public @NonNull Name name() {
        return name;
    }
}
