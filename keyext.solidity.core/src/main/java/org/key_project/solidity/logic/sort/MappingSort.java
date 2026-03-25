package org.key_project.solidity.logic.sort;

import org.key_project.logic.Name;
import org.key_project.logic.sort.Sort;
import org.key_project.solidity.program.ast.abstractions.MappingInterface;
import org.key_project.solidity.program.ast.abstractions.Type;
import org.key_project.util.collection.ImmutableSet;

public class MappingSort implements Sort, MappingInterface {
    private static Type keyType;
    private static Type valueType;

    public MappingSort(Type keyType, Type valueType){
        this.keyType = keyType;
        this.valueType = valueType;
    }

    public MappingSort(MappingInterface map){
        this.keyType = map.keyType();
        this.valueType = map.valueType();
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
        return null;
    }

    @Override
    public Type keyType() {
        return keyType;
    }

    @Override
    public Type valueType() {
        return valueType;
    }
}
