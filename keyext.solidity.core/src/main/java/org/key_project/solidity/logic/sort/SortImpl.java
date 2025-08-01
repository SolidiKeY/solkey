/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.logic.sort;
/*
 * This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only
 */

import org.key_project.logic.Name;
import org.key_project.logic.sort.AbstractSort;
import org.key_project.logic.sort.Sort;
import org.key_project.solidity.logic.SolidityDLTheory;
import org.key_project.util.collection.DefaultImmutableSet;
import org.key_project.util.collection.ImmutableSet;

import org.jspecify.annotations.NonNull;

public class SortImpl extends AbstractSort {
    private ImmutableSet<Sort> ext;

    public SortImpl(Name name, boolean isAbstract, ImmutableSet<Sort> ext) {
        super(name, isAbstract);
        this.ext = ext;
    }

    public SortImpl(Name name, boolean isAbstract) {
        this(name, isAbstract, ImmutableSet.empty());
    }

    public SortImpl(Name name) {
        this(name, false);
    }

    @Override
    public @NonNull ImmutableSet<Sort> extendsSorts() {
        if (this == SolidityDLTheory.FORMULA || this == SolidityDLTheory.UPDATE
                || this == SolidityDLTheory.ANY) {
            return DefaultImmutableSet.nil();
        } else {
            if (ext.isEmpty()) {
                ext = DefaultImmutableSet.<Sort>nil().add(SolidityDLTheory.ANY);
            }
            return ext;
        }
    }

    @Override
    public boolean extendsTrans(@NonNull Sort sort) {
        if (sort == this) {
            return true;
        } else if (this == SolidityDLTheory.FORMULA || this == SolidityDLTheory.UPDATE) {
            return false;
        } else if (sort == SolidityDLTheory.ANY) {
            return true;
        }

        return extendsSorts()
                .exists((Sort superSort) -> superSort == sort || superSort.extendsTrans(sort));
    }


    @Override
    public @NonNull String declarationString() {
        return "";
    }
}
