/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.logic.sort;

import org.key_project.logic.Name;
import org.key_project.logic.sort.Sort;
import org.key_project.util.collection.ImmutableSet;


public class DynamicArraySort extends SortImpl {
    private final Sort sort;

    public DynamicArraySort(Sort sort, ImmutableSet<Sort> ext) {
        super(new Name(sort + "[]"), false, ext);
        this.sort = sort;
    }

    public DynamicArraySort(Sort sort) {
        this(sort, ImmutableSet.empty());
    }
}
