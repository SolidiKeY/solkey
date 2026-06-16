/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.rule.sv.sort;

import org.key_project.logic.Name;
import org.key_project.solidity.common.Services;
import org.key_project.solidity.program.ast.SolidityProgramElement;
import org.key_project.solidity.program.ast.references.FieldReference;

/// Program schema variable sort matching a [FieldReference], i.e. an in-program access to a
/// contract state variable. A taclet binds such a schema variable inside a modality; the field's
/// logic symbol is then resolved when the program element is converted to a term (see
/// [org.key_project.solidity.common.Services#convertToLogicElement]), e.g. via the
/// `\sameAsTerm` variable condition.
public class FieldReferenceSVSort extends ProgramSVSort {

    public FieldReferenceSVSort() {
        super(new Name("FieldReference"));
    }

    @Override
    public boolean canStandFor(SolidityProgramElement pe, Services services) {
        return pe instanceof FieldReference;
    }
}
