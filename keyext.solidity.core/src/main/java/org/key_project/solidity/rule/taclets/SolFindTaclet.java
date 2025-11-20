/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.rule.taclets;

import org.key_project.logic.ChoiceExpr;
import org.key_project.logic.Name;
import org.key_project.logic.SyntaxElement;
import org.key_project.logic.Term;
import org.key_project.logic.op.QuantifiableVariable;
import org.key_project.logic.op.sv.SchemaVariable;
import org.key_project.prover.rules.RuleSet;
import org.key_project.prover.rules.TacletAnnotation;
import org.key_project.prover.rules.TacletApplPart;
import org.key_project.prover.rules.TacletAttributes;
import org.key_project.prover.rules.TacletPrefix;
import org.key_project.prover.rules.tacletbuilder.TacletGoalTemplate;
import org.key_project.solidity.logic.visitor.BoundVarsVisitor;
import org.key_project.solidity.rule.SolTaclet;
import org.key_project.util.collection.DefaultImmutableSet;
import org.key_project.util.collection.ImmutableList;
import org.key_project.util.collection.ImmutableMap;
import org.key_project.util.collection.ImmutableSet;

import org.checkerframework.checker.nullness.qual.EnsuresNonNull;
import org.jspecify.annotations.NonNull;

public abstract class SolFindTaclet extends SolTaclet {

    /// Set of schema variables of the assumes sequent and the (optional) find expression/sequent
    private ImmutableSet<SchemaVariable> assumesAndFindSchemaVariables = null;

    /// creates a Taclet (originally known as Schematic Theory Specific Rules)
    ///
    /// @param name the name of the Taclet
    /// @param find the Term or Sequent that is the pattern that has to be found in a sequent and
    /// the places where it matches the Taclet can be applied
    /// @param applPart contains the application part of a Taclet that is the if-sequence, the
    /// variable conditions
    /// @param goalTemplates a list of goal descriptions.
    /// @param attrs attributes for the Taclet; these are boolean values indicating a noninteractive
    /// or recursive use of the Taclet.
    @EnsuresNonNull({ "matcher", "executor" })
    protected SolFindTaclet(Name name, SyntaxElement find, TacletApplPart applPart,
            ImmutableList<TacletGoalTemplate> goalTemplates, ImmutableList<RuleSet> ruleSets,
            TacletAttributes attrs,
            ImmutableMap<@NonNull SchemaVariable, TacletPrefix> prefixMap, ChoiceExpr choices,
            boolean surviveSymExec,
            ImmutableSet<TacletAnnotation> tacletAnnotations) {
        super(name, find, applPart, goalTemplates, ruleSets, attrs, prefixMap, choices,
            surviveSymExec,
            tacletAnnotations);
    }


    /// the term against which a concrete term occuring in the current goal is matched
    /// @return the top level [Term] in the find-part of the taclet
    public abstract Term find();

    /// @return set of schemavariables of the assumes and the (optional) find part
    public ImmutableSet<SchemaVariable> getAssumesAndFindVariables() {
        if (assumesAndFindSchemaVariables == null) {
            TacletSchemaVariableCollector svc = new TacletSchemaVariableCollector();

            svc.visitAssumes(this.assumesSequent);
            svc.visitFind(this.find());

            assumesAndFindSchemaVariables =
                DefaultImmutableSet.fromSet(svc.getCollectedSchemaVariables());
        }

        return assumesAndFindSchemaVariables;
    }

    /// returns the variables that occur bound in the find part
    protected ImmutableSet<QuantifiableVariable> getBoundVariablesHelper() {
        final BoundVarsVisitor bvv = new BoundVarsVisitor();
        bvv.visit(find());
        return bvv.getBoundVariables();
    }

    /// {@inheritDoc}
    @Override
    public boolean equals(Object o) {
        if (!super.equals(o)) {
            return false;
        }
        return find.equals(((SolFindTaclet) o).find);
    }

    /// {@inheritDoc}
    public int hashCode() {
        return 13 * super.hashCode() + find.hashCode();
    }

    /// appends a string representation of the find expression to the provided stringbuffer
    ///
    /// @param sb the StringBuffer where to append the find expression
    /// @return the same StringBuffer as the one given as argument
    protected StringBuffer toStringFind(StringBuffer sb) {
        return sb.append("\\find(").append(find().toString()).append(")\n");
    }

    /// returns a representation of the Taclet with find part as String
    ///
    /// @return string representation
    public @NonNull String toString() {
        if (tacletAsString == null) {
            StringBuffer sb = new StringBuffer();
            sb.append(name()).append(" {\n");
            sb = toStringAssumes(sb);
            sb = toStringFind(sb);
            sb = toStringVarCond(sb);
            sb = toStringGoalTemplates(sb);
            sb = toStringRuleSets(sb);
            sb = toStringAttribs(sb);
            sb = toStringTriggers(sb);
            tacletAsString = sb.append("}").toString();
        }
        return tacletAsString;
    }
}
