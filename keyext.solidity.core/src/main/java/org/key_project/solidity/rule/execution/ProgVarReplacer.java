/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.rule.execution;

import java.util.Map;

import org.key_project.logic.Term;
import org.key_project.logic.op.Operator;
import org.key_project.logic.op.QuantifiableVariable;
import org.key_project.logic.op.sv.SchemaVariable;
import org.key_project.prover.rules.instantiation.AssumesFormulaInstantiation;
import org.key_project.prover.rules.instantiation.InstantiationEntry;
import org.key_project.prover.sequent.Semisequent;
import org.key_project.prover.sequent.Sequent;
import org.key_project.prover.sequent.SequentChangeInfo;
import org.key_project.prover.sequent.SequentFormula;
import org.key_project.solidity.common.Services;
import org.key_project.solidity.logic.SolidityBlock;
import org.key_project.solidity.logic.op.ElementaryUpdate;
import org.key_project.solidity.logic.op.ProgramVariable;
import org.key_project.solidity.logic.op.SModality;
import org.key_project.solidity.program.ast.SolidityProgramElement;
import org.key_project.solidity.program.ast.visitor.ProgVarReplaceVisitor;
import org.key_project.solidity.proof.TacletIndex;
import org.key_project.solidity.rule.NoPosTacletApp;
import org.key_project.solidity.rule.matching.inst.SVInstantiations;
import org.key_project.util.collection.DefaultImmutableSet;
import org.key_project.util.collection.ImmutableArray;
import org.key_project.util.collection.ImmutableList;
import org.key_project.util.collection.ImmutableMapEntry;
import org.key_project.util.collection.ImmutableSet;

import org.checkerframework.checker.nullness.qual.Nullable;

/// Renames program-variable occurrences across a sequent and a taclet index according to
/// `renamingMap`. Mirrors `de.uka.ilkd.key.proof.ProgVarReplacer` (the Java port at
/// `key.core/.../proof/ProgVarReplacer.java`); see [TacletExecutor#applyAddProgVars] for the
/// caller protocol. AST-level rewriting is delegated to [ProgVarReplaceVisitor]; this class owns
/// the term-, sequent-, and taclet-index-level walks.
public class ProgVarReplacer {
    private final Map<ProgramVariable, ProgramVariable> renamingMap;
    private final Services services;

    public ProgVarReplacer(Map<ProgramVariable, ProgramVariable> renamingMap, Services services) {
        this.renamingMap = renamingMap;
        this.services = services;
    }

    /// Rebuilds every partially-instantiated taclet app in `tacletIndex` whose `SVInstantiations`
    /// reference any old variable in `renamingMap`. Mirrors Java lines 72-94.
    public SequentChangeInfo replace(TacletIndex tacletIndex) {
        ImmutableList<NoPosTacletApp> partialApps = tacletIndex.getPartialInstantiatedApps();
        ImmutableSet<NoPosTacletApp> toRemove = DefaultImmutableSet.nil();
        ImmutableSet<NoPosTacletApp> toAdd = DefaultImmutableSet.nil();
        for (NoPosTacletApp app : partialApps) {
            SVInstantiations insts = app.instantiations();
            SVInstantiations newInsts = replace(insts);
            if (newInsts != insts) {
                ImmutableList<AssumesFormulaInstantiation> assumes =
                    app.assumesFormulaInstantiations();
                NoPosTacletApp newApp =
                    NoPosTacletApp.createNoPosTacletApp(app.taclet(), newInsts,
                        assumes, services);
                toRemove = toRemove.add(app);
                toAdd = toAdd.add(newApp);
            }
        }
        tacletIndex.removeTaclets(toRemove);
        tacletIndex.addTaclets(toAdd);
        return null;
    }

    /// Iterates the antecedent and succedent, rewriting each formula whose term references a
    /// renamed program variable. Mirrors Java lines 158-181.
    public SequentChangeInfo replace(Sequent sequent) {
        SequentChangeInfo result = SequentChangeInfo.createSequentChangeInfo(sequent);
        result = replaceInSemisequent(sequent.antecedent(), result, true);
        result = replaceInSemisequent(sequent.succedent(), result, false);
        return result;
    }

