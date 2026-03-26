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
import org.key_project.solidity.logic.sort.ArraySort;
import org.key_project.solidity.logic.sort.DynamicArraySort;
import org.key_project.solidity.logic.sort.MappingSort;
import org.key_project.solidity.logic.sort.ParametricSortDecl;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.key_project.solidity.program.ast.abstractions.ArrayInterface;
import org.key_project.solidity.program.ast.abstractions.MappingInterface;
import org.key_project.solidity.program.ast.abstractions.Type;

import java.util.HashMap;

/// Container of namespaces for the logic's signature (sorts, function symbols, predicate symbols
/// etc.)
/// as well as calculus related namespaces like rules
public final class NamespaceSet {
    private Namespace<@NonNull Sort> sortNS = new Namespace<>();
    private Namespace<@NonNull ParametricSortDecl> parametricSortNS = new Namespace<>();
    private Namespace<@NonNull Function> funcNS = new Namespace<>();
    private Namespace<@NonNull ParametricFunctionDecl> parametricFuncNS = new Namespace<>();
    private Namespace<@NonNull ProgramVariable> progVarNS = new Namespace<>();
    private Namespace<@NonNull QuantifiableVariable> varNS = new Namespace<>();
    private Namespace<@NonNull RuleSet> ruleSetNS = new Namespace<>();
    private Namespace<@NonNull Choice> choicesNS = new Namespace<>();

    private HashMap<ArrayInterface, ArraySort> arraySorts = new HashMap<>();
    private HashMap<Type, DynamicArraySort> dynamicArraySorts = new HashMap<>();
    private HashMap<MappingInterface, MappingSort> mappingSorts = new HashMap<>();

    public NamespaceSet() {
    }

    public NamespaceSet(Namespace<@NonNull Sort> sortNS,
            Namespace<@NonNull ParametricSortDecl> parametricSortNS,
            Namespace<@NonNull Function> funcNS,
            Namespace<@NonNull ParametricFunctionDecl> parametricFuncNS,
            Namespace<@NonNull ProgramVariable> progVarNS,
            Namespace<@NonNull QuantifiableVariable> varNS,
            Namespace<@NonNull RuleSet> ruleSetNS,
            Namespace<@NonNull Choice> choiceNS) {
        this.sortNS = sortNS;
        this.progVarNS = progVarNS;
        this.funcNS = funcNS;
        this.varNS = varNS;
        this.parametricSortNS = parametricSortNS;
        this.parametricFuncNS = parametricFuncNS;
        this.ruleSetNS = ruleSetNS;
        this.choicesNS = choiceNS;
    }

    public void add(NamespaceSet otherNamespace) {
        final Namespace[] thisComponents = asArray();
        final Namespace[] otherComponents = otherNamespace.asArray();
        for (int i = 0; i < thisComponents.length; i++) {
            thisComponents[i].add(otherComponents[i]);
        }
    }

    private Namespace<?>[] asArray() {
        return new Namespace[] { sorts(), parametricSorts(),
            functions(), parametricFunctions(),
            programVariables(), variables(),
            ruleSets(), choices() };
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
        return new NamespaceSet(sorts().copy(), parametricSorts().copy(),
            functions().copy(), parametricFunctions().copy(),
            programVariables().copy(), variables().copy(),
            ruleSets().copy(), choices().copy());
    }

    @Override
    public String toString() {
        return "NamespaceSet{" +
            "Sorts: " + sortNS + "\n" +
            "ParametricSorts: " + parametricSortNS + "\n" +
            "Functions: " + funcNS + "\n" +
            "ParametricFunctions: " + parametricFuncNS + "\n" +
            "Programvariables: " + progVarNS + "\n" +
            "Variables: " + varNS + "\n" +
            "RuleSets: " + ruleSetNS + "\n" +
            "ChoicesNS: " + choicesNS + "\n" +
            '}';
    }

    public void setSorts(Namespace<@NonNull Sort> sorts) {
        this.sortNS = sorts;
    }

    public Namespace<@NonNull Sort> sorts() {
        return sortNS;
    }

    public ArraySort getArraySort(ArrayInterface type){
        if(!arraySorts.containsKey(type))
            arraySorts.put(type, new ArraySort(type));
        return arraySorts.get(type);
    }

    public DynamicArraySort getDynamicArraySort(Type type){
        if(!dynamicArraySorts.containsKey(type))
            dynamicArraySorts.put(type, new DynamicArraySort(type));
        return dynamicArraySorts.get(type);
    }

    public MappingSort getMappingSort(MappingInterface map){
        if(!mappingSorts.containsKey(map))
            mappingSorts.put(map, new MappingSort(map));
        return mappingSorts.get(map);
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


    /// looks up for the symbol in the namespaces sort, functions and programVariables
    ///
    /// @param name the Name to look up
    /// @return the element of the given name or null
    public @Nullable Named lookupLogicSymbol(Name name) {
        return lookup(name, logicAsArray());
    }

    /// returns all namespaces with symbols that may occur in a real sequent (this means all
    /// namespaces without variables, choices and ruleSets)
    private Namespace<?>[] logicAsArray() {
        return new Namespace[] { sorts(), functions(), programVariables() };
    }

    public void flushToParent() {
        for (Namespace<?> ns : asArray()) {
            ns.flushToParent();
        }
    }

    // TODO: Nullness
    @SuppressWarnings("argument.type.incompatible")
    public NamespaceSet getParent() {
        return new NamespaceSet(sortNS.parent(), parametricSorts().parent(),
            funcNS.parent(), parametricFunctions().parent(),
            progVarNS.parent(), varNS.parent(),
            ruleSetNS.copy(), choicesNS.parent());
    }

    public NamespaceSet copyWithParent() {
        return new NamespaceSet(new Namespace<>(sorts()), new Namespace<>(parametricSorts()),
            new Namespace<>(functions()), new Namespace<>(parametricFunctions()),
            new Namespace<>(programVariables()), new Namespace<>(variables()),
            new Namespace<>(ruleSets()), new Namespace<>(choices()));
    }
}
