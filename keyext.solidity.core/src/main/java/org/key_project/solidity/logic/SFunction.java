package org.key_project.solidity.logic;

import org.key_project.logic.Name;
import org.key_project.logic.op.Function;
import org.key_project.logic.sort.Sort;
import org.key_project.util.collection.ImmutableArray;

public class SFunction extends Function {
    public SFunction(Name name, Sort sort) {
        super(name, new ImmutableArray<>(), sort, new ImmutableArray<>(), true, true, true);
    }
}
