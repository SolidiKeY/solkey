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
/// `@custom:key invariant` clauses and whose condition and body do not touch memory, and binds
/// `inv` to the conjunction of the clauses. `\getVariant(cond, body, dec)` binds `dec` to its
/// `decreases` term and fails when there is none.
public class LoopInvariantCondition implements VariableCondition {
    public enum Kind {
        INVARIANT("\\getInvariant"), VARIANT("\\getVariant"), BREAK("\\getLoopBreak");

        private final String command;

        Kind(String command) {
            this.command = command;
        }
    }

    private final ProgramSV conditionSV;
    private final ProgramSV bodySV;
    private final SchemaVariable resultSV;
    private final Kind kind;

    public LoopInvariantCondition(ProgramSV conditionSV, ProgramSV bodySV,
            SchemaVariable resultSV, Kind kind) {
        this.conditionSV = conditionSV;
        this.bodySV = bodySV;
        this.resultSV = resultSV;
        this.kind = kind;
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
                || LoopFrame.of(loop).memory()) {
            return null;
        }
        Term result = switch (kind) {
            case VARIANT -> LoopSpecCompiler.variant(spec, services);
            case INVARIANT -> withLayoutInv(LoopSpecCompiler.invariant(spec, services),
                loop, services);
            case BREAK -> spec.breakFlag() == null ? services.getTermBuilder().ff()
                    : services.getTermBuilder().equals(
                        services.getTermBuilder().var(spec.breakFlag()),
                        services.getTermBuilder().func(
                            services.getNamespaces().requireFunction("TRUE")));
        };
        return result == null ? null
                : matchCond.setInstantiations(svInst.add(resultSV, result, services));
    }

    private static @Nullable Term withLayoutInv(@Nullable Term invariant,
            LoopStatement loop, Services services) {
        if (invariant == null || !LoopFrame.of(loop).storage()) {
            return invariant;
        }
        TermBuilder tb = services.getTermBuilder();
        NamespaceSet namespaces = services.getNamespaces();
        return tb.and(invariant, tb.func(namespaces.requireFunction("layoutInv"),
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
        return kind.command + "(" + conditionSV.name() + ", "
            + bodySV.name() + ", " + resultSV.name() + ")";
    }
}
