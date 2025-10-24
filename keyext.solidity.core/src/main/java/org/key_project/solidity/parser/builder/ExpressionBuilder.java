/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.parser.builder;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

import org.key_project.logic.Name;
import org.key_project.logic.Namespace;
import org.key_project.logic.Term;
import org.key_project.logic.TermCreationException;
import org.key_project.logic.op.Function;
import org.key_project.logic.op.Operator;
import org.key_project.logic.op.ParsableVariable;
import org.key_project.logic.op.QuantifiableVariable;
import org.key_project.logic.op.sv.OperatorSV;
import org.key_project.logic.sort.Sort;
import org.key_project.solidity.common.Services;
import org.key_project.solidity.logic.NamespaceSet;
import org.key_project.solidity.logic.SolidityBlock;
import org.key_project.solidity.logic.TermFactory;
import org.key_project.solidity.logic.op.BoundVariable;
import org.key_project.solidity.logic.op.Equality;
import org.key_project.solidity.logic.op.Junctor;
import org.key_project.solidity.logic.op.LogicVariable;
import org.key_project.solidity.logic.op.ProgramVariable;
import org.key_project.solidity.logic.op.UpdateJunctor;
import org.key_project.solidity.parser.KeYSolidityDLParser;
import org.key_project.solidity.program.SchemaSolidityReader;
import org.key_project.solidity.program.SolidityReader;
import org.key_project.solidity.util.parsing.BuildingException;
import org.key_project.util.collection.ImmutableArray;
import org.key_project.util.java.StringUtil;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class ExpressionBuilder extends DefaultBuilder {

    private boolean soliditySchemaModeAllowed;

    public record BoundVar(Name name, Sort sort) {
    }

    private List<BoundVariable> boundVars = new ArrayList<>();


    public ExpressionBuilder(Services services, NamespaceSet nss) {
        super(services, nss);
    }

    /**
     * Given a raw modality string, this function trims the modality information.
     *
     * @param raw non-null string
     * @return non-null string
     */
    public static String trimSolidityBlock(String raw) {
        if (raw.startsWith("\\<")) {
            return StringUtil.trim(raw, "\\<>");
        }
        if (raw.startsWith("\\[")) {
            return StringUtil.trim(raw, "\\[]");
        }
        int end = raw.length() - (raw.endsWith("\\endmodality") ? "\\endmodality".length() : 0);
        int start = 0;
        if (raw.startsWith("\\diamond")) {
            start = "\\diamond".length();
        } else if (raw.startsWith("\\box")) {
            start = "\\box".length();
        } else if (raw.startsWith("\\modality")) {
            start = raw.indexOf('}') + 1;
        }
        return raw.substring(start, end);
    }

    /**
     * Given a raw modality string, this method determines the operator name.
     */
    public static String operatorOfSolidityBlock(String raw) {
        if (raw.startsWith("\\<")) {
            return "diamond";
        }
        if (raw.startsWith("\\[")) {
            return "box";
        }
        if (raw.startsWith("\\diamond")) {
            return "diamond";
        }
        if (raw.startsWith("\\box")) {
            return "box";
        }
        if (raw.startsWith("\\modality")) {
            int start = raw.indexOf('{') + 1;
            int end = raw.indexOf('}');
            return raw.substring(start, end);
        }
        return "n/a";
    }

    protected void enableSchemaMode() {
        soliditySchemaModeAllowed = true;
    }

    protected void disableSchemaMode() {
        soliditySchemaModeAllowed = false;
    }

    private static class PairOfStringAndSolidityBlock {
        String opName;
        SolidityBlock solidityBlock;
    }

    private PairOfStringAndSolidityBlock getJavaBlock(Token t) {
        PairOfStringAndSolidityBlock sjb = new PairOfStringAndSolidityBlock();
        String s = t.getText().trim();
        String cleanSolidity = trimSolidityBlock(s);
        sjb.opName = operatorOfSolidityBlock(s);

        try {
            try {
                if (soliditySchemaModeAllowed) {// TEST
                    final SchemaSolidityReader schemaSolidityReader =
                        new SchemaSolidityReader(services, nss);
                    schemaSolidityReader.setSVNamespace(schemaVariables());
                    try {
                        sjb.solidityBlock =
                            schemaSolidityReader.readBlockWithProgramVariables(programVariables(),
                                cleanSolidity);
                    } catch (Exception e) {
                        sjb.solidityBlock =
                            schemaSolidityReader.readBlockWithEmptyContext(cleanSolidity);
                    }
                }
            } catch (Exception e) {
                if (cleanSolidity.startsWith("{..")) {// do not fallback
                    throw e;
                }
            }

            if (sjb.solidityBlock == null) {
                SolidityReader solidityReader = new SolidityReader(services, nss);
                try {
                    sjb.solidityBlock = solidityReader
                            .readBlockWithProgramVariables(programVariables(), cleanSolidity);
                } catch (Exception e1) {
                    sjb.solidityBlock = solidityReader.readBlockWithEmptyContext(cleanSolidity);
                }
            }
        } catch (Exception e) {
            throw new BuildingException(t, "Could not parse java: '" + cleanSolidity + "'", e);
        }
        return sjb;
    }

    protected Term capsulateTf(ParserRuleContext ctx, Supplier<Term> termSupplier) {
        try {
            return termSupplier.get();
        } catch (TermCreationException e) {
            throw new BuildingException(ctx,
                String.format("Could not build term on: %s", ctx.getText()), e);
        }
    }

    @Override
    protected Operator lookupVarfuncId(ParserRuleContext ctx, String varfuncName,
            KeYSolidityDLParser.Formal_sort_argsContext genericArgsCtxt) {
        // Might be quantified variable
        var idx = -1;
        for (int i = 0; i < boundVars.size(); ++i) {
            if (varfuncName.equals(boundVars.get(i).name().toString())) {
                idx = i;
                break;
            }
        }
        if (idx != -1) {
            var deBruijn = boundVars.size() - idx;
            return new LogicVariable(deBruijn, boundVars.get(idx).sort());
        }

        return super.lookupVarfuncId(ctx, varfuncName, genericArgsCtxt);
    }


    public TermFactory getTermFactory() {
        return getServices().getTermFactory();
    }

    @Override
    public @Nullable Object visitTermorseq(KeYSolidityDLParser.TermorseqContext ctx) {
        return super.visitTermorseq(ctx);
    }

    @Override
    public @Nullable Object visitSemisequent(KeYSolidityDLParser.SemisequentContext ctx) {
        return super.visitSemisequent(ctx);
    }

    @Override
    public @Nullable Object visitTerm(KeYSolidityDLParser.TermContext ctx) {
        return super.visitTerm(ctx);
    }

    @Override
    public @Nullable Object visitTerm60(KeYSolidityDLParser.Term60Context ctx) {
        return super.visitTerm60(ctx);
    }

    @Override
    public @Nullable Object visitNegation_term(KeYSolidityDLParser.Negation_termContext ctx) {
        return super.visitNegation_term(ctx);
    }

    @Override
    public @Nullable Object visitQuantifierterm(KeYSolidityDLParser.QuantifiertermContext ctx) {
        return super.visitQuantifierterm(ctx);
    }

    @Override
    public @Nullable Object visitModality_term(KeYSolidityDLParser.Modality_termContext ctx) {
        return super.visitModality_term(ctx);
    }

    @Override
    public @Nullable Object visitUpdate_term(KeYSolidityDLParser.Update_termContext ctx) {
        return super.visitUpdate_term(ctx);
    }

    @Override
    public @Nullable Object visitAtom_prefix(KeYSolidityDLParser.Atom_prefixContext ctx) {
        return super.visitAtom_prefix(ctx);
    }

    private Term termForParsedVariable(ParsableVariable v, ParserRuleContext ctx) {
        if (v instanceof LogicVariable lv) {
            return capsulateTf(ctx, () -> getTermFactory().createTerm(lv));
        } else if (v instanceof ProgramVariable lv) {
            return capsulateTf(ctx, () -> getTermFactory().createTerm(lv));
        } else {
            if (v instanceof OperatorSV sv) {
                return capsulateTf(ctx, () -> getTermFactory().createTerm(sv));
            } else {
                String errorMessage = "";
                errorMessage += v + " is not a logic or program variable";
                semanticError(null, errorMessage);
            }
        }
        return null;
    }

    private @Nullable Term[] visitArguments(
            KeYSolidityDLParser.@Nullable Argument_listContext call) {
        List<Term> arguments = accept(call);
        return arguments == null ? null : arguments.toArray(new Term[0]);
    }

    @Override
    public Term visitAccessterm(KeYSolidityDLParser.AccesstermContext ctx) {
        String firstName = accept(ctx.simple_ident());

        ImmutableArray<QuantifiableVariable> boundVars = null;
        Namespace<@NonNull QuantifiableVariable> origVars = null;
        KeYSolidityDLParser.Formal_sort_argsContext genericArgsCtxt = null;
        if (ctx.formal_sort_args() != null) {
            genericArgsCtxt = ctx.formal_sort_args();
        }
        Term[] args = null;
        if (ctx.call() != null) {
            origVars = variables();
            List<QuantifiableVariable> bv = accept(ctx.call().boundVars);
            boundVars =
                bv != null ? new ImmutableArray<>(bv.toArray(new QuantifiableVariable[0])) : null;
            args = visitArguments(ctx.call().argument_list());
            if (boundVars != null) {
                unbindVars(origVars);
            }
        }

        assert firstName != null;
        Operator op;

        if ("skip".equals(firstName)) {
            op = UpdateJunctor.SKIP;
        } else {
            op = lookupVarfuncId(ctx, firstName,
                genericArgsCtxt);
        }

        Term current;
        Operator finalOp = op;
        if (op instanceof ParsableVariable) {
            if (args != null) {
                semanticError(ctx, "You used the variable `%s` like a predicate or function.", op);
            }
            if (boundVars != null) {
                // addWarning(ctx, "Bounded variable are ignored on a variable");
            }
            current = termForParsedVariable((ParsableVariable) op, ctx);
        } else {
            if (boundVars == null) {
                Term[] finalArgs = args;
                current = capsulateTf(ctx, () -> getTermFactory().createTerm(finalOp, finalArgs));
            } else {
                // sanity check
                assert op instanceof Function;
                for (int i = 0; i < args.length; i++) {
                    if (i < op.arity() && !op.bindVarsAt(i)) {
                        for (QuantifiableVariable qv : args[i].freeVars()) {
                            if (boundVars.contains(qv)) {
                                semanticError(ctx,
                                    "Building function term " + op
                                        + " with bound variables failed: " + "Variable " + qv
                                        + " must not occur free in subterm " + args[i]);
                            }
                        }
                    }
                }
                ImmutableArray<QuantifiableVariable> finalBoundVars = boundVars;
                // create term
                Term[] finalArgs1 = args;
                current = capsulateTf(ctx,
                    () -> getTermFactory().createTerm(finalOp, finalArgs1, finalBoundVars));
            }
        }
        return current;
    }

    public Object visitFuncpred_name(KeYSolidityDLParser.Funcpred_nameContext ctx) {
        List<String> parts = mapOf(ctx.name.simple_ident());
        String varfuncid = ctx.name.getText();

        if (ctx.INT_LITERAL() != null) {// number
            return toZNotation(ctx.INT_LITERAL().getText(), functions());
        }

        assert parts != null && varfuncid != null;

        if ("skip".equals(varfuncid)) {
            return UpdateJunctor.SKIP;
        }

        Operator op;
        String firstName =
            ctx.name == null ? ctx.INT_LITERAL().getText()
                    : ctx.name.simple_ident(0).getText();
        op = lookupVarfuncId(ctx, firstName, null);
        if (op instanceof ProgramVariable v && ctx.name.simple_ident().size() > 1) {
            List<KeYSolidityDLParser.Simple_identContext> otherParts =
                ctx.name.simple_ident().subList(1, ctx.name.simple_ident().size());
            Term tv = getServices().getTermFactory().createTerm(v);
            String memberName = otherParts.get(0).getText();
            memberName = StringUtil.trim(memberName, "()");
            // Operator attr = getAttributeInPrefixSort(v.sort(), memberName);
            // return createAttributeTerm(tv, attr, ctx);
            throw new RuntimeException("TODO");
        }
        return op;
    }

    private Term toZNotation(String text, Namespace<@NonNull Function> functions) {
        throw new RuntimeException("Not implemented yet: " + text);
    }

    @Override
    public @Nullable Object visitIfThenElseTerm(KeYSolidityDLParser.IfThenElseTermContext ctx) {
        return super.visitIfThenElseTerm(ctx);
    }

    @Override
    public @Nullable Object visitParallel_term(KeYSolidityDLParser.Parallel_termContext ctx) {
        return super.visitParallel_term(ctx);
    }

    @Override
    public @Nullable Object visitElementary_update_term(
            KeYSolidityDLParser.Elementary_update_termContext ctx) {
        return super.visitElementary_update_term(ctx);
    }

    @Override
    public @Nullable Object visitEquivalence_term(KeYSolidityDLParser.Equivalence_termContext ctx) {
        Term t = accept(ctx.a);
        for (var c : ctx.b) {
            t = binaryTerm(ctx, Equality.EQV, t, accept(c));
        }
        return t;
    }

    @Override
    public @Nullable Object visitImplication_term(KeYSolidityDLParser.Implication_termContext ctx) {
        final Term termL = accept(ctx.a);
        final Term termR = accept(ctx.b);
        return binaryTerm(ctx, Junctor.IMP, termL, termR);
    }

    @Override
    public @Nullable Object visitDisjunction_term(KeYSolidityDLParser.Disjunction_termContext ctx) {
        Term t = accept(ctx.a);
        for (var c : ctx.b) {
            t = binaryTerm(ctx, Junctor.OR, t, accept(c));
        }
        return t;
    }

    @Override
    public @Nullable Object visitConjunction_term(KeYSolidityDLParser.Conjunction_termContext ctx) {
        Term t = accept(ctx.a);
        for (var c : ctx.b) {
            t = binaryTerm(ctx, Junctor.AND, t, accept(c));
        }
        return t;
    }

    private @Nullable Term binaryTerm(ParserRuleContext ctx, Operator operator, @Nullable Term left,
            @Nullable Term right) {
        if (right == null) {
            return left;
        }
        return capsulateTf(ctx,
            () -> getTermFactory().createTerm(operator, Objects.requireNonNull(left), right));
    }

    @Override
    public @Nullable Object visitEquality_term(KeYSolidityDLParser.Equality_termContext ctx) {
        return super.visitEquality_term(ctx);
    }

    @Override
    public @Nullable Object visitComparison_term(KeYSolidityDLParser.Comparison_termContext ctx) {
        return super.visitComparison_term(ctx);
    }

    @Override
    public @Nullable Object visitWeak_arith_term(KeYSolidityDLParser.Weak_arith_termContext ctx) {
        return super.visitWeak_arith_term(ctx);
    }

    @Override
    public @Nullable Object visitStrong_arith_term_1(
            KeYSolidityDLParser.Strong_arith_term_1Context ctx) {
        return super.visitStrong_arith_term_1(ctx);
    }

    @Override
    public @Nullable Object visitStrong_arith_term_2(
            KeYSolidityDLParser.Strong_arith_term_2Context ctx) {
        return super.visitStrong_arith_term_2(ctx);
    }

    @Override
    public @Nullable Object visitSubstitution_term(
            KeYSolidityDLParser.Substitution_termContext ctx) {
        return super.visitSubstitution_term(ctx);
    }

    @Override
    public @Nullable Object visitCast_term(KeYSolidityDLParser.Cast_termContext ctx) {
        return super.visitCast_term(ctx);
    }

    @Override
    public @Nullable Object visitUnary_minus_term(KeYSolidityDLParser.Unary_minus_termContext ctx) {
        return super.visitUnary_minus_term(ctx);
    }
}
