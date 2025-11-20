/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.rule.matching;

import org.key_project.logic.LogicServices;
import org.key_project.logic.SyntaxElement;
import org.key_project.logic.Term;
import org.key_project.logic.op.sv.SchemaVariable;
import org.key_project.prover.rules.TacletMatcher;
import org.key_project.prover.rules.instantiation.AssumesFormulaInstantiation;
import org.key_project.prover.rules.instantiation.AssumesMatchResult;
import org.key_project.prover.rules.instantiation.MatchResultInfo;
import org.key_project.solidity.rule.SolTaclet;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class VMTacletMatcher implements TacletMatcher {
    public VMTacletMatcher(SolTaclet taclet) {

    }

    @Override
    public @Nullable MatchResultInfo matchFind(@NonNull Term term,
            @NonNull MatchResultInfo matchCond, @NonNull LogicServices services) {
        return null;
    }

    @Override
    public @Nullable MatchResultInfo checkVariableConditions(@Nullable SchemaVariable var,
            @Nullable SyntaxElement instantiationCandidate, @Nullable MatchResultInfo matchCond,
            @NonNull LogicServices services) {
        return null;
    }

    @Override
    public @Nullable MatchResultInfo checkConditions(@Nullable MatchResultInfo matchResultInfo,
            @NonNull LogicServices services) {
        return null;
    }

    @Override
    public @NonNull AssumesMatchResult matchAssumes(
            @NonNull Iterable<@NonNull AssumesFormulaInstantiation> toMatch, @NonNull Term template,
            @NonNull MatchResultInfo matchCond, @NonNull LogicServices services) {
        return null;
    }

    @Override
    public @Nullable MatchResultInfo matchAssumes(
            @NonNull Iterable<AssumesFormulaInstantiation> toMatch,
            @NonNull MatchResultInfo matchCond, @NonNull LogicServices services) {
        return null;
    }

    @Override
    public @Nullable MatchResultInfo matchSV(SchemaVariable sv, SyntaxElement se,
            MatchResultInfo matchCond, LogicServices services) {
        return null;
    }
}
