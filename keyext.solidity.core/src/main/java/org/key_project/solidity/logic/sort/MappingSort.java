/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.logic.sort;

import org.key_project.logic.Name;
import org.key_project.logic.sort.Sort;


public class MappingSort extends SortImpl {
    private static Sort keySort;
    private static Sort valueSort;

    public MappingSort(Sort keySort, Sort valueSort) {
        super(new Name("mapping(" + keySort + " => " + valueSort + ")"));
        this.keySort = keySort;
        this.valueSort = valueSort;
    }
}
