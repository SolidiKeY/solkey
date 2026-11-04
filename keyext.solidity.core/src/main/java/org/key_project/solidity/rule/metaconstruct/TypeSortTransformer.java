/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.rule.metaconstruct;

import java.util.ArrayList;
import java.util.List;

import org.key_project.logic.Name;
import org.key_project.logic.Term;
import org.key_project.logic.op.Function;
import org.key_project.logic.sort.Sort;
import org.key_project.solidity.common.Services;
import org.key_project.solidity.logic.GenericArgument;
import org.key_project.solidity.logic.TermBuilder;
import org.key_project.solidity.logic.op.ParametricFunctionDecl;
import org.key_project.solidity.logic.op.ParametricFunctionInstance;
import org.key_project.solidity.logic.op.ProgramVariable;
import org.key_project.solidity.logic.op.TypedField;
import org.key_project.solidity.logic.sort.ArraySort;
import org.key_project.solidity.logic.sort.TypeSorts;
import org.key_project.solidity.program.ast.StaticTypes;
import org.key_project.solidity.program.ast.abstractions.Type;
import org.key_project.solidity.rule.matching.inst.SVInstantiations;
import org.key_project.util.collection.ImmutableSLList;

import org.jspecify.annotations.Nullable;

public final class TypeSortTransformer extends AbstractTermTransformer {
    public enum Kind {
        TYPE_OF("#typeOf", 1), ELEM_OF("#elemOf", 1), STRUCT_CONFORMS("#structConforms", 2),
        TYPED_SELECT("#typedSelect", 1), LENGTH_AT("#lengthAt", 2), TYPE_AT("#typeAt", 2),
        LENGTH("#len", 1), TYPED_CHILD("#typedChild", 3);

        private final String name;
        private final int arity;

        Kind(String name, int arity) {
            this.name = name;
            this.arity = arity;
        }
    }

    private static final Name EMPTY_OF = new Name("emptyOf");
    private static final Name SELECT = new Name("selectSt");
    private static final Name FIND = new Name("find");
    private static final Name CAST = new Name("cast");
    private static final Name DEFAULT_VALUE = new Name("defaultValue");

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
            case STRUCT_CONFORMS -> structConforms(sortOf(subject), term.sub(1), services);
            case TYPED_SELECT -> typedSelect(subject, services);
            case LENGTH_AT -> services.getTermBuilder()
                    .zTerm(TypeSorts.lengthOf(sortAt(sortOf(subject), term.sub(1), services)));
            case TYPE_AT -> node(sortAt(sortOf(subject), term.sub(1), services), services);
            case TYPED_CHILD -> typedChild(subject, term.sub(1), term.sub(2), services);
            case LENGTH -> inferSort(subject, services) instanceof ArraySort array
                    ? services.getTermBuilder().zTerm(array.length())
                    : instance(SELECT, services.getTheoryInfo().getIntLDT().targetSort(), services,
                        subject, services.getTermBuilder()
                                .func(services.getNamespaces().requireFunction("size")));
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

    private static Term typedSelect(Term select, Services services) {
        Term view = select.sub(0);
        Term selector = select.sub(1);
        Sort viewSort = sortOf(view);
        Sort result = sortOf(select);
        Sort struct = TypeSorts.structSort(services);
        Term node = untyped(view, services);
        if (!result.extendsTrans(struct)) {
            if (viewSort instanceof ArraySort array && isFunction(selector, "size")) {
                return services.getTermBuilder().zTerm(array.length());
            }
            return instance(SELECT, result, services, node, selector);
        }
        Sort child = TypeSorts.childOf(viewSort, selector, services);
        Sort read = child != struct && TypeSorts.containsFixed(child, services) ? child : struct;
        return instance(SELECT, read, services, node, selector);
    }

