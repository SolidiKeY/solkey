/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.rule.metaconstruct;

import org.key_project.logic.Term;
import org.key_project.logic.TerminalSyntaxElement;
import org.key_project.logic.op.Operator;
import org.key_project.logic.op.SortedOperator;
import org.key_project.solidity.common.Services;
import org.key_project.solidity.rule.matching.inst.SVInstantiations;

/// TermTransformer perform complex term transformation which cannot be (efficiently or at all)
/// described by taclets.
public interface TermTransformer extends SortedOperator, Operator,
        /* TODO: check */ TerminalSyntaxElement {

    /// initiates term transformation of <tt>term</tt>. Note the top level operator of parameter
    /// <tt>term</tt> has to be _this_ term transformer.
    Term transform(Term term, SVInstantiations svInst, Services services);

    /// as [#transform(Term,SVInstantiations,Services)], but additionally receives the
    /// uninstantiated taclet term this transformer occurred in, so implementations can inspect
    /// the schema variables (and via <tt>svInst</tt> their program-element instantiations) that
    /// produced the already instantiated <tt>term</tt>.
    default Term transform(Term schemaTerm, Term term, SVInstantiations svInst,
            Services services) {
        return transform(term, svInst, services);
    }
}
