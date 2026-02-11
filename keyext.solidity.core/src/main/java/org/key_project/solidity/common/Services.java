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
import org.key_project.solidity.common.naming.NameRecorder;
import org.key_project.solidity.common.naming.VariableNamer;
import org.key_project.solidity.logic.NamespaceSet;
import org.key_project.solidity.logic.TermBuilder;
import org.key_project.solidity.logic.TermFactory;
import org.key_project.solidity.logic.op.ProgramVariable;
import org.key_project.solidity.program.ast.SolidityInfo;
import org.key_project.solidity.program.ast.SolidityProgramElement;
import org.key_project.solidity.proof.Counter;
import org.key_project.solidity.proof.Node;
import org.key_project.solidity.proof.Proof;
import org.key_project.solidity.proof.SolidityModel;
import org.key_project.solidity.proof.mgt.SpecificationRepository;
import org.key_project.solidity.theory.TheoryInfo;

import org.jspecify.annotations.NonNull;

public class Services implements LogicServices, ProofServices {

    /**
     * proof specific namespaces (functions, predicates, sorts, variables)
     */
    private NamespaceSet namespaces = new NamespaceSet();
    private final TermFactory tf;
    private final TermBuilder tb;
    private TheoryInfo theoryInfo;
    private SolidityInfo solidityInfo;
    /// the Solidity model
    private SolidityModel model;

    /// variable namer for inner renaming
    private final VariableNamer variableNamer = new VariableNamer(this);
    /// records name proposals
    private NameRecorder nameRecorder;
    private final ServiceCaches caches;

    /// map of names to counters
    private final HashMap<String, Counter> counters;

    /// the proof to which this services belongs (might be null)
    private Proof proof;
    private Profile profile;

    public Services() {
        tf = new TermFactory();
        tb = new TermBuilder(tf, this);
        counters = new LinkedHashMap<>();
        solidityInfo = new SolidityInfo();
        nameRecorder = new NameRecorder();
        this.caches = new ServiceCaches();
    }

    @SuppressWarnings({ "argument.type.incompatible", "assignment.type.incompatible",
        "initialization.fields.uninitialized" })
    public Services(Services services) {
        this.namespaces = services.namespaces;
        this.theoryInfo = services.theoryInfo;
        this.tf = new TermFactory();
        this.tb = new TermBuilder(tf, this);
        this.proof = services.proof;
        this.profile = services.profile;
        this.counters = services.counters;
        this.caches = services.caches;
        // this.specRepos = services.specRepos;
        this.solidityInfo = services.solidityInfo;
        this.model = services.model;
        nameRecorder = services.nameRecorder;
    }

    public Services(Profile profile) {
        this();
        assert profile != null;
        this.profile = profile;
    }

    public @NonNull NamespaceSet getNamespaces() {
        return namespaces;
    }

    public SolidityInfo getSolidityInfo() {
        return solidityInfo;
    }

    public void setSolidityModel(SolidityModel model) {
        this.model = model;
    }

    @Override
    public ServiceCaches getCaches() {
        return caches;
    }

    public TermFactory getTermFactory() {
        return tf;
    }

    public TermBuilder getTermBuilder() {
        return tb;
    }

    public TheoryInfo getTheoryInfo() {
        return theoryInfo;
    }

    public void setTheoryInfo(TheoryInfo theoryInfo) {
        this.theoryInfo = theoryInfo;
    }

    public SolidityModel getSolidityModel() {
        return model;
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
        return variableNamer;
    }

    public void addNameProposal(Name name) {
        nameRecorder.addProposal(name);
    }

    public void setProof(Proof proof) {
        this.proof = proof;
    }

    public void setNamespaces(NamespaceSet ns) {
        this.namespaces = ns;
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

    public Proof getProof() {
        return proof;
    }

    public Services copy() {
        throw new RuntimeException("Not implemented yet");
    }

    public Services copyPreservesLDTInformation() {
        throw new RuntimeException("Not implemented yet");
    }

    public void initTheories() {
        throw new RuntimeException("Not implemented yet");
    }
}