    private static Term untyped(Term view, Services services) {
        Sort struct = TypeSorts.structSort(services);
        if (view.op() instanceof ParametricFunctionInstance instance) {
            Name base = instance.getBase().name();
            Term[] subs = subs(view);
            if (base.equals(FIND) || base.equals(SELECT)) {
                return instance(base, struct, services, subs);
            }
            if (base.equals(DEFAULT_VALUE)) {
                return services.getTermBuilder()
                        .func(services.getNamespaces().requireFunction("mtSt"));
            }
            if (base.equals(CAST)) {
                return subs[0].sort().extendsTrans(struct) ? subs[0]
                        : instance(CAST, struct, services, subs);
            }
        }
        return instance(CAST, struct, services, view);
    }

    private static Term typedChild(Term parent, Term selector, Term node, Services services) {
        Sort child = TypeSorts.childOf(inferSort(parent, services), selector, services);
        return child != TypeSorts.structSort(services) && TypeSorts.containsFixed(child, services)
                ? instance(CAST, child, services, node)
                : node;
    }

    private static Sort inferSort(Term term, Services services) {
        Sort struct = TypeSorts.structSort(services);
        if (term.op() instanceof ParametricFunctionInstance instance) {
            Sort sort = instance.getArgs().head().sort();
            Name base = instance.getBase().name();
            if (sort != struct) {
                return sort;
            }
            if (base.equals(SELECT)) {
                return TypeSorts.childOf(inferSort(term.sub(0), services), term.sub(1), services);
            }
            if (base.equals(FIND) && isConcrete(term.sub(1))) {
                return sortAt(inferSort(term.sub(0), services), term.sub(1), services);
            }
            if (base.equals(CAST)) {
                return inferSort(term.sub(0), services);
            }
            return struct;
        }
        if (term.op() instanceof Function f && !(term.op() instanceof TypedField)) {
            return switch (f.name().toString()) {
                case "delNode", "assignSt", "save", "storeSt", "delAt" -> inferSort(term.sub(0),
                    services);
                case "delNodeIndex" -> TypeSorts.childOf(inferSort(term.sub(0), services),
                    services.getTermBuilder().func(
                        services.getNamespaces().requireFunction("at"), term.sub(1)),
                    services);
                default -> struct;
            };
        }
        return struct;
    }

    private static boolean isConcrete(Term path) {
        if (isFunction(path, "nil")) {
            return true;
        }
        if (isFunction(path, "cons")) {
            return isConcrete(path.sub(1));
        }
        return isFunction(path, "consr") && isConcrete(path.sub(0));
    }

    private static Sort sortAt(Sort sort, Term path, Services services) {
        Sort current = sort;
        for (Term selector : selectors(path)) {
            current = TypeSorts.childOf(current, selector, services);
        }
        return current;
    }

    private static List<Term> selectors(Term path) {
        if (isFunction(path, "nil")) {
            return new ArrayList<>();
        }
        if (isFunction(path, "cons")) {
            List<Term> rest = selectors(path.sub(1));
            rest.addFirst(path.sub(0));
            return rest;
        }
        if (isFunction(path, "consr")) {
            List<Term> init = selectors(path.sub(0));
            init.add(path.sub(1));
            return init;
        }
        throw new IllegalArgumentException("Not a concrete path: " + path);
    }

    private static boolean isFunction(Term term, String name) {
        return term.op() instanceof Function f && !(term.op() instanceof TypedField)
                && name.equals(f.name().toString());
    }

    private static Term[] subs(Term term) {
        Term[] subs = new Term[term.arity()];
        for (int i = 0; i < subs.length; i++) {
            subs[i] = term.sub(i);
        }
        return subs;
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

    private static Term instance(Name name, @Nullable Sort sort, Services services,
            Term... subs) {
        ParametricFunctionDecl decl =
            services.getNamespaces().parametricFunctions().lookup(name);
        if (decl == null || sort == null) {
            throw new IllegalStateException("Parametric function " + name + " is not declared");
        }
        return services.getTermBuilder().func(ParametricFunctionInstance.get(decl,
            ImmutableSLList.<GenericArgument>nil().prepend(new GenericArgument(sort)), services),
            subs);
    }
}
