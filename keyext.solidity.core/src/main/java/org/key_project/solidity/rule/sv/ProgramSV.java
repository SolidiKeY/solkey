/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.rule.sv;

import org.key_project.logic.Name;
import org.key_project.logic.SyntaxElement;
import org.key_project.solidity.program.ast.SolidityProgramElement;
import org.key_project.solidity.rule.matching.inst.ProgramList;
import org.key_project.solidity.rule.sv.sort.ProgramSVSort;
import org.key_project.util.collection.ImmutableArray;

import org.jspecify.annotations.NonNull;

public class ProgramSV extends OperatorSV {
    private final boolean isListSV;

    private static final ProgramList EMPTY_LIST_INSTANTIATION =
        new ProgramList(new ImmutableArray<>(new SolidityProgramElement[0]));

    /// creates a new SchemaVariable used as a placeholder for program constructs
    ///
    /// @param name the Name of the SchemaVariable allowed to match a list of program constructs
    ProgramSV(Name name, ProgramSVSort s, boolean isListSV) {
        super(name, s, false, false);
        this.isListSV = isListSV;
    }

    public boolean isListSV() {
        return isListSV;
    }

    @Override
    public @NonNull SyntaxElement getChild(int n) {
        throw new IndexOutOfBoundsException("ProgramSV " + this + " has no children");
    }

    @Override
    public int getChildCount() {
        return 0;
    }
}
