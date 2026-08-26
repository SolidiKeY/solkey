/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.logic.sort;

import org.key_project.logic.Name;
import org.key_project.logic.sort.Sort;
import org.key_project.util.collection.ImmutableSet;

public class ArraySort extends SortImpl {
    public final Sort elementSort;
    private final int length;

    public ArraySort(Sort elementSort, int length, ImmutableSet<Sort> ext) {
        super(new Name(elementSort + "[" + length + "]"), false, ext);
        this.elementSort = elementSort;
        this.length = length;
    }

    public ArraySort(Sort elementSort, int length) {
        this(elementSort, length, ImmutableSet.empty());
    }

    public Sort elementSort() {
        return elementSort;
    }

    public int length() {
        return length;
    }
}
