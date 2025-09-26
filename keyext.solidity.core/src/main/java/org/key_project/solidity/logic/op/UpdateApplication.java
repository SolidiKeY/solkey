/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.logic.op;

import org.jspecify.annotations.NonNull;
import org.key_project.logic.Name;
import org.key_project.logic.SyntaxElement;
import org.key_project.logic.Term;
import org.key_project.logic.TermCreationException;
import org.key_project.logic.op.AbstractOperator;
import org.key_project.logic.sort.Sort;
import org.key_project.solidity.logic.SolidityDLTheory;

/// Singleton class defining a binary operator {@code {u}t} that applies updates {@code u} to terms, formulas, or
/// other updates {@code t}.
public final class UpdateApplication extends AbstractOperator {

    public static final UpdateApplication UPDATE_APPLICATION = new UpdateApplication();

    private UpdateApplication() {
        super(new Name("update-application"), 2, false);
    }

    @Override
    public @NonNull Sort sort(Sort @NonNull [] sorts) {
        return sorts[1];
    }


    @Override
    public <T extends Term> void validTopLevelException(T term)
            throws TermCreationException {
        super.validTopLevelException(term);
        if (term.sub(0).sort() != SolidityDLTheory.UPDATE) {
            throw new TermCreationException(this, term);
        }
    }


    /// @return the index of the subterm representing the update being applied
    public static int updatePos() {
        return 0;
    }


    /// @return the subterm representing the update being applies
    /// @param t term with this operator as top level operator
    public static Term getUpdate(Term t) {
        assert t.op() == UPDATE_APPLICATION;
        return t.sub(updatePos());
    }

    /// @return the subterm representing the update being applies
    /// @param t term with this operator as top level operator
    public static Term getTarget(Term t) {
        assert t.op() == UPDATE_APPLICATION;
        return t.sub(targetPos());
    }


    /// @return the index of the subterm representing the formula/term/update that the update is
    /// applied to
    public static int targetPos() {
        return 1;
    }

    @Override
    public int getChildCount() {
        return 0;
    }

    @Override
    public @NonNull SyntaxElement getChild(int n) {
        throw new IndexOutOfBoundsException("UpdateApplication has no children");
    }
}
