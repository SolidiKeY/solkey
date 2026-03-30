package org.key_project.solidity.program.ast.abstractions;

import org.jspecify.annotations.Nullable;
import org.key_project.logic.Name;
import org.key_project.logic.SyntaxElement;
import org.key_project.logic.sort.Sort;
import org.key_project.solidity.common.Services;
import org.key_project.util.collection.ImmutableArray;

import java.util.List;

public class TupleType implements Type {
    ImmutableArray<Type> types;

    public TupleType(List<Type> types){
        this.types = new ImmutableArray<>(types);
    }

    @Override
    public @Nullable Sort getSort(Services services) {
        return null;
    }

    @Override
    public Name name() {
        return null;
    }

    @Override
    public SyntaxElement getChild(int n) {
        if(0 <= n && n < getChildCount())
            return types.get(n);
        throw new IndexOutOfBoundsException("!(0 < " + n + " <= " + getChildCount() + ")");
    }

    @Override
    public int getChildCount() {
        return types.size();
    }
}
