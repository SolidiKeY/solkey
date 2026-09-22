/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.rule.taclets;

import org.key_project.logic.ChoiceExpr;
import org.key_project.logic.Name;
import org.key_project.logic.Term;
import org.key_project.logic.op.sv.SchemaVariable;
import org.key_project.prover.rules.RuleSet;
import org.key_project.prover.rules.TacletAnnotation;
import org.key_project.prover.rules.TacletApplPart;
import org.key_project.prover.rules.TacletAttributes;
import org.key_project.prover.rules.TacletPrefix;
import org.key_project.prover.rules.tacletbuilder.TacletGoalTemplate;
import org.key_project.prover.sequent.Sequent;
import org.key_project.solidity.rule.execution.SuccTacletExecutor;
import org.key_project.util.collection.ImmutableList;
import org.key_project.util.collection.ImmutableMap;
import org.key_project.util.collection.ImmutableSet;

import org.jspecify.annotations.NonNull;

/// A SuccTaclet represents a taclet whose find part has to match a top level formula in the
/// succedent of the sequent.
public class SolSuccTaclet extends SolFindTaclet {
    /// creates a [Taclet] (old name Schematic Theory Specific Rule) with the given parameters
    /// that works on the succedent.
    ///
    /// @param name the name of the [Taclet]
    /// @param find the find sequent of the Taclet
    /// @param applPart contains the application part of a taclet that is the if-sequent, the
    /// variable conditions
    /// @param goalTemplates a list of goal descriptions.
    /// @param ruleSets a list of rule sets for the Taclet
    /// @param attrs attributes for the Taclet; these are boolean values indicating a
    /// non-interactive or recursive use of the Taclet.
    /// @param prefixMap an ImmutableMap from [SchemaVariable] to [TacletPrefix] that
    /// contains the prefix for each SchemaVariable in the taclet
    public SolSuccTaclet(Name name, Sequent find, TacletApplPart applPart,
            ImmutableList<TacletGoalTemplate> goalTemplates, ImmutableList<RuleSet> ruleSets,
            TacletAttributes attrs,
            ImmutableMap<@NonNull SchemaVariable, TacletPrefix> prefixMap,
            ChoiceExpr choices,
            boolean surviveSymbExec, ImmutableSet<TacletAnnotation> tacletAnnotations,
            ImmutableList<@NonNull SchemaVariable> noFreeVarIns) {
        super(name, find, applPart, goalTemplates, ruleSets, attrs, prefixMap, choices,
            surviveSymbExec, tacletAnnotations, noFreeVarIns);
    }

    @Override
    public Term find() {
        return ((Sequent) find).succedent().getFirst().formula();
    }

    @Override
    protected void createAndInitializeExecutor() {
        executor = new SuccTacletExecutor(this);
    }

    /// toString for the find part
    protected StringBuffer toStringFind(StringBuffer sb) {
        return sb.append("\\find(==>").append(find().toString()).append(")\n");
    }

    @Override
    public @NonNull SolSuccTaclet setName(@NonNull String s) {
        return new SolSuccTaclet(new Name(s), (Sequent) find, copyApplPart(), goalTemplates(),
            getRuleSets(), copyAttrs(),
            prefixMap, choices, getSurviveSymbExec(), tacletAnnotations, noFreeVarIns);
    }

}
