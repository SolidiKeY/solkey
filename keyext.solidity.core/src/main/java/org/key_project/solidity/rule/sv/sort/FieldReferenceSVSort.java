/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.rule.sv.sort;

import org.key_project.logic.Name;
import org.key_project.logic.Term;
import org.key_project.solidity.common.Services;
import org.key_project.solidity.program.ast.SolidityProgramElement;
import org.key_project.solidity.program.ast.references.FieldReference;

/// Program schema variable sort matching a [FieldReference], i.e. an in-program access to a
/// contract state variable. A taclet can bind such a schema variable and then resolve the field's
/// logic symbol with the `#fieldSymbol` term transformer
/// ([org.key_project.solidity.rule.metaconstruct.MetaFieldSymbol]).
public class FieldReferenceSVSort extends ProgramSVSort {

    public FieldReferenceSVSort() {
        super(new Name("FieldReference"));
    }

    @Override
    public boolean canStandFor(Term t) {
        return t.op() instanceof FieldReference;
    }

    @Override
    public boolean canStandFor(SolidityProgramElement pe, Services services) {
        return pe instanceof FieldReference;
    }
}
