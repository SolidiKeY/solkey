/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.common;


import org.jspecify.annotations.NonNull;
import org.key_project.logic.Name;
import org.key_project.prover.engine.GoalChooserFactory;
import org.key_project.prover.proof.ProofGoal;
import org.key_project.prover.proof.ProofObject;
import org.key_project.prover.rules.Rule;
import org.key_project.solidity.proof.mgt.RuleJustification;
import org.key_project.solidity.strategy.StrategyFactory;

public interface Profile {
    /// returns the rule source containg all taclets for this profile
    RuleCollection getStandardRules();

    /// the name of this profile
    String name();

    RuleJustification getJustification(Rule r);

    /// returns true if strategy <code>strategyName</code> may be used with this profile.
    ///
    /// @return supportedStrategies()->exists(s | s.name.equals(strategyName))
    boolean supportsStrategyFactory(Name strategyName);

    /// returns the StrategyFactory for strategy <code>strategyName</code>
    ///
    /// @param strategyName the Name of the strategy
    /// @return the StrategyFactory to build the demanded strategy
    StrategyFactory getStrategyFactory(Name strategyName);

    /// returns the strategy factory for the default strategy of this profile
    StrategyFactory getDefaultStrategyFactory();

    /// returns the default builder for a goal chooser
    <P extends @NonNull ProofObject<G>, G extends @NonNull ProofGoal<@NonNull G>> GoalChooserFactory<P, G> getDefaultGoalChooserBuilder();

    /// returns a new builder instance for the selected goal choooser
    <P extends @NonNull ProofObject<G>, G extends @NonNull ProofGoal<@NonNull G>> GoalChooserFactory<P, G> getSelectedGoalChooserBuilder();
}
