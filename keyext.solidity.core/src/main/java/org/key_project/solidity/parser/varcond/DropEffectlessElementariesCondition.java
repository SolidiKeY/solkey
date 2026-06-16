/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.parser.varcond;

import java.util.LinkedHashSet;
import java.util.Set;

import org.key_project.logic.LogicServices;
import org.key_project.logic.SyntaxElement;
import org.key_project.logic.Term;
import org.key_project.logic.op.sv.SchemaVariable;
import org.key_project.prover.rules.VariableCondition;
import org.key_project.prover.rules.instantiation.MatchResultInfo;
import org.key_project.solidity.common.Services;
import org.key_project.solidity.logic.op.ElementaryUpdate;
import org.key_project.solidity.logic.op.ProgramVariable;
import org.key_project.solidity.logic.op.SModality;
import org.key_project.solidity.logic.op.UpdateApplication;
import org.key_project.solidity.logic.op.UpdateJunctor;
import org.key_project.solidity.program.ast.visitor.BoundedProgramVariableCollector;
import org.key_project.solidity.rule.matching.inst.SVInstantiations;
import org.key_project.solidity.rule.sv.UpdateSV;

public final class DropEffectlessElementariesCondition implements VariableCondition {
    private final UpdateSV u;
    private final SchemaVariable x;
    private final SchemaVariable result;

    public DropEffectlessElementariesCondition(UpdateSV u,
            SchemaVariable x,
            SchemaVariable x2) {
        this.u = u;
        this.x = x;
        this.result = x2;
    }

    private static Term dropEffectlessElementariesHelper(Term update,
            Set<ProgramVariable> relevantVars, Services services) {
        if (update.op() instanceof ElementaryUpdate eu) {
            var lhs = (ProgramVariable) eu.lhs();
            if (relevantVars.contains(lhs)) {
                relevantVars.remove(lhs);
                return null;
            } else {
                return services.getTermBuilder().skip();
            }
        } else if (update.op() == UpdateJunctor.PARALLEL_UPDATE) {
            Term sub0 = update.sub(0);
            Term sub1 = update.sub(1);
            // first descend to the second sub-update to keep relevantVars in
            // good order
            Term newSub1 = dropEffectlessElementariesHelper(sub1, relevantVars, services);
            Term newSub0 = dropEffectlessElementariesHelper(sub0, relevantVars, services);
            if (newSub0 == null && newSub1 == null) {
                return null;
            } else {
                newSub0 = newSub0 == null ? sub0 : newSub0;
                newSub1 = newSub1 == null ? sub1 : newSub1;
                return services.getTermBuilder().parallel(newSub0, newSub1);
            }
        } else if (update.op() == UpdateApplication.UPDATE_APPLICATION) {
            Term sub0 = update.sub(0);
            Term sub1 = update.sub(1);
            Term newSub1 = dropEffectlessElementariesHelper(sub1, relevantVars, services);
            return newSub1 == null ? null : services.getTermBuilder().apply(sub0, newSub1);
        } else {
            return null;
        }
    }


    private static Term dropEffectlessElementaries(Term update, Term target, Services services) {
        // The helper only ever queries the relevant-vars set with the update's LHS variables, so
        // we only need to know which of *those* occur in the target. Collecting just the update's
        // LHS variables and searching the target for them — with early termination once all are
        // found — avoids the full program walk that made symbolic execution quadratic (each
        // update-simplification step otherwise re-walked the whole remaining program).
        final Set<ProgramVariable> updateVars = new LinkedHashSet<>();
        collectUpdateLhsVars(update, updateVars);

        final Set<ProgramVariable> relevantVars = new LinkedHashSet<>();
        searchTerm(target, updateVars, relevantVars, services);

        Term simplifiedUpdate = dropEffectlessElementariesHelper(update, relevantVars, services);
        return simplifiedUpdate == null ? null
                : services.getTermBuilder().apply(simplifiedUpdate, target);
    }

    /// Collects the LHS variables of the elementary updates that
    /// [#dropEffectlessElementariesHelper] can reach, mirroring its traversal exactly (both sides
    /// of a parallel update; only the inner update of an update application).
    private static void collectUpdateLhsVars(Term update, Set<ProgramVariable> out) {
        final var op = update.op();
        if (op instanceof ElementaryUpdate eu) {
            out.add((ProgramVariable) eu.lhs());
        } else if (op == UpdateJunctor.PARALLEL_UPDATE) {
            collectUpdateLhsVars(update.sub(0), out);
            collectUpdateLhsVars(update.sub(1), out);
        } else if (op == UpdateApplication.UPDATE_APPLICATION) {
            collectUpdateLhsVars(update.sub(1), out);
        }
    }

    /// Records in `found` those variables of `interested` that occur in `term` — either as a term
    /// operator or inside a non-empty modality's program. Across a modality the contract storage
    /// and memory variables are always protected (the program may access them implicitly), and the
    /// program walk uses a [BoundedProgramVariableCollector] so it stops as soon as all interested
    /// variables are found. The recursion itself descends only until `found` already covers
    /// `interested`.
    private static void searchTerm(Term term, Set<ProgramVariable> interested,
            Set<ProgramVariable> found, Services services) {
        if (found.size() == interested.size()) {
            return;
        }
        final var op = term.op();
        if (op instanceof ProgramVariable pv && interested.contains(pv)) {
            found.add(pv);
        } else if (op instanceof SModality mod && !mod.programBlock().isEmpty()) {
            // protect storage and memory across modalities (the program may access them
            // implicitly), accessed via the respective LDT getters
            ProgramVariable storage = services.getTheoryInfo().getStructLDT().getStorage();
            ProgramVariable memory = services.getTheoryInfo().getMemoryLDT().getBaseMemory();
            if (interested.contains(storage)) {
                found.add(storage);
            }
            if (interested.contains(memory)) {
                found.add(memory);
            }
            if (found.size() < interested.size()) {
                BoundedProgramVariableCollector pvc = new BoundedProgramVariableCollector(
                    mod.programBlock().program(), services, interested);
                pvc.start();
                for (ProgramVariable v : interested) {
                    if (pvc.result().contains(v)) {
                        found.add(v);
                    }
                }
            }
        }
        for (int i = 0, n = term.arity(); i < n && found.size() < interested.size(); i++) {
            searchTerm(term.sub(i), interested, found, services);
        }
    }

    @Override
    public MatchResultInfo check(SchemaVariable var, SyntaxElement instCandidate,
            MatchResultInfo mc,
            LogicServices services) {
        SVInstantiations svInst = (SVInstantiations) mc.getInstantiations();
        Term uInst = svInst.getInstantiation(u);
        Term xInst = svInst.getInstantiation(x);
        Term resultInst = svInst.getInstantiation(result);
        if (uInst == null || xInst == null) {
            return mc;
        }

        Term properResultInst = dropEffectlessElementaries(uInst, xInst, (Services) services);
        if (properResultInst == null) {
            return null;
        } else if (resultInst == null) {
            svInst = svInst.add(result, properResultInst, services);
            return mc.setInstantiations(svInst);
        } else if (resultInst.equals(properResultInst)) {
            return mc;
        } else {
            return null;
        }
    }


    @Override
    public String toString() {
        return "\\dropEffectlessElementaries(" + u + ", " + x + ", " + result + ")";
    }
}
