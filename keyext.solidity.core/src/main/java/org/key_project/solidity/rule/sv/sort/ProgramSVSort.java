/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.rule.sv.sort;

import org.key_project.logic.Name;
import org.key_project.solidity.common.Services;
import org.key_project.solidity.logic.sort.SortImpl;
import org.key_project.solidity.program.ast.SolidityProgramElement;

public abstract class ProgramSVSort extends SortImpl {

    public static final ProgramSVSort VARIABLE = null;

    public ProgramSVSort(Name name) {
        super(name);
    }

    public boolean canStandFor(SolidityProgramElement pe, Services services) {
        throw new RuntimeException("Not implemented yet");
    }
}
