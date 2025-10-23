/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.logic.op;

import org.key_project.logic.Name;
import org.key_project.logic.op.Function;
import org.key_project.logic.sort.Sort;
import org.key_project.solidity.logic.SolidityDLTheory;
import org.key_project.util.collection.ImmutableArray;

import org.jspecify.annotations.Nullable;

/// This class represents a function or predicate symbol in the logic
public class SFunction extends Function {
    public SFunction(Name name, ImmutableArray<Sort> argSorts, Sort sort,
            @Nullable ImmutableArray<Boolean> whereToBind, boolean isRigid, boolean unique,
            boolean isSkolemConstant) {
        super(name, argSorts, sort, whereToBind, isRigid, unique, isSkolemConstant);
        assert sort != SolidityDLTheory.UPDATE;
        assert !(unique && sort == SolidityDLTheory.FORMULA);

    }

    public SFunction(Name name, ImmutableArray<Sort> argSorts, Sort sort, boolean isRigid,
            boolean unique, boolean isSkolemConstant) {
        this(name, argSorts, sort, null, isRigid, unique, isSkolemConstant);
    }

    public SFunction(Name name, ImmutableArray<Sort> argSorts, Sort sort, boolean isRigid,
            boolean unique) {
        this(name, argSorts, sort, null, isRigid, unique, false);
    }

    public SFunction(Name name, ImmutableArray<Sort> argSorts, Sort sort, boolean isRigid) {
        this(name, argSorts, sort, null, isRigid, false, false);
    }

}
