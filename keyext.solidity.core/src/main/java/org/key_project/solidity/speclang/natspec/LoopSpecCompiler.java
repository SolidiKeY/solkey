/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.speclang.natspec;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

import org.key_project.logic.Name;
import org.key_project.logic.Term;
import org.key_project.logic.op.Operator;
import org.key_project.solidity.common.Services;
import org.key_project.solidity.logic.NamespaceSet;
import org.key_project.solidity.logic.TermFactory;
import org.key_project.solidity.logic.op.ProgramVariable;
import org.key_project.solidity.parser.KeYIO;
import org.key_project.solidity.speclang.LoopSpec;
import org.key_project.util.collection.ImmutableArray;

import org.jspecify.annotations.Nullable;

public final class LoopSpecCompiler {
    private LoopSpecCompiler() {}

    public static Term invariant(LoopSpec spec, Services services) {
        String text = String.join(" && ",
            spec.invariants().stream().map(clause -> "(" + clause + ")").toList());
        return compile(spec, services,
            (compiler, context) -> compiler.formula(text, context, where(spec, "invariant")));
    }

    public static @Nullable Term variant(LoopSpec spec, Services services) {
        String text = spec.decreases();
        if (text == null) {
            return null;
        }
        return compile(spec, services,
            (compiler, context) -> compiler.term(text, context, where(spec, "decreases")));
    }

    private interface Translation {
        String apply(SpecCompiler compiler, SpecCompiler.Context context);
    }

    private static String where(LoopSpec spec, String clause) {
        return spec.contract().name() + "." + spec.function().name() + ", loop " + clause;
    }

    private static Term compile(LoopSpec spec, Services services, Translation translation) {
        Map<String, SpecType> locals = new HashMap<>();
        NamespaceSet namespaces = services.getNamespaces().copyWithParent();
        Map<Operator, ProgramVariable> placeholders = new IdentityHashMap<>();
        for (Map.Entry<String, LoopSpec.Binding> entry : spec.bindings().entrySet()) {
            SpecType type = scalar(entry.getValue().type());
            if (type == null) {
                continue;
            }
            String name = entry.getKey();
            ProgramVariable variable = entry.getValue().variable();
            locals.put(name, type);
            if (variable.name().toString().equals(name)) {
                namespaces.programVariables().add(variable);
            } else {
                ProgramVariable placeholder = new ProgramVariable(new Name(name),
                    variable.getKeYSolidityType(), variable.getDataLocation());
                namespaces.programVariables().add(placeholder);
                placeholders.put(placeholder, variable);
            }
        }
        String text = translation.apply(new SpecCompiler(spec.contract(), spec.function()),
            new SpecCompiler.Context("storage", "net", false, locals));
        Term term = new KeYIO(services, namespaces).parseExpression(text);
        return placeholders.isEmpty() ? term
                : replace(term, placeholders, services.getTermFactory());
    }

    private static @Nullable SpecType scalar(String type) {
        try {
            SpecType specType = SpecType.of(type);
            return specType instanceof SpecType.Int || specType instanceof SpecType.Bool
                    ? specType
                    : null;
        } catch (SpecException e) {
            return null;
        }
    }

    private static Term replace(Term term, Map<Operator, ProgramVariable> placeholders,
            TermFactory factory) {
        ProgramVariable variable = placeholders.get(term.op());
        if (variable != null) {
            return factory.createTerm(variable);
        }
        Term[] subs = new Term[term.arity()];
        boolean changed = false;
        for (int i = 0; i < subs.length; i++) {
            subs[i] = replace(term.sub(i), placeholders, factory);
            changed |= subs[i] != term.sub(i);
        }
        return changed ? factory.createTerm(term.op(), new ImmutableArray<>(subs),
            term.boundVars()) : term;
    }
}
