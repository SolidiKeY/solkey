/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.parser.varcond;

import org.key_project.logic.SyntaxElement;
import org.key_project.logic.op.sv.SchemaVariable;
import org.key_project.solidity.common.Services;
import org.key_project.solidity.program.ast.StorageDeleteTypes;
import org.key_project.solidity.program.ast.declarations.FieldDeclaration;
import org.key_project.solidity.rule.VariableConditionAdapter;
import org.key_project.solidity.rule.matching.inst.SVInstantiations;

public class NoFixedArrayElementCondition extends VariableConditionAdapter {
    private final SchemaVariable field;

    public NoFixedArrayElementCondition(SchemaVariable field) {
        this.field = field;
    }

    @Override
    public boolean check(SchemaVariable var, SyntaxElement instCandidate, SVInstantiations instMap,
            Services services) {
        if (var != field) {
            return true;
        }
        return instCandidate instanceof FieldDeclaration fd && !StorageDeleteTypes
                .hasFixedArrayElement(fd.getTypeReference().resolvedType());
    }

    @Override
    public String toString() {
        return "\\noFixedArrayElement(" + field + ")";
    }
}
