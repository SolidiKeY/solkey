/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.common;


import org.key_project.logic.LogicServices;
import org.key_project.logic.Name;
import org.key_project.logic.Term;
import org.key_project.prover.proof.ProofServices;
import org.key_project.prover.proof.SessionCaches;
import org.key_project.solidity.common.naming.VariableNamer;
import org.key_project.solidity.logic.NamespaceSet;
import org.key_project.solidity.logic.TermBuilder;
import org.key_project.solidity.logic.TermFactory;
import org.key_project.solidity.logic.op.ProgramVariable;
import org.key_project.solidity.program.ast.SolidityModel;
import org.key_project.solidity.program.ast.SolidityProgramElement;
import org.key_project.solidity.proof.Counter;
import org.key_project.solidity.proof.Node;
import org.key_project.solidity.proof.Proof;
import org.key_project.solidity.proof.mgt.SpecificationRepository;
import org.key_project.solidity.theory.TheoryInfo;

import org.jspecify.annotations.NonNull;

public class Services implements LogicServices, ProofServices {

    /**
     * proof specific namespaces (functions, predicates, sorts, variables)
     */
    private NamespaceSet namespaces = new NamespaceSet();
    private SolidityModel solidityModel;
    private final TermFactory termFactory;
    private final TermBuilder termBuilder;
    private TheoryInfo theoryInfo;

    public Services() {
        termFactory = new TermFactory();
        termBuilder = new TermBuilder(termFactory, this);
    }

    public @NonNull NamespaceSet getNamespaces() {
        return namespaces;
    }

    public SolidityModel getSolidityInfo() {
        return solidityModel;
    }

    @Override
    public SessionCaches getCaches() {
        return null;
    }

    public TermFactory getTermFactory() {
        return termFactory;
    }

    public TermBuilder getTermBuilder() {
        return null;
    }

    public TheoryInfo getTheoryInfo() {
        return theoryInfo;
    }

    public void setTheoryInfo(TheoryInfo ldTs) {
        this.theoryInfo = ldTs;
    }

    /// this functionality should be moved to an external class
    public static Term convertToLogicElement(SolidityProgramElement pe, Services services) {
        var tb = services.getTermBuilder();
        if (pe instanceof ProgramVariable pv) {
            return tb.var(pv);
        }
        throw new IllegalArgumentException(
            "Unknown or not convertible ProgramElement " + pe + " of type "
                + pe.getClass());
    }

    public VariableNamer getVariableNamer() {
        throw new RuntimeException("Not implemented yet");
    }

    public void addNameProposal(Name name) {
        throw new RuntimeException("Not implemented yet");
    }

    public void setProof(Proof proof) {
        throw new RuntimeException("Not implemented yet");
    }

    public void setNamespaces(NamespaceSet ns) {
        throw new RuntimeException("Not implemented yet");
    }

    public Profile getProfile() {
        throw new RuntimeException("Not implemented yet");
    }

    public void saveNameRecorder(Node n) {
        throw new RuntimeException("Not implemented yet");
    }

    public Services getOverlay(NamespaceSet localNamespaces) {
        throw new RuntimeException("Not implemented yet");
    }

    public SpecificationRepository getSpecificationRepository() {
        throw new RuntimeException("Not implemented yet");
    }

    public Counter getCounter(String nodes) {
        throw new RuntimeException("Not implemented yet");
    }
}
