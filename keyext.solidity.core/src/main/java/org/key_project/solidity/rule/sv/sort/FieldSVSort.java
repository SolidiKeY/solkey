/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.rule.sv.sort;

import org.key_project.logic.Name;
import org.key_project.solidity.common.Services;
import org.key_project.solidity.program.ast.SolidityProgramElement;
import org.key_project.solidity.program.ast.declarations.FieldDeclaration;

/// Program schema variable sort matching the *field-name* child of a `MemberExp`,
/// i.e. the right-hand identifier in `receiver.a`. Used in taclets of shape
/// `\find(... { s#sp.s#a = s#se; } ...)` so the receiver and field can be
/// captured as separate schema variables and re-emitted in `\replacewith`.
public class FieldSVSort extends ProgramSVSort {

    public FieldSVSort() {
        super(new Name("Field"));
    }

    @Override
    public boolean canStandFor(SolidityProgramElement pe, Services services) {
        return pe instanceof FieldDeclaration;
    }
}
