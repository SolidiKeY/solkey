/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.parser.builder;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.key_project.logic.Choice;
import org.key_project.logic.Name;
import org.key_project.logic.Named;
import org.key_project.logic.sort.Sort;
import org.key_project.prover.rules.RuleSet;
import org.key_project.solidity.common.Services;
import org.key_project.solidity.logic.NamespaceSet;
import org.key_project.solidity.logic.SolidityDLTheory;
import org.key_project.solidity.logic.op.ProgramVariable;
import org.key_project.solidity.logic.sort.GenericParameter;
import org.key_project.solidity.logic.sort.GenericSort;
import org.key_project.solidity.logic.sort.ParametricSortDecl;
import org.key_project.solidity.logic.sort.SortImpl;
import org.key_project.solidity.parser.KeYSolidityDLParser;
import org.key_project.solidity.parser.ParsingFacade;
import org.key_project.solidity.program.ast.abstractions.KeYSolidityType;
import org.key_project.util.collection.ImmutableList;
import org.key_project.util.collection.ImmutableSet;
import org.key_project.util.collection.Immutables;
import org.key_project.util.java.CollectionUtil;

import org.antlr.v4.runtime.Token;

public class DeclarationBuilder extends DefaultBuilder {

    private final Map<String, String> category2Default = new HashMap<>();

    public DeclarationBuilder(Services services, NamespaceSet nss) {
        super(services, nss);
    }

    @Override
    public Object visitDecls(KeYSolidityDLParser.DeclsContext ctx) {
        mapMapOf(ctx.option_decls(), ctx.options_choice(), ctx.ruleset_decls(),
            ctx.sort_decls(), ctx.datatype_decls(),
            ctx.prog_var_decls(), ctx.schema_var_decls());
        return null;
    }

    @Override
    public Object visitDatatype_decl(KeYSolidityDLParser.Datatype_declContext ctx) {
        // boolean freeAdt = ctx.FREE() != null;
        var name = ctx.name.getText();
        var doc = ctx.DOC_COMMENT() != null
                ? ctx.DOC_COMMENT().getText()
                : null;
        List<GenericParameter> typeParameters = accept(ctx.formal_sort_param_decls());
        if (typeParameters == null) {
            var s = new SortImpl(new Name(name), false);
            sorts().addSafely(s);
        } else {
            var doubled = CollectionUtil.findDuplicates(typeParameters);
            if (!doubled.isEmpty()) {
                semanticError(ctx.formal_sort_param_decls(),
                    "Type parameters must be unique within a declaration. Found duplicate: %s",
                    doubled.getFirst());
            }
            var s = new ParametricSortDecl(new Name(name), false,
                ImmutableList.fromList(typeParameters), doc);
            namespaces().parametricSorts().addSafely(s);
        }
        return null;
    }

    @Override
    public Object visitProg_var_decls(KeYSolidityDLParser.Prog_var_declsContext ctx) {
        for (int i = 0; i < ctx.simple_ident_comma_list().size(); i++) {
            List<String> varNames = accept(ctx.simple_ident_comma_list(i));
            final KeYSolidityType kst = accept(ctx.typemapping(i));
            assert varNames != null;
            for (String varName : varNames) {
                Name pvName = new Name(varName);
                Named name = lookup(pvName);
                if (name != null) {
                    // TODO question: throw warning?
                    if (!(name instanceof ProgramVariable pv)
                            || !pv.getKeYSolidityType().equals(kst)) {
                        programVariables().add(new ProgramVariable(pvName, kst, null));
                    }
                } else {
                    programVariables().add(new ProgramVariable(pvName, kst, null));
                }
            }
        }
        return null;
    }

    @Override
    public Object visitChoice(KeYSolidityDLParser.ChoiceContext ctx) {
        String cat = ctx.category.getText();
        for (KeYSolidityDLParser.OptionDeclContext optdecl : ctx.optionDecl()) {
            Token catctx = optdecl.IDENT;
            String name = cat + ":" + catctx.getText();
            Choice c = choices().lookup(new Name(name));
            if (c == null) {
                c = new Choice(catctx.getText(), cat);
                choices().add(c);
            }
            category2Default.putIfAbsent(cat, name);
        }
        category2Default.computeIfAbsent(cat, it -> {
            choices().add(new Choice("On", cat));
            choices().add(new Choice("Off", cat));
            return cat + ":On";
        });
        return null;
    }

