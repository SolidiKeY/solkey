/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.parser.varcond;

import org.key_project.logic.LogicServices;
import org.key_project.logic.SyntaxElement;
import org.key_project.logic.Term;
import org.key_project.logic.op.sv.SchemaVariable;
import org.key_project.prover.rules.VariableCondition;
import org.key_project.prover.rules.instantiation.MatchResultInfo;
import org.key_project.solidity.common.Services;
import org.key_project.solidity.logic.NamespaceSet;
import org.key_project.solidity.logic.TermBuilder;
import org.key_project.solidity.program.ast.statement.LoopStatement;
import org.key_project.solidity.rule.matching.inst.SVInstantiations;
import org.key_project.solidity.rule.metaconstruct.LoopFrame;
import org.key_project.solidity.rule.sv.ProgramSV;
import org.key_project.solidity.speclang.LoopSpec;
import org.key_project.solidity.speclang.natspec.LoopSpecCompiler;

import org.jspecify.annotations.Nullable;

/// `\getInvariant(cond, body, inv)` matches a `while (cond) body` whose loop carries
/// `@custom:key invariant` clauses and whose body does not touch memory, and binds `inv` to the
/// conjunction of the clauses. `\getVariant(cond, body, dec)` binds `dec` to its `decreases`
/// term and fails when there is none.
public class LoopInvariantCondition implements VariableCondition {
    private final ProgramSV conditionSV;
    private final ProgramSV bodySV;
    private final SchemaVariable resultSV;
    private final boolean variant;

    public LoopInvariantCondition(ProgramSV conditionSV, ProgramSV bodySV,
            SchemaVariable resultSV, boolean variant) {
        this.conditionSV = conditionSV;
        this.bodySV = bodySV;
        this.resultSV = resultSV;
        this.variant = variant;
    }

    @Override
    public @Nullable MatchResultInfo check(SchemaVariable var, SyntaxElement instCandidate,
            MatchResultInfo matchCond, LogicServices lServices) {
        final var services = (Services) lServices;
        final var svInst = (SVInstantiations) matchCond.getInstantiations();
        if (svInst.getInstantiation(resultSV) != null) {
            return matchCond;
        }
        Object condition = svInst.getInstantiation(conditionSV);
        Object body = svInst.getInstantiation(bodySV);
        if (condition == null || body == null || svInst.getContextInstantiation() == null) {
            return null;
        }
        LoopStatement loop =
            find(svInst.getContextInstantiation().contextProgram(), condition, body);
        LoopSpec spec = loop == null ? null : loop.getSpec();
        if (spec == null || spec.invariants().isEmpty()
                || LoopFrame.of(loop.getBody()).memory()) {
            return null;
        }
        Term result = variant ? LoopSpecCompiler.variant(spec, services)
                : withWellformedStorage(LoopSpecCompiler.invariant(spec, services), loop,
                    services);
        return result == null ? null
                : matchCond.setInstantiations(svInst.add(resultSV, result, services));
    }

    private static @Nullable Term withWellformedStorage(@Nullable Term invariant,
            LoopStatement loop, Services services) {
        if (invariant == null || !LoopFrame.of(loop.getBody()).storage()) {
            return invariant;
        }
        TermBuilder tb = services.getTermBuilder();
        NamespaceSet namespaces = services.getNamespaces();
        return tb.and(invariant, tb.func(namespaces.requireFunction("wellformed"),
            tb.var(namespaces.programVariables().lookup("storage"))));
    }

    private static @Nullable LoopStatement find(SyntaxElement element, Object condition,
            Object body) {
        if (element instanceof LoopStatement loop && loop.getCondition() == condition
                && loop.getBody() == body) {
            return loop;
        }
        for (int i = 0; i < element.getChildCount(); i++) {
            LoopStatement found = find(element.getChild(i), condition, body);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return (variant ? "\\getVariant(" : "\\getInvariant(") + conditionSV.name() + ", "
            + bodySV.name() + ", " + resultSV.name() + ")";
    }
}
