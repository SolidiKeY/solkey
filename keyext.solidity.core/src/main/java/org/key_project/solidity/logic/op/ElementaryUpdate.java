/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.logic.op;

import java.lang.ref.WeakReference;
import java.util.WeakHashMap;

import org.key_project.logic.Name;
import org.key_project.logic.SyntaxElement;
import org.key_project.logic.op.AbstractSortedOperator;
import org.key_project.logic.op.UpdateableOperator;
import org.key_project.logic.sort.Sort;

import static org.key_project.logic.op.Modifier.NONE;
import static org.key_project.solidity.logic.SolidityDLTheory.UPDATE;

import org.jspecify.annotations.NonNull;

/// Represents an elementary update {@code x:=t } where {@code x} is a program variable and {@code
/// t} a term of
/// compatible sort.
/// The program variable {@code x} is part of the operator, hence, there is one elementary update
/// for each program
/// variable. This class ensures that there is also at most one per program variable to ensure
/// reference identity
/// required for operators.
///
/// @see UpdateJunctor
/// @see UpdateApplication
public class ElementaryUpdate extends AbstractSortedOperator {

    private static final WeakHashMap<UpdateableOperator, WeakReference<ElementaryUpdate>> instances =
        new WeakHashMap<>();

    private final UpdateableOperator lhs;

    private ElementaryUpdate(UpdateableOperator lhs) {
        super(new Name("elem-update(" + lhs + ")"), new Sort[] { lhs.sort() },
            UPDATE,
            NONE);
        this.lhs = lhs;
        assert lhs.arity() == 0;
    }


    /// Returns the elementary update operator for the passed left hand side.
    public static ElementaryUpdate getInstance(UpdateableOperator lhs) {
        WeakReference<ElementaryUpdate> ref = instances.get(lhs);
        ElementaryUpdate result = null;
        if (ref != null) {
            result = ref.get();
        }
        if (result == null) {
            result = new ElementaryUpdate(lhs);
            ref = new WeakReference<>(result);
            instances.put(lhs, ref);
        }
        return result;
    }


    /// Returns the left hand side of this elementary update operator.
    public UpdateableOperator lhs() {
        return lhs;
    }

    @Override
    public int getChildCount() {
        return 1;
    }

    @Override
    public @NonNull SyntaxElement getChild(int n) {
        if (n == 0)
            return lhs;
        throw new IndexOutOfBoundsException("Index should be 0 <= " + n + " < " + getChildCount());
    }
}
