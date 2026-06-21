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

    /// Restricts which program variables this SV may match. `ANY` admits every program variable
    /// (legacy behavior); `STORAGE_LOCAL` and `NON_STORAGE_LOCAL` partition them by whether the
    /// variable is a `storage`-qualified local, which the converter resorts to `List`. The two
    /// filters together let rules with overlapping `\find` patterns (e.g.
    /// `storageRootReadSelect` and `storageLocalRootRebind`) become
    /// pairwise disjoint without relying on the update RHS's sort check to reject the wrong rule.
    public enum Filter {
        ANY, STORAGE_LOCAL, NON_STORAGE_LOCAL
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

    @Override
    public boolean canStandFor(SolidityProgramElement pe, Services services) {
        if (!(pe instanceof ProgramVariable pv)) {
            return false;
        }
        return switch (filter) {
            case ANY -> true;
            case STORAGE_LOCAL -> pv.getDataLocation() == DataLocation.Storage;
            case NON_STORAGE_LOCAL -> pv.getDataLocation() != DataLocation.Storage;
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
            case NON_STORAGE_LOCAL -> pv.getDataLocation() != DataLocation.Storage;
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
            case "storage", "storage.local" -> Filter.STORAGE_LOCAL;
            case "non-storage", "value" -> Filter.NON_STORAGE_LOCAL;
            default -> throw new IllegalArgumentException(
                "Unknown Variable sort flag '" + parameter + "' (expected 'storage' or 'value')");
        };
        ProgramSVSort result =
            new ProgramVariableSVSort(new Name("Variable[name=" + parameter + "]"),
                f);
        PARAMETERIZED_SORTS.put(parameter, result);
        return result;
    }
}
