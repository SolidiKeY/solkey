/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.rule.sv.sort;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.key_project.logic.Name;
import org.key_project.logic.Term;
import org.key_project.solidity.common.Services;
import org.key_project.solidity.logic.op.ProgramVariable;
import org.key_project.solidity.program.ast.SolidityProgramElement;
import org.key_project.solidity.program.ast.declarations.FunctionEnums.DataLocation;

public class ProgramVariableSVSort extends ProgramSVSort {

    /// Restricts which program variables this SV may match. `ANY` admits every program variable;
    /// location-specific filters admit only the corresponding Solidity local kind. Whether a
    /// variable holds a plain value is not a variable property here — rules discriminate that
    /// through the accessed field/path instead.
    public enum Filter {
        ANY, STORAGE_LOCAL, MEMORY_LOCAL
    }

    private static final Map<String, ProgramSVSort> PARAMETERIZED_SORTS = new HashMap<>();

    private final Filter filter;

    protected ProgramVariableSVSort(Name name) {
        this(name, Filter.ANY);
    }

    private ProgramVariableSVSort(Name name, Filter filter) {
        super(name);
        this.filter = filter;
    }

    public Filter getFilter() {
        return filter;
    }

    @Override
    public boolean canStandFor(SolidityProgramElement pe, Services services) {
        if (!(pe instanceof ProgramVariable pv)) {
            return false;
        }
        return switch (filter) {
            case ANY -> true;
            case STORAGE_LOCAL -> pv.getDataLocation() == DataLocation.Storage;
            case MEMORY_LOCAL -> pv.getDataLocation() == DataLocation.Memory;
        };
    }

    /// A program variable is itself a logic term, so it may match in term position (the update
    /// calculus relies on this, e.g. `{pv := t}pv`).
    @Override
    public boolean canStandFor(Term t) {
        if (!(t.op() instanceof ProgramVariable pv)) {
            return false;
        }
        return switch (filter) {
            case ANY -> true;
            case STORAGE_LOCAL -> pv.getDataLocation() == DataLocation.Storage;
            case MEMORY_LOCAL -> pv.getDataLocation() == DataLocation.Memory;
        };
    }

    @Override
    public boolean mayOccurInTermPosition() {
        return true;
    }

    @Override
    public ProgramSVSort createInstance(String parameter) {
        ProgramSVSort cached = PARAMETERIZED_SORTS.get(parameter);
        if (cached != null) {
            return cached;
        }
        Filter f = switch (parameter.toLowerCase(Locale.ROOT)) {
            case "storage", "storage,local" -> Filter.STORAGE_LOCAL;
            case "memory", "memory,local" -> Filter.MEMORY_LOCAL;
            default -> throw new IllegalArgumentException(
                "Unknown Variable sort flag '" + parameter
                    + "' (expected 'storage' or 'memory')");
        };
        ProgramSVSort result =
            new ProgramVariableSVSort(new Name("Variable[" + parameter + "]"),
                f);
        PARAMETERIZED_SORTS.put(parameter, result);
        return result;
    }
}
