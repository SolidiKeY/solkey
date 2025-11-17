/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.common;


import org.key_project.logic.LogicServices;
import org.key_project.prover.proof.ProofServices;
import org.key_project.prover.proof.SessionCaches;
import org.key_project.solidity.logic.NamespaceSet;
import org.key_project.solidity.logic.TermBuilder;
import org.key_project.solidity.logic.TermFactory;
import org.key_project.solidity.program.ast.SolidityModel;
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
}
