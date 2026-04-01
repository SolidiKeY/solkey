package org.key_project.solidity.logic.sort;

import org.key_project.logic.Name;
import org.key_project.logic.sort.Sort;

public class ArraySort extends SortImpl {
    private final Sort sort;
    private final int length;

    public ArraySort(Sort sort, int length) {
        super(new Name(sort + "[" + length + "]"));
        this.sort = sort;
        this.length = length;
    }
}
