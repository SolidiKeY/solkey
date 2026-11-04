/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.parser.varcond;

import org.key_project.logic.LogicServices;
import org.key_project.logic.Name;
import org.key_project.logic.SyntaxElement;
import org.key_project.logic.Term;
import org.key_project.logic.op.sv.SchemaVariable;
import org.key_project.prover.rules.VariableCondition;
import org.key_project.prover.rules.instantiation.MatchResultInfo;
import org.key_project.solidity.common.Services;
import org.key_project.solidity.logic.GenericArgument;
import org.key_project.solidity.logic.TermBuilder;
import org.key_project.solidity.logic.op.ParametricFunctionInstance;
import org.key_project.solidity.program.ast.SolidityProgramElement;
import org.key_project.solidity.program.ast.StaticTypes;
import org.key_project.solidity.program.ast.abstractions.ArrayType;
import org.key_project.solidity.program.ast.abstractions.Type;
import org.key_project.solidity.program.ast.expressions.Expression;
import org.key_project.solidity.rule.matching.inst.SVInstantiations;
import org.key_project.util.collection.ImmutableSLList;

import org.jspecify.annotations.Nullable;

public final class StaticLengthCondition implements VariableCondition {
    private final SchemaVariable array;
    private final SchemaVariable length;

    public StaticLengthCondition(SchemaVariable array, SchemaVariable length) {
        this.array = array;
        this.length = length;
    }

    @Override
    public @Nullable MatchResultInfo check(SchemaVariable var, SyntaxElement instCandidate,
            MatchResultInfo matchCond, LogicServices lServices) {
        if (var != array) {
            return matchCond;
        }
        if (!(instCandidate instanceof Expression expression)) {
            return null;
        }
        Services services = (Services) lServices;
        SVInstantiations inst = (SVInstantiations) matchCond.getInstantiations();
        if (inst.getInstantiation(length) != null) {
            return matchCond;
        }
        Type type = StaticTypes.unwrap(expression.getType());
        Term value = type instanceof ArrayType fixed
                ? services.getTermBuilder().zTerm(fixed.length())
                : cellRead(expression, services);
        return matchCond.setInstantiations(inst.add(length, value, services));
    }

    static Term cellRead(SolidityProgramElement array, Services services) {
        TermBuilder tb = services.getTermBuilder();
        var find = ParametricFunctionInstance.get(
            services.getNamespaces().parametricFunctions().lookup(new Name("find")),
            ImmutableSLList.<GenericArgument>nil().prepend(
                new GenericArgument(services.getTheoryInfo().getIntLDT().targetSort())),
            services);
        Term path = tb.func(services.getNamespaces().requireFunction("consr"),
            Services.convertToLogicElement(array, services),
            tb.func(services.getNamespaces().requireFunction("size")));
        return tb.func(find, tb.var(services.getNamespaces().programVariables().lookup("storage")),
            path);
    }

    @Override
    public String toString() {
        return "\\staticLength(" + array.name() + ", " + length.name() + ")";
    }
}