    @Override
    public Object visitOption_decls(KeYSolidityDLParser.Option_declsContext ctx) {
        return mapOf(ctx.choice());
    }

    @Override
    public Object visitOptions_choice(KeYSolidityDLParser.Options_choiceContext ctx) {
        return null;
    }

    @Override
    public Object visitSort_decls(KeYSolidityDLParser.Sort_declsContext ctx) {
        for (KeYSolidityDLParser.One_sort_declContext c : ctx.one_sort_decl()) {
            c.accept(this);
        }
        return null;
    }

    @Override
    public Object visitOne_sort_decl(KeYSolidityDLParser.One_sort_declContext ctx) {
        List<Sort> sortOneOf = accept(ctx.sortOneOf);
        List<Sort> sortExt = accept(ctx.sortExt);
        boolean isGenericSort = ctx.GENERIC() != null;
        boolean isAbstractSort = ctx.ABSTRACT() != null;
        List<Sort> createdSorts = new LinkedList<>();
        var doc = ParsingFacade.getValueDocumentation(ctx.DOC_COMMENT());

        if (ctx.sortIds != null) {
            for (var idCtx : ctx.sortIds.simple_ident_dots()) {
                String sortId = accept(idCtx);
                Name sortName = new Name(sortId);

                ImmutableSet<Sort> ext = sortExt == null ? ImmutableSet.empty()
                        : Immutables.createSetFrom(sortExt);
                ImmutableSet<Sort> oneOf = sortOneOf == null ? ImmutableSet.empty()
                        : Immutables.createSetFrom(sortOneOf);

                Sort existingSort = sorts().lookup(sortName);
                if (existingSort == null) {
                    Sort s;
                    if (isGenericSort) {
                        s = new GenericSort(sortName, ext, oneOf);
                    } else if (new Name("any").equals(sortName)) {
                        s = SolidityDLTheory.ANY;
                    } else {
                        s = new SortImpl(sortName, isAbstractSort, ext);
                    }
                    sorts().add(s);
                    createdSorts.add(s);
                } else {
                    // weigl: agreement on KaKeY meeting: this should be ignored until we finally
                    // have
                    // local namespaces for generic sorts
                    // addWarning(ctx, "Sort declaration is ignored, due to collision.");
                    // LOGGER.info("Sort declaration of {} in {} is ignored due to collision
                    // (already "
                    // + "present in {}).", sortName, BuilderHelpers.getPosition(ctx),
                    // existingSort.getOrigin());
                }
            }
        } else {
            // parametric sort
            var declCtx = ctx.parametric_sort_decl();
            assert declCtx != null : "One of the two must be present";
            List<GenericParameter> typeParams =
                visitFormal_sort_param_decls(declCtx.formal_sort_param_decls());
            ImmutableList<GenericParameter> params = ImmutableList.fromList(typeParams);
            var doubled = CollectionUtil.findDuplicates(params);
            if (!doubled.isEmpty()) {
                semanticError(declCtx,
                    "Type parameters must be unique within a declaration. Found duplicate: %s",
                    doubled.getFirst());
            }
            String name = declCtx.simple_ident_dots().getText();
            Name sortName = new Name(name);
            var sortDecl = new ParametricSortDecl(sortName, isAbstractSort, params, doc);
            namespaces().parametricSorts().add(sortDecl);
        }
        return createdSorts;
    }

    @Override
    public List<Sort> visitExtends_sorts(KeYSolidityDLParser.Extends_sortsContext ctx) {
        return mapOf(ctx.sortId());
    }

    @Override
    public List<Sort> visitOneof_sorts(KeYSolidityDLParser.Oneof_sortsContext ctx) {
        return mapOf(ctx.sortId());
    }

    @Override
    public Object visitRuleset_decls(KeYSolidityDLParser.Ruleset_declsContext ctx) {
        for (String id : this.<String>mapOf(ctx.simple_ident())) {
            Name name = new Name(id);
            var h = new RuleSet(name);
            if (ruleSets().lookup(name) == null) {
                ruleSets().add(h);
            }
        }
        return null;
    }
}
