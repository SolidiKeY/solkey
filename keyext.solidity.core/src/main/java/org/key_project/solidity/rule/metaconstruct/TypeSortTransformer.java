/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.rule.metaconstruct;

import java.util.ArrayList;
import java.util.List;

import org.key_project.logic.Name;
import org.key_project.logic.Term;
import org.key_project.logic.sort.Sort;
import org.key_project.solidity.common.Services;
import org.key_project.solidity.logic.GenericArgument;
import org.key_project.solidity.logic.TermBuilder;
import org.key_project.solidity.logic.op.ParametricFunctionDecl;
import org.key_project.solidity.logic.op.ParametricFunctionInstance;
import org.key_project.solidity.logic.op.ProgramVariable;
import org.key_project.solidity.logic.op.TypedField;
import org.key_project.solidity.logic.sort.TypeSorts;
import org.key_project.solidity.program.ast.StaticTypes;
import org.key_project.solidity.program.ast.abstractions.Type;
import org.key_project.solidity.rule.matching.inst.SVInstantiations;
import org.key_project.util.collection.ImmutableSLList;

public final class TypeSortTransformer extends AbstractTermTransformer {
    public enum Kind {
        TYPE_OF("#typeOf", 1), ELEM_OF("#elemOf", 1), MEMBER_OF("#memberOf", 2),
        LENGTH_OF("#lengthOf", 1), STRUCT_CONFORMS("#structConforms", 2), NODE_OF("#nodeOf", 1);

        private final String name;
        private final int arity;

        Kind(String name, int arity) {
            this.name = name;
            this.arity = arity;
        }
    }

    private static final Name EMPTY_OF = new Name("emptyOf");
    private static final Name SELECT = new Name("selectSt");

    private final Kind kind;

    public TypeSortTransformer(Kind kind) {
        super(new Name(kind.name), kind.arity);
        this.kind = kind;
    }

    @Override
    public Term transform(Term term, SVInstantiations svInst, Services services) {
        Term subject = term.sub(0);
        return switch (kind) {
            case TYPE_OF -> node(TypeSorts.of(typeOf(subject), services), services);
            case ELEM_OF -> {
                Sort element = TypeSorts.elementOf(sortOf(subject), services);
                yield node(element == null ? TypeSorts.structSort(services) : element, services);
            }
            case MEMBER_OF -> term.sub(1).op() instanceof TypedField member
                    ? node(TypeSorts.memberOf(member, services), services)
                    : services.getTermBuilder().func(
                        services.getNamespaces().requireFunction("fieldType"), term.sub(1));
            case LENGTH_OF -> services.getTermBuilder()
                    .zTerm(TypeSorts.lengthOf(sortOf(subject)));
            case STRUCT_CONFORMS -> structConforms(sortOf(subject), term.sub(1), services);
            case NODE_OF -> node(sortOf(subject), services);
        };
    }

    private static Type typeOf(Term subject) {
        return switch (subject.op()) {
            case ProgramVariable pv -> StaticTypes.unwrap(pv.getType());
            case TypedField field -> field.type();
            default -> null;
        };
    }

    private static Sort sortOf(Term term) {
        return term.op() instanceof ParametricFunctionInstance instance
                ? instance.getArgs().head().sort()
                : term.sort();
    }

    private static Term node(Sort sort, Services services) {
        return TypeSorts.isTrivial(sort, services)
                ? services.getTermBuilder()
                        .func(services.getNamespaces().requireFunction("mtSt"))
                : instance(EMPTY_OF, sort, services);
    }

    private static Term structConforms(Sort struct, Term node, Services services) {
        TermBuilder tb = services.getTermBuilder();
        List<Term> parts = new ArrayList<>();
        for (TypedField member : TypeSorts.membersOf(struct, services)) {
            Sort sort = TypeSorts.memberOf(member, services);
            if (!TypeSorts.isTrivial(sort, services)) {
                parts.add(tb.func(services.getNamespaces().requireFunction("conforms"),
                    node(sort, services),
                    instance(SELECT, TypeSorts.structSort(services), services, node,
                        tb.func(member))));
            }
        }
        return tb.and(parts);
    }

    private static Term instance(Name name, Sort sort, Services services, Term... subs) {
        ParametricFunctionDecl decl =
            services.getNamespaces().parametricFunctions().lookup(name);
        if (decl == null) {
            throw new IllegalStateException("Parametric function " + name + " is not declared");
        }
        return services.getTermBuilder().func(ParametricFunctionInstance.get(decl,
            ImmutableSLList.<GenericArgument>nil().prepend(new GenericArgument(sort)), services),
            subs);
    }
}
