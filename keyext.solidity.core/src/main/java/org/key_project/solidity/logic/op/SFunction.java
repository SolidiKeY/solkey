/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.logic.op;

import org.key_project.logic.Name;
import org.key_project.logic.op.Function;
import org.key_project.logic.sort.Sort;
import org.key_project.solidity.logic.SolidityDLTheory;
import org.key_project.util.collection.ImmutableArray;

public class SFunction extends Function {
    protected SFunction(Name name, ImmutableArray<Sort> argSorts, Sort sort,
            ImmutableArray<Boolean> whereToBind, boolean isRigid, boolean unique,
            boolean isSkolemConstant) {
        super(name, argSorts, sort, whereToBind, isRigid, unique, isSkolemConstant);
        assert sort != SolidityDLTheory.UPDATE;
        assert !(unique && sort == SolidityDLTheory.FORMULA);

    }

    protected SFunction(Name name, ImmutableArray<Sort> argSorts, Sort sort, boolean isRigid,
            boolean unique, boolean isSkolemConstant) {
        this(name, argSorts, sort, new ImmutableArray<>(), isRigid, unique, isSkolemConstant);
    }

    protected SFunction(Name name, ImmutableArray<Sort> argSorts, Sort sort, boolean isRigid,
            boolean unique) {
        this(name, argSorts, sort, new ImmutableArray<>(), isRigid, unique, false);
    }

    protected SFunction(Name name, ImmutableArray<Sort> argSorts, Sort sort, boolean isRigid) {
        this(name, argSorts, sort, new ImmutableArray<>(), isRigid, false, false);
    }

}
