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
    public static final ProgramSVSort FIELD = new FieldSVSort();
    public static final ProgramSVSort PATH =
        new PathSVSort("Path", PathSVSort.Location.ANY, PathSVSort.Simplicity.ANY);
    public static final ProgramSVSort STORAGE_PATH =
        new PathSVSort("StoragePath", PathSVSort.Location.STORAGE, PathSVSort.Simplicity.ANY);
    public static final ProgramSVSort SIMPLE_STORAGE_PATH =
        new PathSVSort("SimpleStoragePath", PathSVSort.Location.STORAGE,
            PathSVSort.Simplicity.SIMPLE);
    public static final ProgramSVSort COMPLEX_STORAGE_PATH =
        new PathSVSort("ComplexStoragePath", PathSVSort.Location.STORAGE,
            PathSVSort.Simplicity.COMPLEX);
    public static final ProgramSVSort MEMORY_PATH =
        new PathSVSort("MemoryPath", PathSVSort.Location.MEMORY, PathSVSort.Simplicity.ANY);
    public static final ProgramSVSort SIMPLE_MEMORY_PATH =
        new PathSVSort("SimpleMemoryPath", PathSVSort.Location.MEMORY,
            PathSVSort.Simplicity.SIMPLE);
    public static final ProgramSVSort COMPLEX_MEMORY_PATH =
        new PathSVSort("ComplexMemoryPath", PathSVSort.Location.MEMORY,
            PathSVSort.Simplicity.COMPLEX);
    public static final ProgramSVSort TYPE = new TypeSVSort();


    @SuppressWarnings("argument.type.incompatible")
    protected ProgramSVSort(Name name) {
        super(name, false, DefaultImmutableSet.nil());
        NAME2SORT.put(name, this);
    }

    public ProgramSVSort createInstance(String parameter) {
        throw new UnsupportedOperationException();
    }

    /// Whether this sort may stand for the given program element (inside a modality).
    public abstract boolean canStandFor(SolidityProgramElement pe, Services services);

    /// Whether this sort may stand for the given *term* in a term position of `\find`/`\assumes`.
    ///
    /// This is intentionally `false` by default: a program schema variable may not occur in a
    /// term position. The sole exception is a schema variable standing for a program *variable*,
    /// because a program variable is itself a logic term (an updateable operator) — the update
    /// calculus relies on matching e.g. `{pv := t}pv`. Such sorts override this method.
    ///
    /// To relate any other program schema variable to a term outside a modality, use a `\term`
    /// schema variable together with the `\sameAsTerm` variable condition.
    public boolean canStandFor(Term t) {
        return false;
    }

    /// Whether a schema variable of this sort may legitimately appear in a *term* position
    /// (outside a modality) of `\find`/`\assumes`. Only program-variable sorts may, because a
    /// program variable is itself a logic term (needed by the update calculus). Kept consistent
    /// with [#canStandFor(Term)]. Defaults to `false`; the taclet builder reports a clear error
    /// for any other program schema variable used in a term position there.
    public boolean mayOccurInTermPosition() {
        return false;
    }

}
