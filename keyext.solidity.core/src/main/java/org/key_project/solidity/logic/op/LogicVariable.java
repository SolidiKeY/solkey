/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.logic.op;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.key_project.logic.Name;
import org.key_project.logic.SyntaxElement;
import org.key_project.logic.op.AbstractSortedOperator;
import org.key_project.logic.op.ParsableVariable;
import org.key_project.logic.op.QuantifiableVariable;
import org.key_project.logic.sort.Sort;

import static org.key_project.logic.op.Modifier.RIGID;
import static org.key_project.solidity.logic.SolidityDLTheory.FORMULA;
import static org.key_project.solidity.logic.SolidityDLTheory.UPDATE;

import org.jspecify.annotations.NonNull;


/// The objects of this class represent logical variables, used e.g. for quantification.
/// It uses De Brujin indices instead of names.
///
/// This class is for occurrences of logic variables in formulas/terms. For the class
/// for definition of logical variables {@see BoundVariable}.
public final class LogicVariable extends AbstractSortedOperator
        implements QuantifiableVariable, ParsableVariable {
    private static final Map<LogicVariable, LogicVariable> CACHE =
        new HashMap<LogicVariable, LogicVariable>();

    private final int index;

    public static LogicVariable create(int index, Sort sort) {
        var lv = new LogicVariable(index, sort);
        if (CACHE.containsKey(lv)) {
            return CACHE.get(lv);
        } else {
            CACHE.put(lv, lv);
        }
        return lv;
    }

    private LogicVariable(int index, Sort sort) {
        super(new Name("@" + index), sort, RIGID);
        assert sort != FORMULA;
        assert sort != UPDATE;
        this.index = index;
    }

    public int getIndex() {
        return index;
    }

    @Override
    public @NonNull String toString() {
        return name() + ":" + sort();
    }

    @Override
    public int getChildCount() {
        return 0;
    }

    @Override
    public @NonNull SyntaxElement getChild(int n) {
        throw new IndexOutOfBoundsException("Index should be 0 <= " + n + " < " + getChildCount());
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass())
            return false;
        LogicVariable that = (LogicVariable) o;
        return index == that.index && sort().equals(that.sort());
    }

    @Override
    public int hashCode() {
        return Objects.hash(index, sort());
    }
}
