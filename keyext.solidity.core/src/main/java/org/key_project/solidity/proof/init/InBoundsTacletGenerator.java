/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.proof.init;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.key_project.logic.ChoiceExpr;
import org.key_project.logic.Name;
import org.key_project.logic.Term;
import org.key_project.logic.op.Function;
import org.key_project.prover.rules.RuleSet;
import org.key_project.prover.rules.Taclet;
import org.key_project.solidity.common.Services;
import org.key_project.solidity.logic.TermBuilder;
import org.key_project.solidity.program.ast.abstractions.PrimitiveType;
import org.key_project.solidity.rule.sv.SchemaVariableFactory;
import org.key_project.solidity.rule.sv.TermSV;
import org.key_project.solidity.rule.taclets.SolRewriteTaclet;
import org.key_project.solidity.rule.taclets.builder.RewriteTacletBuilder;
import org.key_project.solidity.rule.taclets.builder.RewriteTacletGoalTemplate;
import org.key_project.solidity.theory.IntLDT;

/// Generates the choice-guarded expansion taclets for the per-type range predicates declared by
/// [IntLDT], mirroring Java KeY's `expandInInt` design: under `intRules:ignoreOverFlow` a
/// predicate like `inUint8(i)` rewrites to `true` (unbounded mathematical integers), under
/// `intRules:soliditySemantics` it rewrites to its literal bounds (`0 <= i & i <= 255`). The
/// program taclets in `solidityProgramRules.key` emit the predicates via the `#inBounds` term
/// transformers; these rules are built here instead of being spelled out 128 times in a `.key`
/// file.
public final class InBoundsTacletGenerator {

    private InBoundsTacletGenerator() {
    }

    public static void registerTaclets(InitConfig initConfig) {
        Services services = initConfig.getServices();
        IntLDT intLDT = services.getTheoryInfo().getIntLDT();
        RuleSet concrete = initConfig.ruleSetNS().lookup(new Name("concrete"));
        if (concrete == null) {
            throw new IllegalStateException(
                "ruleset 'concrete' not found; cannot register inBounds expansion taclets");
        }
        ChoiceExpr ignoreOverFlow = ChoiceExpr.variable("intRules", "ignoreOverFlow");
        ChoiceExpr soliditySemantics = ChoiceExpr.variable("intRules", "soliditySemantics");

        TermBuilder tb = services.getTermBuilder();
        List<Taclet> taclets = new ArrayList<>();
        Set<Function> generated = new HashSet<>();
        for (PrimitiveType primitiveType : PrimitiveType.all()) {
            Function predicate = intLDT.getInBounds(primitiveType);
            BigInteger min = primitiveType.minValue();
            BigInteger max = primitiveType.maxValue();
            if (predicate == null || min == null || max == null
                    || !generated.add(predicate)) {
                continue;
            }
            TermSV boundSV =
                SchemaVariableFactory.createTermSV(new Name("i"), intLDT.targetSort());
            Term i = tb.var(boundSV);
            Term find = tb.func(predicate, i);
            String predicateName = predicate.name().toString();
            String expandName = "expand" + Character.toUpperCase(predicateName.charAt(0))
                + predicateName.substring(1);
            Term bounds = tb.and(tb.leq(tb.zTerm(min.toString()), i),
                tb.leq(i, tb.zTerm(max.toString())));
            taclets.add(buildExpansion(services, concrete, expandName + "True", find,
                tb.tt(), ignoreOverFlow));
            taclets.add(buildExpansion(services, concrete, expandName, find,
                bounds, soliditySemantics));
        }
        initConfig.addTaclets(taclets);
    }

    private static Taclet buildExpansion(Services services, RuleSet ruleSet, String name,
            Term find, Term replacewith, ChoiceExpr choice) {
        RewriteTacletBuilder<SolRewriteTaclet> builder = new RewriteTacletBuilder<>();
        builder.setName(new Name(name));
        builder.setFind(find);
        builder.addTacletGoalTemplate(new RewriteTacletGoalTemplate(replacewith));
        builder.addRuleSet(ruleSet);
        builder.setChoices(choice);
        return builder.getRewriteTaclet(services);
    }
}
