/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
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
