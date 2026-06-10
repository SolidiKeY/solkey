/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.proof.mgt;

import java.util.LinkedHashSet;
import java.util.Set;

import org.key_project.prover.rules.RuleApp;
import org.key_project.solidity.proof.Proof;
import org.key_project.solidity.proof.ProofEvent;
import org.key_project.solidity.proof.ProofTreeAdapter;
import org.key_project.solidity.proof.ProofTreeEvent;
import org.key_project.solidity.proof.RuleAppListener;
import org.key_project.solidity.speclang.Contract;
import org.key_project.util.collection.DefaultImmutableSet;
import org.key_project.util.collection.ImmutableSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ProofCorrectnessMgt {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProofCorrectnessMgt.class);

    private final DefaultMgtProofListener proofListener = new DefaultMgtProofListener();
    private final DefaultMgtProofTreeListener proofTreeListener = new DefaultMgtProofTreeListener();

    private final Proof proof;
    private final SpecificationRepository specRepos;

    private final Set<RuleApp> cachedRuleApps = new LinkedHashSet<>();
    private ProofStatus proofStatus = ProofStatus.OPEN;

    // -------------------------------------------------------------------------
    // constructors
    // -------------------------------------------------------------------------

    public ProofCorrectnessMgt(Proof p) {
        this.proof = p;
        this.specRepos = p.getServices().getSpecificationRepository();
    }

    // -------------------------------------------------------------------------
    // internal methods
    // -------------------------------------------------------------------------
    /*
     *
     * private boolean allHaveMeasuredBy(ImmutableList<Contract> contracts) {
     * for (Contract contract : contracts) {
     * if (!contract.hasMby()) {
     * return false;
     * }
     * }
     * return true;
     * }
     */
    // -------------------------------------------------------------------------
    // public interface
    // -------------------------------------------------------------------------

    public RuleJustification getJustification(RuleApp r) {
        return proof.getInitConfig().getJustifInfo().getJustification(r, proof.getServices());
    }


    public ImmutableSet<Contract> getUsedContracts() {
        ImmutableSet<Contract> result = DefaultImmutableSet.nil();
        for (RuleApp ruleApp : cachedRuleApps) {
            RuleJustification ruleJusti = getJustification(ruleApp);
            if (ruleJusti instanceof RuleJustificationBySpec) {
                Contract contract = ((RuleJustificationBySpec) ruleJusti).spec();
                ImmutableSet<Contract> atomicContracts = specRepos.splitContract(contract);
                assert atomicContracts != null;
                atomicContracts = specRepos.getInheritedContracts(atomicContracts);
                result = result.union(atomicContracts);
            }
        }
        return result;
    }

    public void updateProofStatus() {
        final ImmutableSet<Proof> all = specRepos.getAllProofs();

        // mark open proofs as open, all others as presumably closed
        ImmutableSet<Proof> presumablyClosed = DefaultImmutableSet.nil();
        for (Proof p : all) {
            if (!p.isDisposed()) {
                if (p.openGoals().size() > 0) {
                    // some branch is open
                    p.mgt().proofStatus = ProofStatus.OPEN;
                } else {
                    // all branches are properly closed
                    p.mgt().proofStatus = ProofStatus.CLOSED;
                    presumablyClosed = presumablyClosed.add(p);
                }
            }
        }

        // revert status of all "presumably closed" proofs for which at least one
        // used contract is definitely not proven to "lemmas left"
        boolean changed = true;
        while (changed) {
            changed = false;
            for (Proof p : presumablyClosed) {
                for (Contract usedContract : p.mgt().getUsedContracts()) {
                    final ImmutableSet<Proof> usedProofs = specRepos.getProofs(usedContract);
                    if (usedProofs.isEmpty()) {
                        p.mgt().proofStatus = ProofStatus.CLOSED_BUT_LEMMAS_LEFT;
                        presumablyClosed = presumablyClosed.remove(p);
                        changed = true;
                    } else {
                        for (Proof usedProof : usedProofs) {
                            if (usedProof.mgt().proofStatus != ProofStatus.CLOSED) {
                                p.mgt().proofStatus = ProofStatus.CLOSED_BUT_LEMMAS_LEFT;
                                presumablyClosed = presumablyClosed.remove(p);
                                changed = true;
                                break;
                            }
                        }
                    }
                }
            }
        }
    }

    public void ruleApplied(RuleApp r) {
        RuleJustification rj = getJustification(r);
        if (rj == null) {
            LOGGER.debug("No justification found for rule " + r.rule().name());
            return;
        }
        if (!rj.isAxiomJustification()) {
            cachedRuleApps.add(r);
        }
    }


    public void ruleUnApplied(RuleApp r) {
        cachedRuleApps.remove(r);
    }


    /// Tells whether a contract for the passed target may be applied in the passed goal without
    /// creating circular dependencies.
    public boolean isContractApplicable(Object /* Contract */ contract) {
        throw new RuntimeException("Not yet implemented");
    }

    public void removeProofListener() {
        proof.removeRuleAppListener(proofListener);
    }

    // -------------------------------------------------------------------------
    // inner classes
    // -------------------------------------------------------------------------

    private class DefaultMgtProofListener implements RuleAppListener {
        @Override
        public void ruleApplied(ProofEvent e) {
            ProofCorrectnessMgt.this.ruleApplied(e.getRuleAppInfo().getRuleApp());
        }
    }


    private class DefaultMgtProofTreeListener extends ProofTreeAdapter {
        @Override
        public void proofClosed(ProofTreeEvent e) {
            updateProofStatus();
        }

        @Override
        public void proofStructureChanged(ProofTreeEvent e) {
            updateProofStatus();
        }
    }
}
