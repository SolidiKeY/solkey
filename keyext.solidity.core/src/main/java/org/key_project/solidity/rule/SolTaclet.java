/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.rule;

import org.key_project.logic.ChoiceExpr;
import org.key_project.logic.Name;
import org.key_project.logic.SyntaxElement;
import org.key_project.logic.op.QuantifiableVariable;
import org.key_project.logic.op.sv.SchemaVariable;
import org.key_project.prover.rules.*;
import org.key_project.prover.rules.tacletbuilder.TacletGoalTemplate;
import org.key_project.solidity.logic.visitor.BoundVarsVisitor;
import org.key_project.solidity.rule.matching.VMTacletMatcher;
import org.key_project.solidity.rule.taclets.TacletSchemaVariableCollector;
import org.key_project.util.collection.DefaultImmutableSet;
import org.key_project.util.collection.ImmutableList;
import org.key_project.util.collection.ImmutableMap;
import org.key_project.util.collection.ImmutableSet;

import org.checkerframework.checker.nullness.qual.EnsuresNonNull;
import org.jspecify.annotations.NonNull;

/// Taclets are the DL-extension of schematic theory specific rules. They are used to describe rules
/// of a logic (sequent) calculus. A typical taclet definition looks similar to
///
/// <code>
/// taclet_name { if ( ... ) find ( ... ) goal_descriptions }
/// </code>
///
/// where the if-part must and the find-part can contain a sequent arrow, that indicates, if a term
/// has to occur at the top level and if so, on which side of the sequent. The goal descriptions
/// consists of lists of add and replacewith constructs. They describe, how to construct a new goal
/// out of the old one by adding or replacing parts of the sequent. Each of these lists describe a
/// new goal, whereas if no such list exists, means that the goal is closed.
///
/// The find part of a taclet is used to attached the rule to a term in the sequent of the current
/// goal. Therefore the term of the sequent has to match the schema as found in the taclet's find
/// part. The taclet is then attached to this term, more precise not the taclet itself, but an
/// application object of this taclet (see [TacletApp][TacletApp]. When
/// this attached taclet application object is applied, the new goals are constructed as described
/// by
/// the goal descriptions. For example
///
/// <code>
/// find (A | B ==>) replacewith ( A ==> ); replacewith(B ==>)
/// </code>
///
/// creates two new goals, where the first has been built by replacing <code> A | B </code> with
/// <code>A</code> and the second one by replacing <code>A | B</code> with <code>B</code>. For a
/// complete description of the syntax and semantics of taclets consult the KeY-Manual. The objects
/// of this class serve different purposes: First they represent the syntactical structure of a
/// taclet, but they also include the taclet interpreter isself. The taclet interpreter knows two
/// modes: the match and the execution mode. The match mode tries to find a a mapping from
/// schemavariables to a given term or formula. In the execution mode, a given goal is manipulated
/// in
/// the manner as described by the goal descriptions.
///
///
/// But an object of this class neither copies or split the goal, nor it iterates through a sequent
/// looking where it can be applied, these tasks have to be done in advance. For example by one of
/// the following classes [RuleAppIndex][RuleAppIndex] or
/// [TacletAppIndex][TacletAppIndex] or
/// [TacletApp][TacletApp]
///
public abstract class SolTaclet extends Taclet implements Rule {

    /// Integer to cache the hashcode
    private int hashcode = 0;

    /* TODO: find better solution */
    private final boolean surviveSymbExec;

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
    protected SolTaclet(Name name, SyntaxElement find, TacletApplPart applPart,
            ImmutableList<TacletGoalTemplate> goalTemplates,
            ImmutableList<RuleSet> ruleSets,
            TacletAttributes attrs,
            ImmutableMap<@NonNull SchemaVariable, TacletPrefix> prefixMap, ChoiceExpr choices,
            boolean surviveSmbExec, ImmutableSet<TacletAnnotation> tacletAnnotations) {
        super(name, find, applPart, goalTemplates, ruleSets, attrs, prefixMap, choices,
            tacletAnnotations);
        this.surviveSymbExec = surviveSmbExec;
    }

    @EnsuresNonNull("matcher")
    @Override
    protected void createAndInitializeMatcher() {
        this.matcher = new VMTacletMatcher(this);
    }

    @EnsuresNonNull("executor")
    @Override
    protected abstract void createAndInitializeExecutor();

    @Override
    public ImmutableSet<QuantifiableVariable> getBoundVariables() {
        if (boundVariables == null) {
            ImmutableSet<QuantifiableVariable> result =
                DefaultImmutableSet.nil();

            for (final TacletGoalTemplate tgt : goalTemplates()) {
                result = result.union(tgt.getBoundVariables());
            }

            final BoundVarsVisitor bvv = new BoundVarsVisitor();
            bvv.visit(assumesSequent());
            result = result.union(bvv.getBoundVariables()).union(getBoundVariablesHelper());

            boundVariables = result;
        }

        return boundVariables;
    }


    @Override
    /// collects bound variables in taclet entities others than goal templates
    ///
    /// @return set of variables that occur bound in taclet entities others than goal templates
    protected abstract ImmutableSet<QuantifiableVariable> getBoundVariablesHelper();

    /// returns the set of schemavariables of the taclet's if-part
    ///
    /// @return Set of schemavariables of the if part
    protected ImmutableSet<SchemaVariable> getAssumesVariables() {
        // should be synchronized
        if (assumesVariables == null) {
            TacletSchemaVariableCollector svc = new TacletSchemaVariableCollector();
            svc.visitAssumes(assumesSequent());
            assumesVariables =
                DefaultImmutableSet.fromCollection(svc.getCollectedSchemaVariables());
        }

        return assumesVariables;
    }

    @Override
    public Taclet setName(String name) {
        return null;
    }

    public boolean getSurviveSymbExec() {
        return surviveSymbExec;
    }


    /// return true if <code>o</code> is a taclet of the same name and <code>o</code> and
    /// <code>this</code> contain no mutually exclusive taclet options.
    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }

        if (o == null || o.getClass() != this.getClass()) {
            return false;
        }

        final SolTaclet t2 = (SolTaclet) o;
        if (!name.equals(t2.name)) {
            return false;
        }

        if ((assumesSequent == null && t2.assumesSequent != null)
                || (assumesSequent != null && t2.assumesSequent == null)) {
            return false;
        } else if (assumesSequent != null && !assumesSequent.equals(t2.assumesSequent)) {
            return false;
        }

        if (!choices.equals(t2.choices)) {
            return false;
        }

        return goalTemplates.equals(t2.goalTemplates);
    }

    @Override
    public int hashCode() {
        if (hashcode == 0) {
            hashcode = 37 * name.hashCode() + 17;
            if (hashcode == 0) {
                hashcode = -1;
            }
        }
        return hashcode;
    }


}
