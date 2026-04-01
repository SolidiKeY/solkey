package org.key_project.solidity.logic.sort;

import org.key_project.logic.Name;
import org.key_project.logic.sort.Sort;


public class MappingSort extends SortImpl {
    private static Sort keySort;
    private static Sort valueSort;

    public MappingSort(Sort keySort, Sort valueSort){
        super(new Name("mapping(" + keySort + " => " + valueSort + ")"));
        this.keySort = keySort;
        this.valueSort = valueSort;
    }
}
