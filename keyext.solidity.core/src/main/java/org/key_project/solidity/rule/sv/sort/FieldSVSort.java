/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.rule.sv.sort;

import org.key_project.logic.Name;
import org.key_project.solidity.common.Services;
import org.key_project.solidity.program.ast.SolidityProgramElement;
import org.key_project.solidity.program.ast.declarations.FieldDeclaration;

/// Program schema variable sort matching a [FieldDeclaration], i.e. a struct field or contract
/// state variable declaration. Used in taclets to match the field part of a member expression
/// like `alice.age` where `age` is a FieldDeclaration.
public class FieldSVSort extends ProgramSVSort {

    public FieldSVSort() {
        super(new Name("Field"));
    }

    @Override
    public boolean canStandFor(SolidityProgramElement pe, Services services) {
        return pe instanceof FieldDeclaration;
    }
}
