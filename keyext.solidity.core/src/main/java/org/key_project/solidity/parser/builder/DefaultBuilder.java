/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.parser.builder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

import org.key_project.logic.Choice;
import org.key_project.logic.Name;
import org.key_project.logic.Named;
import org.key_project.logic.Namespace;
import org.key_project.logic.op.Function;
import org.key_project.logic.op.Operator;
import org.key_project.logic.op.ParsableVariable;
import org.key_project.logic.op.QuantifiableVariable;
import org.key_project.logic.op.sv.SchemaVariable;
import org.key_project.logic.sort.Sort;
import org.key_project.prover.rules.RuleSet;
import org.key_project.solidity.common.Services;
import org.key_project.solidity.logic.GenericArgument;
import org.key_project.solidity.logic.NamespaceSet;
import org.key_project.solidity.logic.op.ParametricFunctionInstance;
import org.key_project.solidity.logic.op.ProgramVariable;
import org.key_project.solidity.logic.sort.*;
import org.key_project.solidity.parser.KeYSolidityDLParser;
import org.key_project.solidity.program.ast.abstractions.KeYSolidityType;
import org.key_project.solidity.rule.metaconstruct.AbstractTermTransformer;
import org.key_project.util.collection.ImmutableList;
import org.key_project.util.collection.ImmutableSLList;

import org.antlr.v4.runtime.ParserRuleContext;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.key_project.solidity.logic.SolidityDLTheory.FORMULA;

public class DefaultBuilder extends AbstractBuilder<@Nullable Object> {
    protected static final Logger LOGGER = LoggerFactory.getLogger(DefaultBuilder.class);

    protected final Services services;
    protected final NamespaceSet nss;
    private Namespace<@NonNull SchemaVariable> schemaVariablesNamespace = new Namespace<>();

    public DefaultBuilder(Services services, NamespaceSet nss) {
        this.services = services;
        this.nss = nss;
    }

    private ImmutableList<GenericArgument> getGenericArgs(
            KeYSolidityDLParser.Formal_sort_argsContext ctx,
            ImmutableList<GenericParameter> params) {
        if (ctx.sortId().size() != params.size()) {
            semanticError(ctx, "Expected %d sort arguments, got only %d",
                params.size(), ctx.sortId().size());
        }
        ImmutableList<GenericArgument> args = ImmutableSLList.nil();
        for (int i = params.size() - 1; i >= 0; i--) {
            var arg = ctx.sortId(i);
            var sort = visitSortId(arg);
            args = args.prepend(new GenericArgument(sort));
        }
        return args;
    }

    protected Namespace<@NonNull ProgramVariable> programVariables() {
        return namespaces().programVariables();
    }

    @Override
    public List<String> visitPvset(KeYSolidityDLParser.PvsetContext ctx) {
        return mapOf(ctx.varId());
    }

    @Override
    public List<RuleSet> visitRulesets(KeYSolidityDLParser.RulesetsContext ctx) {
        return mapOf(ctx.ruleset());
    }

    @Override
    public RuleSet visitRuleset(KeYSolidityDLParser.RulesetContext ctx) {
        String id = ctx.IDENT().getText();
        Name name = new Name(id);
        RuleSet h = ruleSets().lookup(name);
        if (h == null) {
            semanticError(ctx, String.format("Rule set %s was not defined.", name));
        }
        return h;
    }

    protected Named lookup(Name n) {
        final Namespace<?>[] lookups =
            { programVariables(),
                variables(), functions() };
        return doLookup(n, lookups);
    }

    protected <T> T doLookup(Name n, Namespace<?>... lookups) {
        for (Namespace<?> lookup : lookups) {
            Object l;
            if (lookup != null && (l = lookup.lookup(n)) != null) {
                try {
                    return (T) l;
                } catch (ClassCastException e) {
                }
            }
        }
        return null;
    }

    public NamespaceSet namespaces() {
        return nss;
    }

    protected Namespace<@NonNull QuantifiableVariable> variables() {
        return namespaces().variables();
    }

    protected Namespace<@NonNull Sort> sorts() {
        return namespaces().sorts();
    }

    protected Namespace<@NonNull Function> functions() {
        return namespaces().functions();
    }

    protected Namespace<@NonNull RuleSet> ruleSets() {
        return namespaces().ruleSets();
    }

    protected Namespace<@NonNull Choice> choices() {
        return namespaces().choices();
    }

    protected <T> T withSortAndConsts(Namespace<@NonNull Sort> sorts,
            Namespace<@NonNull Function> consts, Supplier<T> fn) {
        var oldSorts = nss.sorts();
        var oldFns = nss.functions();
        nss.setSorts(sorts);
        nss.setFunctions(consts);
        var res = fn.get();
        nss.setSorts(oldSorts);
        nss.setFunctions(oldFns);
        return res;
    }

    public String visitSimple_ident_dots(KeYSolidityDLParser.Simple_ident_dotsContext ctx) {
        return ctx.getText();
    }

    public List<Sort> visitArg_sorts_or_formula(
            KeYSolidityDLParser.Arg_sorts_or_formulaContext ctx) {
        return mapOf(ctx.arg_sorts_or_formula_helper());
    }

    public Sort visitArg_sorts_or_formula_helper(
            KeYSolidityDLParser.Arg_sorts_or_formula_helperContext ctx) {
        if (ctx.FORMULA() != null) {
            return FORMULA;
        } else {
            return accept(ctx.sortId());
        }
    }

    protected void unbindVars(Namespace<@NonNull QuantifiableVariable> orig) {
        namespaces().setVariables(orig);
    }

    /// looks up and returns the sort of the given name or null if none has been found
    protected Sort lookupSort(String name) {
        return sorts().lookup(new Name(name));
    }


