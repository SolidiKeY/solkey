/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.rule.sv.sort;

import org.key_project.logic.Name;
import org.key_project.logic.Term;
import org.key_project.logic.sort.Sort;
import org.key_project.solidity.common.Services;
import org.key_project.solidity.logic.op.ParametricFunctionInstance;
import org.key_project.solidity.logic.sort.SortImpl;
import org.key_project.solidity.program.ast.SolidityProgramElement;
import org.key_project.util.collection.DefaultImmutableSet;

import java.util.LinkedHashMap;
import java.util.Map;

public abstract class ProgramSVSort extends SortImpl {
    private static final Map<Name, ProgramSVSort> NAME2SORT = new LinkedHashMap<>(60);

    public static Map<Name, ProgramSVSort> name2sort() {
        return NAME2SORT;
    }

    public static final ProgramSVSort VARIABLE = null;
    public static final ProgramSVSort TYPE = null;
    public static final ProgramSVSort EXPRESSION = null;
    public static final ProgramSVSort NON_SIMPLE_EXPRESSION = null;
    public static final ProgramSVSort SIMPLE_EXPRESSION = null;


    @SuppressWarnings("argument.type.incompatible")
    protected ProgramSVSort(Name name) {
        super(name, false, DefaultImmutableSet.nil());
        NAME2SORT.put(name, this);
    }

    public ProgramSVSort createInstance(String parameter) {
        throw new UnsupportedOperationException();
    }

    public boolean canStandFor(Term t) {
        return true;
    }

    public abstract boolean canStandFor(SolidityProgramElement pe, Services services);

}
