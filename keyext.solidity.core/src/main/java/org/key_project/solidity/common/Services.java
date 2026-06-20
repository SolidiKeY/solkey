/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.common;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Objects;

import org.key_project.logic.LogicServices;
import org.key_project.logic.Name;
import org.key_project.logic.Term;
import org.key_project.logic.op.Function;
import org.key_project.prover.proof.ProofServices;
import org.key_project.solidity.common.naming.NameRecorder;
import org.key_project.solidity.common.naming.VariableNamer;
import org.key_project.solidity.logic.NamespaceSet;
import org.key_project.solidity.logic.TermBuilder;
import org.key_project.solidity.logic.TermFactory;
import org.key_project.solidity.logic.op.ProgramVariable;
import org.key_project.solidity.program.ast.SolidityInfo;
import org.key_project.solidity.program.ast.SolidityProgramElement;
import org.key_project.solidity.program.ast.abstractions.KeYSolidityType;
import org.key_project.solidity.program.ast.declarations.FieldDeclaration;
import org.key_project.solidity.program.ast.expressions.IndexExpression;
import org.key_project.solidity.program.ast.expressions.MemberExp;
import org.key_project.solidity.program.ast.expressions.literals.Literal;
import org.key_project.solidity.program.ast.references.FieldReference;
import org.key_project.solidity.proof.Counter;
import org.key_project.solidity.proof.Node;
import org.key_project.solidity.proof.Proof;
import org.key_project.solidity.proof.SolidityModel;
import org.key_project.solidity.proof.mgt.SpecificationRepository;
import org.key_project.solidity.theory.LDT;
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
        nameRecorder = new NameRecorder();
        solidityInfo = new SolidityInfo();
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
        if (pe instanceof FieldReference fieldRef) {
            // a contract field access resolves, lazily and by name, to the field's
            // registered Field-sorted constant — no logic operator is stored on the AST node
            return tb.func(fieldTerm(fieldRef.getFieldConstantName(), services));
        }
        if (pe instanceof MemberExp member) {
            Term basePath = convertToLogicElement(member.getLeftExp(), services);
            Term field = memberFieldTerm(member, services);
            return appendPathSegment(basePath, field, services);
        }
        if (pe instanceof IndexExpression index) {
            Term basePath = convertToLogicElement(index.getLeftExp(), services);
            Term indexTerm = convertToLogicElement(index.getIndexExp(), services);
            return appendPathSegment(basePath, indexFieldTerm(indexTerm, services), services);
        }
        if (pe instanceof Literal lit) {
            LDT ldt = services.getTheoryInfo().get(lit.getLDTName());
            if (ldt != null) {
                return Objects.requireNonNull(ldt.translateLiteral(lit, services));
            }
        }
        throw new IllegalArgumentException(
            "Cannot convert program element '" + pe + "' of type '"
                + pe.getClass().getSimpleName()
                + "' into a logic term. If this is a new AST node, add a case to "
                + "Services.convertToLogicElement. If it is an (uninstantiated) schema variable, "
                + "check that the taclet binds it and that its ProgramSVSort matches the program "
                + "position it should stand for.");
    }

    private static Function fieldTerm(Name constantName, Services services) {
        Function constant = services.getNamespaces().functions().lookup(constantName);
        if (constant == null) {
            throw new IllegalStateException(
                "no field constant registered under name " + constantName);
        }
        return constant;
    }

    private static Term memberFieldTerm(MemberExp member, Services services) {
        if (member.getRightExp() instanceof FieldDeclaration field) {
            return services.getTermBuilder().func(fieldTerm(field.name(), services));
        }
        throw new IllegalArgumentException(
            "Cannot convert member access '" + member + "' into a logic path: member '"
                + member.getRightExp() + "' is not a field declaration.");
    }

    private static Term indexFieldTerm(Term indexTerm, Services services) {
        Function at = services.getNamespaces().functions().lookup(new Name("at"));
        if (at == null) {
            throw new IllegalStateException("index field constructor 'at' is not available");
        }
        return services.getTermBuilder().func(at, indexTerm);
    }

    private static Term appendPathSegment(Term basePath, Term field, Services services) {
        var tb = services.getTermBuilder();
        Function cons = services.getNamespaces().functions().lookup(new Name("cons"));
        Function consr = services.getNamespaces().functions().lookup(new Name("consr"));
        Function nil = services.getNamespaces().functions().lookup(new Name("nil"));
        if (cons == null || consr == null || nil == null) {
            throw new IllegalStateException("list constructors are not available");
        }
        if ("List".equals(basePath.sort().name().toString())) {
            return tb.func(consr, basePath, field);
        }
        if ("Struct".equals(basePath.sort().name().toString())) {
            return tb.func(cons, field, tb.func(nil));
        }
        return tb.func(cons, basePath, tb.func(cons, field, tb.func(nil)));
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

    private void setTheoryInfo(TheoryInfo theoryInfo) {
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
        s.getSolidityInfo().initialize(s, new ArrayList<>());
        nameRecorder = nameRecorder.copy();
        return s;
    }

    /// creates a new service object with the same ldt information as the actual one
    public Services copyPreservesLDTInformation() {
        Services s = new Services(getProfile());
        s.setNamespaces(namespaces.copy());
        s.setTheoryInfo(theoryInfo);
        s.getSolidityInfo().initialize(s, new ArrayList<>());
        return s;
    }

    public void initTheories(ArrayList<KeYSolidityType> unresolvedTypes) {
        if (theoryInfo == null) {
            theoryInfo = new TheoryInfo(this);
            solidityInfo.initialize(this, unresolvedTypes);
        } else {
            throw new IllegalStateException("Tried to initialize theories twice");
        }
    }

}
