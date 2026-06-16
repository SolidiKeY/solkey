/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.rule.sv.sort;

import java.util.LinkedHashMap;
import java.util.Map;

import org.key_project.logic.Name;
import org.key_project.logic.Term;
import org.key_project.solidity.common.Services;
import org.key_project.solidity.logic.sort.SortImpl;
import org.key_project.solidity.program.ast.SolidityProgramElement;
import org.key_project.util.collection.DefaultImmutableSet;

public abstract class ProgramSVSort extends SortImpl {
    private static final Map<Name, ProgramSVSort> NAME2SORT = new LinkedHashMap<>(60);

    public static Map<Name, ProgramSVSort> name2sort() {
        return NAME2SORT;
    }

    public static final ProgramSVSort VARIABLE = new ProgramVariableSVSort(new Name("Variable"));
    public static final ProgramSVSort SIMPLE_EXPRESSION = new SimpleExpressionSVSort();
    public static final ProgramSVSort EXPRESSION = new ExpressionSVSort();
    public static final ProgramSVSort NON_SIMPLE_EXPRESSION = new NonSimpleExpressionSVSort();
    public static final ProgramSVSort FUNCTION_BODY = new FunctionBodySVSort();
    public static final ProgramSVSort FIELD_REFERENCE = new FieldReferenceSVSort();
    public static final ProgramSVSort TYPE = new TypeSVSort();


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