    /// looks up a function, (program) variable or static query of the given name varfunc_id and the
    /// argument terms args in the namespaces and Solidity info.
    ///
    /// @param varfuncName the String with the symbols name
    /// @param genericArgsCtxt
    protected Operator lookupVarfuncId(ParserRuleContext ctx, String varfuncName,
            KeYSolidityDLParser.Formal_sort_argsContext genericArgsCtxt) {
        Name name = new Name(varfuncName);
        Operator[] operators =
            { schemaVariables().lookup(name), variables().lookup(name),
                programVariables().lookup(new Name(varfuncName)),
                functions().lookup(name),
                AbstractTermTransformer.name2metaop(varfuncName),
            };

        for (Operator op : operators) {
            if (op != null) {
                return op;
            }
        }

        if (genericArgsCtxt != null) {
            var d = nss.parametricFunctions().lookup(name);
            if (d == null) {
                semanticError(ctx, "Could not find parametric function: %s", name);
                return null;
            }
            var args = getGenericArgs(genericArgsCtxt, d.getParameters());
            return ParametricFunctionInstance.get(d, args, services);
        }
        semanticError(ctx, "Could not find (program) variable or constant %s", varfuncName);
        return null;
    }

    public String visitString_value(KeYSolidityDLParser.String_valueContext ctx) {
        return ctx.getText().substring(1, ctx.getText().length() - 1);
    }

    public Services getServices() {
        return services;
    }

    public Namespace<SchemaVariable> schemaVariables() {
        return schemaVariablesNamespace;
    }

    public void setSchemaVariables(Namespace<SchemaVariable> ns) {
        this.schemaVariablesNamespace = ns;
    }

    @Override
    public Object visitVarIds(KeYSolidityDLParser.VarIdsContext ctx) {
        Collection<String> ids = accept(ctx.simple_ident_comma_list());
        List<ParsableVariable> list = new ArrayList<>(ids.size());
        for (String id : ids) {
            ParsableVariable v = (ParsableVariable) lookup(new Name(id));
            if (v == null) {
                semanticError(ctx, "Variable " + id + " not declared.");
            }
            list.add(v);
        }
        return list;
    }

    @Override
    public Object visitSimple_ident_dots_comma_list(
            KeYSolidityDLParser.Simple_ident_dots_comma_listContext ctx) {
        return mapOf(ctx.simple_ident_dots());
    }

    @Override
    public String visitSimple_ident(KeYSolidityDLParser.Simple_identContext ctx) {
        return ctx.IDENT().getText();
    }

    @Override
    public List<String> visitSimple_ident_comma_list(
            KeYSolidityDLParser.Simple_ident_comma_listContext ctx) {
        return mapOf(ctx.simple_ident());
    }

    @Override
    public List<Boolean> visitWhere_to_bind(KeYSolidityDLParser.Where_to_bindContext ctx) {
        List<Boolean> list = new ArrayList<>(ctx.children.size());
        ctx.b.forEach(it -> list.add(it.getText().equalsIgnoreCase("true")));
        return list;
    }

    @Override
    public List<Sort> visitArg_sorts(KeYSolidityDLParser.Arg_sortsContext ctx) {
        return mapOf(ctx.sortId());
    }

    @Override
    public Sort visitSortId(KeYSolidityDLParser.SortIdContext ctx) {
        String name = ctx.id.getText();
        if (ctx.formal_sort_args() != null) {
            // parametric sorts should be instantiated
            ParametricSortDecl sortDecl = nss.parametricSorts().lookup(name);
            if (sortDecl == null) {
                semanticError(ctx, "Could not find polymorphic sort: %s", name);
            }
            ImmutableList<GenericArgument> params =
                getGenericArgs(ctx.formal_sort_args(), sortDecl.getParameters());
            return ParametricSortInstance.get(sortDecl, params, services);
        } else {
            Sort s = lookupSort(name);
            if (s == null) {
                semanticError(ctx, "Could not find sort: %s", ctx.getText());
            }
            return s;
        }
    }

    @Override
    public KeYSolidityType visitTypemapping(KeYSolidityDLParser.TypemappingContext ctx) {
        String type = visitSimple_ident_dots(ctx.simple_ident_dots());
        KeYSolidityType kst = services.getSolidityInfo().getKeYSolidityType(type);
        if (kst == null) {
            Sort sort = lookupSort(type);
            if (sort != null) {
                kst = new KeYSolidityType(null, sort);
            }
        }

        if (kst == null) {
            semanticError(ctx, "Unknown type: " + type);
        }

        return kst;
    }


    public Object visitFuncpred_name(KeYSolidityDLParser.Funcpred_nameContext ctx) {
        return ctx.getText();
    }

    @Override
    public @Nullable List<GenericParameter> visitFormal_sort_param_decls(
            KeYSolidityDLParser.Formal_sort_param_declsContext ctx) {
        return mapOf(ctx.formal_sort_param_decl());
    }

    @Override
    public GenericParameter visitFormal_sort_param_decl(
            KeYSolidityDLParser.Formal_sort_param_declContext ctx) {
        GenericParameter.Variance variance;
        if (ctx.PLUS() != null) {
            variance = GenericParameter.Variance.COVARIANT;
        } else if (ctx.MINUS() != null) {
            variance = GenericParameter.Variance.CONTRAVARIANT;
        } else {
            variance = GenericParameter.Variance.INVARIANT;
        }

        var name = ctx.simple_ident().getText();
        Sort paramSort = sorts().lookup(name);
        if (paramSort == null) {
            semanticError(ctx, "Parameter sort %s not found", name);
        }
        if (!(paramSort instanceof GenericSort)) {
            semanticError(ctx, "Parameter sort %s is not a generic sort", name);
        }
        return new GenericParameter((GenericSort) paramSort, variance);
    }
}
