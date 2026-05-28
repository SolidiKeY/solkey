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

    private final TermFactory tf;
    private final TermBuilder tb;
    private final ServiceCaches caches;
    /// variable namer for inner renaming
    private final VariableNamer variableNamer = new VariableNamer(this);
    /// map of names to counters
    private final HashMap<String, Counter> counters;
    private final SolidityInfo solidityInfo;

    private SpecificationRepository specificationRepository;
    /**
     * proof specific namespaces (functions, predicates, sorts, variables)
     */
    private NamespaceSet namespaces = new NamespaceSet();
    private TheoryInfo theoryInfo;
    /// the Solidity model
    private SolidityModel model;
    /// records name proposals
    private NameRecorder nameRecorder;
    /// the proof to which this services belongs (might be null)
    private Proof proof;
    private Profile profile;

    public Services() {
        tf = new TermFactory();
        tb = new TermBuilder(tf, this);
        specificationRepository = new SpecificationRepository(this);
        counters = new LinkedHashMap<>();
        caches = new ServiceCaches();
        solidityInfo = new SolidityInfo();
        nameRecorder = new NameRecorder();
        theoryInfo = new TheoryInfo(this);
    }

    public Services(Services services) {
        this.namespaces = services.namespaces;
        this.theoryInfo = services.theoryInfo;
        this.tf = new TermFactory();
        this.tb = new TermBuilder(tf, this);
        this.proof = services.proof;
        this.profile = services.profile;
        this.counters = services.counters;
        this.caches = services.caches;
        this.specificationRepository = services.specificationRepository;
        this.solidityInfo = services.solidityInfo;
        this.model = services.model;
        this.nameRecorder = services.nameRecorder;
    }

    public Services(Profile profile) {
        this();
        assert profile != null;
        this.profile = profile;
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

    public @NonNull NamespaceSet getNamespaces() {
        return namespaces;
    }

    public void setNamespaces(NamespaceSet ns) {
        this.namespaces = ns;
    }

    public SolidityInfo getSolidityInfo() {
        return solidityInfo;
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

    public void setSolidityModel(SolidityModel model) {
        this.model = model;
    }

    public VariableNamer getVariableNamer() {
        return variableNamer;
    }

    public void addNameProposal(Name name) {
        nameRecorder.addProposal(name);
    }

    public Profile getProfile() {
        return profile;
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
        return specificationRepository;
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

    /// Reset all counters associated with this service.
    /// Only use this method if the proof is empty!
    public void resetCounters() {
        if (proof.root().childrenCount() > 0) {
            throw new IllegalStateException("tried to reset counters on non-empty proof");
        }
        counters.clear();
    }

    public NameRecorder getNameRecorder() {
        return nameRecorder;
    }

    public Proof getProof() {
        return proof;
    }

    public void setProof(Proof proof) {
        this.proof = proof;
    }

    public Services copy() {
        return copy(getProfile());
    }

    public Services copy(Profile profile) {
        var s = new Services(profile);
        s.specificationRepository = specificationRepository;
        s.setNamespaces(namespaces.copy());
        s.setTheoryInfo(theoryInfo.copy(s));
        s.setSolidityModel(getSolidityModel());
        nameRecorder = nameRecorder.copy();
        return s;
    }

    /// creates a new service object with the same ldt information as the actual one
    public Services copyPreservesLDTInformation() {
        Services s = new Services(getProfile());
        s.setNamespaces(namespaces.copy());
        s.setTheoryInfo(theoryInfo);
        return s;
    }

    public void initTheories() {
        theoryInfo = new TheoryInfo(this);
    }

}
