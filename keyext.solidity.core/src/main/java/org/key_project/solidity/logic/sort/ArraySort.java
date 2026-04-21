/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
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
