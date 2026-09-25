/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.parser.varcond;

import org.key_project.logic.SyntaxElement;
import org.key_project.logic.Term;
import org.key_project.logic.op.sv.SchemaVariable;
import org.key_project.solidity.common.Services;
import org.key_project.solidity.logic.op.FixedArrayField;
import org.key_project.solidity.rule.VariableConditionAdapter;
import org.key_project.solidity.rule.matching.inst.SVInstantiations;

/// `\fixedLength(sv)` holds when `sv` is instantiated with a fixed-size array field, or with a
/// path list whose last field is one, so `#fixedLength(sv)` can produce its declared length.
public class FixedLengthCondition extends VariableConditionAdapter {
    private final SchemaVariable path;
    private final boolean negated;

    public FixedLengthCondition(SchemaVariable path, boolean negated) {
        this.path = path;
        this.negated = negated;
    }

    @Override
    public boolean check(SchemaVariable var, SyntaxElement instCandidate, SVInstantiations instMap,
            Services services) {
        if (var != path) {
            return true;
        }
        boolean fixed =
            instCandidate instanceof Term term && FixedArrayField.lengthOf(term).isPresent();
        return fixed != negated;
    }

    @Override
    public String toString() {
        return (negated ? "\\not " : "") + "\\fixedLength(" + path.name() + ")";
    }
}
