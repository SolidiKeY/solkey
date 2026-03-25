package org.key_project.solidity.logic.sort;

import org.key_project.logic.Name;
import org.key_project.logic.sort.Sort;
import org.key_project.solidity.program.ast.abstractions.ArrayInterface;
import org.key_project.solidity.program.ast.abstractions.Type;
import org.key_project.util.collection.ImmutableSet;

public class ArraySort implements Sort, ArrayInterface {
    private final Type type;
    private final int length;
    private final Name name;

    public ArraySort(Type type, int length) {
        this.type = type;
        this.length = length;
        name = new Name("Array " + type + " " + length);
    }

    public ArraySort(ArrayInterface array) {
        this.type = array.type();
        this.length = array.length();
        name = new Name("Array " + type + " " + length);
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

    @Override
    public Type type() {
        return type;
    }

    @Override
    public int length() {
        return length;
    }
}
