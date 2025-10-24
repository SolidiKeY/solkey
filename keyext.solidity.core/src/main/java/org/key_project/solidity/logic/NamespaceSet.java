/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.logic;

import org.key_project.logic.Choice;
import org.key_project.logic.Name;
import org.key_project.logic.Named;
import org.key_project.logic.Namespace;
import org.key_project.logic.op.Function;
import org.key_project.logic.op.QuantifiableVariable;
import org.key_project.logic.sort.Sort;
import org.key_project.prover.rules.RuleSet;
import org.key_project.solidity.logic.op.ParametricFunctionDecl;
import org.key_project.solidity.logic.op.ProgramVariable;
import org.key_project.solidity.logic.sort.ParametricSortDecl;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/// Container of namespaces for the logic's signature (sorts, function symbols, predicate symbols
/// etc.)
/// as well as calculus related namespaces like rules
public class NamespaceSet {

    private Namespace<@NonNull Sort> sortNS = new Namespace<>();
    private Namespace<@NonNull QuantifiableVariable> varNS = new Namespace<>();
    private Namespace<@NonNull Function> funcNS = new Namespace<>();
    private Namespace<@NonNull ProgramVariable> progVarNS = new Namespace<>();
    private Namespace<@NonNull RuleSet> ruleSetNS = new Namespace<>();
    private Namespace<@NonNull ParametricSortDecl> parametricSortNS = new Namespace<>();
    private Namespace<@NonNull ParametricFunctionDecl> parametricFuncNS = new Namespace<>();
    private Namespace<@NonNull Choice> choicesNS = new Namespace<>();

    public NamespaceSet() {}

    public NamespaceSet(Namespace<@NonNull Sort> sortNS,
            Namespace<@NonNull ProgramVariable> progVarNS,
            Namespace<@NonNull Function> funcNS, Namespace<@NonNull QuantifiableVariable> varNS) {
        this.sortNS = sortNS;
        this.varNS = varNS;
        this.progVarNS = progVarNS;
        this.funcNS = funcNS;
    }

    public void add(NamespaceSet ns) {
        variables().add(ns.variables());
        sorts().add(ns.sorts());
        programVariables().add(ns.programVariables());
        functions().add(ns.functions());
    }

    private Namespace<?>[] asArray() {
        return new Namespace[] { sorts(), programVariables(), functions(), variables() };
    }

    /// looks up if the given name is found in one of the namespaces and returns the named object or
    /// null if no object with the same name has been found
    public @Nullable Named lookup(Name name) {
        final Namespace<?>[] spaces = asArray();
        return lookup(name, spaces);
    }

    /// @param name
    /// @param spaces
    /// @return the element with the given name if found in the given namespaces, otherwise
    /// <tt>null</tt>
    private @Nullable Named lookup(Name name, final Namespace<?>[] spaces) {
        for (Namespace<?> space : spaces) {
            final Named n = space.lookup(name);
            if (n != null) {
                return n;
            }
        }
        return null;
    }

    public NamespaceSet copy() {
        return new NamespaceSet(sorts().copy(), programVariables().copy(), functions().copy(),
            variables().copy());
    }

    @Override
    public String toString() {
        return "Sorts: " + sorts() + "\n" + "ProgramVariables: " + programVariables()
            + "\n" + "Functions: " + functions() + "\n" + "Variables: " + variables() + "\n";
    }

    public void setSorts(Namespace<@NonNull Sort> sorts) {
        this.sortNS = sorts;
    }

    public Namespace<@NonNull Sort> sorts() {
        return sortNS;
    }

    public void setVariables(Namespace<@NonNull QuantifiableVariable> varNS) {
        this.varNS = varNS;
    }

    public Namespace<@NonNull QuantifiableVariable> variables() {
        return varNS;
    }

    public void setFunctions(Namespace<@NonNull Function> funcNS) {
        this.funcNS = funcNS;
    }

    public Namespace<@NonNull Function> functions() {
        return funcNS;
    }

    public void setRuleSets(Namespace<@NonNull RuleSet> ruleSetNS) {
        this.ruleSetNS = ruleSetNS;
    }

    public Namespace<@NonNull RuleSet> ruleSets() {
        return ruleSetNS;
    }

    public void setParametricFunctions(
            Namespace<@NonNull ParametricFunctionDecl> parametricFuncNS) {
        this.parametricFuncNS = parametricFuncNS;
    }

    public Namespace<@NonNull ParametricFunctionDecl> parametricFunctions() {
        return parametricFuncNS;
    }

    public Namespace<@NonNull ParametricSortDecl> parametricSorts() {
        return parametricSortNS;
    }

    public void setParametricSorts(Namespace<ParametricSortDecl> parametricSortNS) {
        this.parametricSortNS = parametricSortNS;
    }

    public Namespace<@NonNull ProgramVariable> programVariables() {
        return progVarNS;
    }

    public void programVariables(Namespace<@NonNull ProgramVariable> progVarNS) {
        this.progVarNS = progVarNS;
    }

    public Namespace<@NonNull Choice> choices() {
        return choicesNS;
    }

    public void choices(Namespace<@NonNull Choice> choices) {
        this.choicesNS = choices;
    }

}
