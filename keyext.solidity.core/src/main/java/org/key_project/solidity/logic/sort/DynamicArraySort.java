package org.key_project.solidity.logic.sort;

import org.key_project.logic.Name;
import org.key_project.logic.sort.Sort;


public class DynamicArraySort extends SortImpl {
    private final Sort sort;

    public DynamicArraySort(Sort sort) {
        super(new Name(sort + "[]"));
        this.sort = sort;
    }
}
