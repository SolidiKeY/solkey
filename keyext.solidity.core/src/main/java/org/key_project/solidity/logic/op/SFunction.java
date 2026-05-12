/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.logic.op;

import org.key_project.logic.Name;
import org.key_project.logic.op.Function;
import org.key_project.logic.sort.Sort;
import org.key_project.util.collection.ImmutableArray;

import org.jspecify.annotations.Nullable;

import static org.key_project.solidity.logic.SolidityDLTheory.FORMULA;
import static org.key_project.solidity.logic.SolidityDLTheory.UPDATE;

/// This class represents a function or predicate symbol in the logic
public class SFunction extends Function {
    public SFunction(Name name, Sort sort, ImmutableArray<Sort> argSorts,
            @Nullable ImmutableArray<Boolean> whereToBind, boolean isRigid, boolean unique,
            boolean isSkolemConstant) {
        super(name, argSorts, sort, whereToBind, isRigid, unique, isSkolemConstant);
        assert sort != UPDATE;
        assert !(unique && sort == FORMULA);

    }

    public SFunction(Name name, Sort sort, ImmutableArray<Sort> argSorts, boolean isRigid,
            boolean unique, boolean isSkolemConstant) {
        this(name, sort, argSorts, null, isRigid, unique, isSkolemConstant);
    }

    public SFunction(Name name, Sort sort, ImmutableArray<Sort> argSorts, boolean isRigid,
            boolean unique) {
        this(name, sort, argSorts, null, isRigid, unique, false);
    }

    public SFunction(Name name, Sort sort, ImmutableArray<Sort> argSorts, boolean isRigid) {
        this(name, sort, argSorts, null, isRigid, false, false);
    }

    public SFunction(Name name, Sort sort, Sort[] argSorts, boolean isRigid, boolean unique) {
        this(name, sort,
            argSorts == null ? new ImmutableArray<>() : new ImmutableArray<>(argSorts),
            null, isRigid, unique, false);
    }

    public SFunction(Name name, Sort sort, Sort[] argSorts, Boolean @Nullable [] whereToBind,
            boolean unique, boolean isSkolemConstant) {
        super(name, new ImmutableArray<>(argSorts), sort,
            whereToBind == null ? null : new ImmutableArray<>(whereToBind),
            true, unique, isSkolemConstant);
    }

    public SFunction(Name name, Sort sort, Sort[] argSorts, Boolean @Nullable [] whereToBind,
            boolean unique) {
        super(name, new ImmutableArray<>(argSorts), sort,
            whereToBind == null ? null : new ImmutableArray<>(whereToBind),
            true, unique, false);
    }

    public SFunction(Name name, Sort sort) {
        this(name, sort, null, true, false, false);
    }

    public SFunction(Name name, Sort sort, Sort... argSorts) {
        this(name, sort, argSorts, null, false, false);
    }

    public SFunction(Name name, Sort sort, boolean isSkolemConstant, Sort... argSorts) {
        this(name, sort, argSorts, null, false, isSkolemConstant);
    }
}
