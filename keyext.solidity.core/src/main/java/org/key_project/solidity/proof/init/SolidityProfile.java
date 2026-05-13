/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.proof.init;


import org.key_project.logic.Name;
import org.key_project.prover.engine.GoalChooserFactory;
import org.key_project.prover.rules.Rule;
import org.key_project.solidity.common.Profile;
import org.key_project.solidity.common.RuleCollection;
import org.key_project.solidity.proof.Goal;
import org.key_project.solidity.proof.Proof;
import org.key_project.solidity.proof.io.RuleSourceFactory;
import org.key_project.solidity.proof.mgt.AxiomJustification;
import org.key_project.solidity.proof.mgt.RuleJustification;
import org.key_project.solidity.prover.impl.DefaultGoalChooserFactory;
import org.key_project.solidity.prover.impl.DepthFirstGoalChooserFactory;
import org.key_project.solidity.rule.BuiltInRule;
import org.key_project.solidity.rule.SolTaclet;
import org.key_project.solidity.strategy.ModularSolidityDLStrategyFactory;
import org.key_project.solidity.strategy.StrategyFactory;
import org.key_project.util.collection.ImmutableList;
import org.key_project.util.collection.ImmutableSLList;
import org.key_project.util.collection.ImmutableSet;

import org.jspecify.annotations.NonNull;

public class SolidityProfile implements Profile {
    public static final String NAME = "Solidity Profile";

    private static SolidityProfile defaultInstance;

    public static final StrategyFactory DEFAULT = new ModularSolidityDLStrategyFactory();

    // maybe move these fields to abstract parent AbstractProfile
    private final RuleCollection standardRules;

    private GoalChooserFactory<@NonNull Proof, @NonNull Goal> prototype;

    protected SolidityProfile(String standardRuleFilename) {
        standardRules = new RuleCollection(
            RuleSourceFactory.fromDefaultLocation(standardRuleFilename), initBuiltInRules());
        this.prototype = new DepthFirstGoalChooserFactory();
    }

    public SolidityProfile() {
        this("standardSolidityRules.key");
    }

    public static SolidityProfile getDefaultInstance() {
        if (defaultInstance == null) {
            defaultInstance = new SolidityProfile();
        }
        return defaultInstance;
    }

    @Override
    public RuleCollection getStandardRules() {
        return standardRules;
    }

    @Override
    public String name() {
        return NAME;
    }

    protected ImmutableList<BuiltInRule> initBuiltInRules() {
        return ImmutableSLList.<BuiltInRule>nil();
    }

    @Override
    public RuleJustification getJustification(Rule r) {
        // if (r == UseOperationContractRule.INSTANCE)
        // return new ComplexRuleJustificationBySpec();
        if (r instanceof SolTaclet t)
            return t.getRuleJustification();
        else
            return AxiomJustification.INSTANCE;
    }

    @Override
    public StrategyFactory getDefaultStrategyFactory() {
        return DEFAULT;
    }

    protected ImmutableSet<StrategyFactory> getStrategyFactories() {
        return ImmutableSet.singleton(DEFAULT);
    }

    @Override
    public boolean supportsStrategyFactory(Name strategy) {
        return getStrategyFactory(strategy) != null;
    }

    @Override
    public StrategyFactory getStrategyFactory(Name n) {
        for (StrategyFactory sf : getStrategyFactories()) {
            if (sf.name().equals(n)) {
                return sf;
            }
        }
        return null;
    }

    /// returns the default builder for a goal chooser
    ///
    /// @return this implementation returns a new instance of [DefaultGoalChooserFactory]
    @Override
    public GoalChooserFactory<Proof, @NonNull Goal> getDefaultGoalChooserBuilder() {
        return new DefaultGoalChooserFactory();
    }

    /// returns a copy of the selected goal chooser builder
    @Override
    public GoalChooserFactory<@NonNull Proof, @NonNull Goal> getSelectedGoalChooserBuilder() {
        return prototype.copy();
    }
}
