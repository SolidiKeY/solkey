/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.logic.op;

import org.key_project.logic.Name;
import org.key_project.logic.Term;
import org.key_project.logic.TermCreationException;
import org.key_project.logic.op.Function;
import org.key_project.logic.op.sv.SchemaVariable;
import org.key_project.logic.sort.Sort;
import org.key_project.solidity.rule.metaconstruct.AbstractTermTransformer;
import org.key_project.solidity.rule.sv.sort.ProgramSVSort;
import org.key_project.util.collection.ImmutableArray;

import org.jspecify.annotations.Nullable;

import static org.key_project.solidity.logic.SolidityDLTheory.FORMULA;
import static org.key_project.solidity.logic.SolidityDLTheory.UPDATE;

/// This class represents a function or predicate symbol in the logic
public class SFunction extends Function {

    public static final ImmutableArray<Sort> NO_ARGUMENTS = new ImmutableArray<>();

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
            argSorts == null ? NO_ARGUMENTS : new ImmutableArray<>(argSorts),
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
        this(name, sort, /* argSorts */ new ImmutableArray<>(), true, false, false);
    }

    public SFunction(Name name, Sort sort, boolean isRigid, boolean unique) {
        this(name, sort, /* argSorts */ new ImmutableArray<>(), isRigid, unique, false);
    }

    public SFunction(Name name, Sort sort, Sort... argSorts) {
        this(name, sort, argSorts, null, false, false);
    }

    public SFunction(Name name, Sort sort, boolean isSkolemConstant, Sort... argSorts) {
        this(name, sort, argSorts, null, false, isSkolemConstant);
    }

    /// In addition to the arity checks of the base operator, validates that every argument's sort
    /// conforms to the declared argument sort (so ill-typed terms such as `add(int, bool)` are
    /// rejected at construction). Mirrors legacy KeY's sorted-operator check.
    @Override
    public <T extends Term> void validTopLevelException(T term) throws TermCreationException {
        super.validTopLevelException(term);
        for (int i = 0, n = arity(); i < n; i++) {
            if (!possibleSub(i, term.sub(i))) {
                throw new TermCreationException(this, term);
            }
        }
    }

    /// Whether `sub` may legally occur as the `at`-th argument: its sort must be a (transitive)
    /// subsort of the declared argument sort, with the usual escapes for term transformers (the
    /// meta
    /// sort) and program schema-variable sorts, which are matched loosely.
    private boolean possibleSub(int at, Term sub) {
        // Schema-variable subterms (in taclet find/replacewith terms) are matched loosely; their
        // sorts are checked at instantiation time, not here.
        if (sub.op() instanceof SchemaVariable) {
            return true;
        }
        final Sort s = sub.sort();
        final Sort argSort = argSort(at);
        return s == AbstractTermTransformer.METASORT
                || s instanceof ProgramSVSort
                || argSort == AbstractTermTransformer.METASORT
                || argSort instanceof ProgramSVSort
                || s.extendsTrans(argSort);
    }
}
