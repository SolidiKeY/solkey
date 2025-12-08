/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.common;


import java.util.HashMap;
import java.util.LinkedHashMap;

import org.key_project.logic.LogicServices;
import org.key_project.logic.Name;
import org.key_project.logic.Term;
import org.key_project.prover.proof.ProofServices;
import org.key_project.prover.proof.SessionCaches;
import org.key_project.solidity.common.naming.NameRecorder;
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
    private final TermFactory tf;
    private final TermBuilder tb;
    private TheoryInfo theoryInfo;

    /// variable namer for inner renaming
    @SuppressWarnings({ "assignment.type.incompatible", "argument.type.incompatible" })
    private final VariableNamer innerVarNamer = new VariableNamer(this);

    /// map of names to counters
    private final HashMap<String, Counter> counters;
    private Proof proof;
    private NameRecorder nameRecorder;


    public Services() {
        tf = new TermFactory();
        tb = new TermBuilder(tf, this);
        counters = new LinkedHashMap<>();
        solidityModel = new SolidityModel();
    }

    @SuppressWarnings({ "argument.type.incompatible", "assignment.type.incompatible",
        "initialization.fields.uninitialized" })
    public Services(Services services) {
        this.namespaces = services.namespaces;
        // this.ldts = services.ldts;
        this.tf = new TermFactory();
        this.tb = new TermBuilder(tf, this);
        this.proof = services.proof;
        // this.profile = services.profile;
        this.counters = services.counters;
        // this.caches = services.caches;
        // this.specRepos = services.specRepos;
        this.solidityModel = services.solidityModel;
        // this.solidityInfo = services.solidityInfo;
        nameRecorder = services.nameRecorder;
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

    public TermFactory getTf() {
        return tf;
    }

    public TermBuilder getTb() {
        return tb;
    }

    public TheoryInfo getTheoryInfo() {
        return theoryInfo;
    }

    public void setTheoryInfo(TheoryInfo theoryInfo) {
        this.theoryInfo = theoryInfo;
    }

    /// this functionality should be moved to an external class
    public static Term convertToLogicElement(SolidityProgramElement pe, Services services) {
        var tb = services.getTb();
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
        nameRecorder.addProposal(name);
    }

    public void setProof(Proof proof) {
        this.proof = proof;
    }

    public void setNamespaces(NamespaceSet ns) {
        throw new RuntimeException("Not implemented yet");
    }

    public Profile getProfile() {
        throw new RuntimeException("Not implemented yet");
    }

    public void saveNameRecorder(Node n) {
        n.setNameRecorder(nameRecorder);
        nameRecorder = new NameRecorder();
    }

    public Services getOverlay(NamespaceSet localNamespaces) {
        Services result = new Services(this);
        result.setNamespaces(namespaces);
        return result;
    }

    public SpecificationRepository getSpecificationRepository() {
        throw new RuntimeException("Not implemented yet");
    }

    public Counter getCounter(String name) {
        Counter c = counters.get(name);
        if (c != null) {
            return c;
        }
        c = new Counter(name);
        counters.put(name, c);
        return c;
    }

    public NameRecorder getNameRecorder() {
        return nameRecorder;
    }
}
