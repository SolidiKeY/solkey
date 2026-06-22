/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.parser.varcond;

import org.key_project.logic.LogicServices;
import org.key_project.logic.SyntaxElement;
import org.key_project.logic.op.sv.SchemaVariable;
import org.key_project.prover.rules.VariableCondition;
import org.key_project.prover.rules.instantiation.MatchResultInfo;
import org.key_project.solidity.program.ast.SolidityProgramElement;
import org.key_project.solidity.program.ast.expressions.IndexExpression;
import org.key_project.solidity.program.ast.expressions.MemberExp;
import org.key_project.solidity.rule.matching.inst.SVInstantiations;

import org.jspecify.annotations.Nullable;

/// Variable condition `\pathBase(deepPath, basePath)` extracting the base path from a deep path.
///
/// For a [MemberExp] like `alice.account.balance`, the base is `alice.account`.
/// For an [IndexExpression] like `arr[i][j]`, the base is `arr[i]`.
///
/// This condition instantiates the `basePath` schema variable with the left (base) expression
/// of the deep path.
public final class PathBaseCondition implements VariableCondition {
    private final SchemaVariable deepPathSV;
    private final SchemaVariable baseSV;

    public PathBaseCondition(SchemaVariable deepPathSV, SchemaVariable baseSV) {
        this.deepPathSV = deepPathSV;
        this.baseSV = baseSV;
    }

    @Override
    @Nullable
    public MatchResultInfo check(SchemaVariable var, SyntaxElement svSubst,
            MatchResultInfo matchCond, LogicServices lServices) {
        if (var != deepPathSV && var != baseSV) {
            return matchCond;
        }
        final SVInstantiations inst = (SVInstantiations) matchCond.getInstantiations();

        // Resolve the deep path
        final Object deepInst = var == deepPathSV ? svSubst : inst.getInstantiation(deepPathSV);
        if (deepInst == null) {
            // deepPath not yet matched; defer
            return matchCond;
        }
        if (!(deepInst instanceof SolidityProgramElement pe)) {
            return null;
        }

        // Extract the base from the deep path
        final SolidityProgramElement base;
        if (pe instanceof MemberExp member) {
            base = (SolidityProgramElement) member.getLeftExp();
        } else if (pe instanceof IndexExpression index) {
            base = (SolidityProgramElement) index.getLeftExp();
        } else {
            return null;
        }

        // Check or bind the base schema variable
        final Object baseInst = var == baseSV ? svSubst : inst.getInstantiation(baseSV);
        if (baseInst == null) {
            // baseSV is still free: bind it
            return matchCond.setInstantiations(inst.add(baseSV, base, lServices));
        }
        // Check that existing instantiation matches
        return base.equals(baseInst) ? matchCond : null;
    }

    @Override
    public String toString() {
        return "\\pathBase(" + deepPathSV + ", " + baseSV + ")";
    }
}
