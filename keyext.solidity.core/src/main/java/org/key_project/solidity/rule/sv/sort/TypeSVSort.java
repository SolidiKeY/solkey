/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.rule.sv.sort;

import org.key_project.logic.Name;
import org.key_project.solidity.common.Services;
import org.key_project.solidity.program.ast.SolidityProgramElement;
import org.key_project.solidity.program.ast.abstractions.Type;

/// Program schema variable sort matching a Solidity [Type] (e.g. a primitive type, a contract or
/// struct type). Declared in `.key` files as `\program Type t`.
public class TypeSVSort extends ProgramSVSort {

    public TypeSVSort() {
        super(new Name("Type"));
    }

    @Override
    public boolean canStandFor(SolidityProgramElement pe, Services services) {
        return pe instanceof Type;
    }
}
