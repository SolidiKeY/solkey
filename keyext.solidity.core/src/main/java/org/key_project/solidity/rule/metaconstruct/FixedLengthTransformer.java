/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.rule.metaconstruct;

import org.key_project.logic.Name;
import org.key_project.logic.Term;
import org.key_project.solidity.common.Services;
import org.key_project.solidity.logic.op.FixedArrayField;
import org.key_project.solidity.rule.matching.inst.SVInstantiations;

/// `#fixedLength(t)` is the declared length of the fixed-size array field `t`, or of the last
/// field of the path list `t`. Guard its use with `\fixedLength(t)`.
public class FixedLengthTransformer extends AbstractTermTransformer {
    public FixedLengthTransformer() {
        super(new Name("#fixedLength"), 1);
    }

    @Override
    public Term transform(Term term, SVInstantiations svInst, Services services) {
        Term path = term.sub(0);
        int length = FixedArrayField.lengthOf(path).orElseThrow(() -> new IllegalStateException(
            "#fixedLength applied to " + path + ", which is not a fixed-size array field"));
        return services.getTermBuilder().zTerm(length);
    }
}