    private SequentChangeInfo replaceInSemisequent(Semisequent semi, SequentChangeInfo info,
            boolean inAntec) {
        for (SequentFormula sf : semi) {
            SequentFormula newSf = replace(sf);
            if (newSf != sf) {
                Sequent current = info.sequent();
                int idx = current.formulaNumberInSequent(inAntec, sf);
                info.combine(current.replaceFormula(idx, newSf));
            }
        }
        return info;
    }

    /// Rebuilds the formula if its term changed under [#replace(Term)].
    public SequentFormula replace(SequentFormula sf) {
        Term t = sf.formula();
        Term newT = replace(t);
        return newT == t ? sf : new SequentFormula(newT);
    }

    /// Top-level term dispatch (mirror of Java lines 249-259).
    public @Nullable Term replace(@Nullable Term t) {
        if (t == null) {
            return null;
        }
        Operator op = t.op();
        if (op instanceof ProgramVariable pv) {
            ProgramVariable replacement = renamingMap.get(pv);
            return replacement == null ? t : services.getTermFactory().createTerm(replacement);
        }
        return standardReplace(t);
    }

    /// Walks subterms, then rebuilds the operator if it carries program-variable state that needs
    /// renaming (`ElementaryUpdate` LHS, `SModality` program). Mirror of Java lines 206-244.
    private Term standardReplace(Term t) {
        boolean changed = false;
        Term[] newSubs = new Term[t.arity()];
        for (int i = 0, n = t.arity(); i < n; i++) {
            Term sub = t.sub(i);
            Term newSub = replace(sub);
            newSubs[i] = newSub == null ? sub : newSub;
            changed |= newSubs[i] != sub;
        }

        Operator op = t.op();
        if (op instanceof ElementaryUpdate eu && renamingMap.containsKey(eu.lhs())) {
            op = ElementaryUpdate.getInstance(renamingMap.get(eu.lhs()));
            changed = true;
        } else if (op instanceof SModality smod) {
            SolidityProgramElement oldProg = smod.programBlock().program();
            SolidityProgramElement newProg = replace(oldProg);
            if (newProg != oldProg) {
                op = SModality.getModality(smod.kind(), new SolidityBlock(newProg));
                changed = true;
            }
        }

        if (!changed) {
            return t;
        }
        @SuppressWarnings("unchecked")
        ImmutableArray<QuantifiableVariable> boundVars =
            (ImmutableArray<QuantifiableVariable>) t.boundVars();
        return services.getTermFactory().createTerm(op, newSubs, boundVars);
    }

    /// Replaces program-variable occurrences inside a Solidity program AST (mirror of Java lines
    /// 279-283).
    public SolidityProgramElement replace(SolidityProgramElement pe) {
        ProgVarReplaceVisitor v = new ProgVarReplaceVisitor(pe, renamingMap, false, services);
        v.start();
        return v.result();
    }

    /// Walks all instantiations in `insts` and rebuilds entries whose `Term`/program payload
    /// references a renamed variable. Mirror of Java lines 100-153.
    public SVInstantiations replace(SVInstantiations insts) {
        SVInstantiations result = insts;
        for (ImmutableMapEntry<SchemaVariable, InstantiationEntry<?>> e :
                (Iterable<ImmutableMapEntry<SchemaVariable, InstantiationEntry<?>>>) insts::pairIterator) {
            SchemaVariable sv = e.key();
            Object inst = e.value().getInstantiation();
            if (inst instanceof Term t) {
                Term newT = replace(t);
                if (newT != t) {
                    result = result.replace(sv, newT, services);
                }
            } else if (inst instanceof SolidityProgramElement pe) {
                SolidityProgramElement newPe = replace(pe);
                if (newPe != pe) {
                    result = result.replace(sv, newPe, services);
                }
            }
            // Other instantiation kinds (Operator, ProgramListInstantiation, context) are not
            // currently touched by `\addprogvars` callers; extend here when a test surfaces such a
            // case.
        }
        return result;
    }
}
