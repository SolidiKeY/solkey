/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.parser.builder;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

import org.key_project.logic.Name;
import org.key_project.logic.Namespace;
import org.key_project.logic.Term;
import org.key_project.logic.TermCreationException;
import org.key_project.logic.op.AbstractSortedOperator;
import org.key_project.logic.op.Function;
import org.key_project.logic.op.Operator;
import org.key_project.logic.op.ParsableVariable;
import org.key_project.logic.op.QuantifiableVariable;
import org.key_project.logic.op.sv.OperatorSV;
import org.key_project.logic.op.sv.SchemaVariable;
import org.key_project.logic.sort.Sort;
import org.key_project.prover.sequent.Sequent;
import org.key_project.prover.sequent.SequentFormula;
import org.key_project.solidity.common.Services;
import org.key_project.solidity.logic.NamespaceSet;
import org.key_project.solidity.logic.SolidityBlock;
import org.key_project.solidity.logic.TermFactory;
import org.key_project.solidity.logic.op.*;
import org.key_project.solidity.parser.KeYSolidityDLLexer;
import org.key_project.solidity.parser.KeYSolidityDLParser;
import org.key_project.solidity.program.SolidityReader;
import org.key_project.solidity.program.SoliditySchemaReader;
import org.key_project.solidity.proof.calculus.SoliditySequentKit;
import org.key_project.solidity.rule.sv.ModalOperatorSV;
import org.key_project.solidity.rule.sv.VariableSV;
import org.key_project.solidity.theory.LDT;
import org.key_project.solidity.util.parsing.BuildingException;
import org.key_project.util.collection.ImmutableArray;
import org.key_project.util.collection.ImmutableList;
import org.key_project.util.collection.ImmutableSLList;
import org.key_project.util.collection.ImmutableSet;
import org.key_project.util.java.StringUtil;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import static org.key_project.solidity.logic.SolidityDLTheory.FORMULA;

public class ExpressionBuilder extends DefaultBuilder {

    private boolean soliditySchemaModeAllowed;

    public record BoundVar(Name name, Sort sort) {
    }

    private final List<BoundVariable> boundVars = new ArrayList<>();


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

    private void bindVar() {
        namespaces().setVariables(new Namespace<>(variables()));
    }

    private BoundVariable bindVar(String name, Sort sort) {
        var e = new BoundVariable(new Name(name), sort);
        boundVars.add(e);
        return e;
    }

    private void unbindVars(List<@NonNull BoundVariable> vars) {
        boundVars.removeAll(vars);
    }


    private static class PairOfStringAndSolidityBlock {
        String opName;
        SolidityBlock solidityBlock;
    }

    private PairOfStringAndSolidityBlock getSolidityBlock(Token t) {
        PairOfStringAndSolidityBlock sjb = new PairOfStringAndSolidityBlock();
        String s = t.getText().trim();
        String cleanSolidity = trimSolidityBlock(s);
        sjb.opName = operatorOfSolidityBlock(s);

        try {
            try {
                if (soliditySchemaModeAllowed) {// TEST
                    final SoliditySchemaReader schemaSolidityReader =
                        new SoliditySchemaReader(services, nss);
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
            throw new BuildingException(t, "Could not parse Solidity code: '" + cleanSolidity + "'",
                e);
        }
        return sjb;
    }

    private ImmutableSet<SModality.SolidityModalityKind> lookupOperatorSV(String opName,
            ImmutableSet<SModality.SolidityModalityKind> modalityKinds) {
        SchemaVariable sv = schemaVariables().lookup(new Name(opName));
        if (sv instanceof ModalOperatorSV osv) {
            modalityKinds = modalityKinds.union(osv.getModalities());
        } else {
            semanticError(null, "Schema variable " + opName + " not defined.");
        }
        return modalityKinds;
    }

    protected ImmutableSet<SModality.SolidityModalityKind> opSVHelper(String opName,
            ImmutableSet<SModality.SolidityModalityKind> modalityKinds) {
        if (opName.charAt(0) == '#') {
            return lookupOperatorSV(opName, modalityKinds);
        } else {
            SModality.SolidityModalityKind m = SModality.SolidityModalityKind.getKind(opName);
            if (m == null) {
                semanticError(null, "Unrecognised operator: " + opName);
            }
            modalityKinds = modalityKinds.add(m);
        }
        return modalityKinds;
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
            return LogicVariable.create(deBruijn, boundVars.get(idx).sort());
        }

        return super.lookupVarfuncId(ctx, varfuncName, genericArgsCtxt);
    }


    public TermFactory getTermFactory() {
        return getServices().getTermFactory();
    }

    public Object visitTermParen(KeYSolidityDLParser.TermParenContext ctx) {
        Term base = accept(ctx.term());
        if (ctx.attribute().isEmpty()) {
            return base;
        }
        return null;// handleAttributes(base, ctx.attribute());
    }

    @Override
    public @Nullable Object visitTermorseq(KeYSolidityDLParser.TermorseqContext ctx) {
        Term head = accept(ctx.head);
        Sequent s = accept(ctx.s);
        ImmutableList<SequentFormula> ss = accept(ctx.ss);
        if (head != null && s == null && ss == null) {
            return head;
        }
        if (head != null && ss != null) {
            // A sequent with only head in the antecedent.
            return SoliditySequentKit
                    .createSequent(ImmutableSLList.singleton(new SequentFormula(head)), ss);
        }
        if (head != null && s != null) {
            // A sequent. Prepend head to the antecedent.
            ImmutableList<SequentFormula> newAnt =
                s.antecedent().insertFirst(new SequentFormula(head)).getFormulaList();
            return SoliditySequentKit.createSequent(newAnt, s.succedent().asList());
        }
        if (ss != null) {
            return SoliditySequentKit.createSequent(ImmutableSLList.nil(), ss);
        }
        assert (false);
        return null;
    }

    @Override
    public @Nullable Object visitSemisequent(KeYSolidityDLParser.SemisequentContext ctx) {
        ImmutableList<SequentFormula> semiSeq = accept(ctx.ss);
        if (semiSeq == null) {
            semiSeq = ImmutableSLList.nil();
        }
        Term head = accept(ctx.term());
        if (head != null) {
            semiSeq = semiSeq.prepend(new SequentFormula(head));
        }
        return semiSeq;
    }

    @Override
    public Sequent visitSeq(KeYSolidityDLParser.SeqContext ctx) {
        return SoliditySequentKit.createSequent(accept(ctx.ant), accept(ctx.suc));
    }

    @Override
    public Sequent visitSeqEOF(KeYSolidityDLParser.SeqEOFContext ctx) {
        return accept(ctx.seq());
    }


    @Override
    public @Nullable Object visitQuantifierterm(KeYSolidityDLParser.QuantifiertermContext ctx) {
        Operator op = null;
        Namespace<@NonNull QuantifiableVariable> orig = variables();
        if (ctx.FORALL() != null) {
            op = Quantifier.ALL;
        }
        if (ctx.EXISTS() != null) {
            op = Quantifier.EX;
        }
        List<@NonNull BoundVariable> vars = accept(ctx.bound_variables());
        assert vars != null;
        var bound = new ImmutableArray<QuantifiableVariable>(vars);
        Term a1 = accept(ctx.sub);
        Term a = getTermFactory().createTerm(op, new ImmutableArray<>(a1),
            bound);
        unbindVars(orig);
        unbindVars(vars);
        return a;
    }


    @Override
    public @Nullable Object visitModality_term(KeYSolidityDLParser.Modality_termContext ctx) {
        Term a1 = accept(ctx.sub);
        if (ctx.MODALITY() == null) {
            return a1;
        }

        PairOfStringAndSolidityBlock strSMB = getSolidityBlock(ctx.MODALITY().getSymbol());
        Operator op;
        if (strSMB.opName.charAt(0) == '#') {
            /*
             * if (!inSchemaMode()) { semanticError(ctx,
             * "No schema elements allowed outside taclet declarations (" + strSMB.opName + ")"); }
             */
            var kind =
                (SModality.SolidityModalityKind) schemaVariables().lookup(new Name(strSMB.opName));
            op = SModality.getModality(kind, strSMB.solidityBlock);
        } else {
            var kind = SModality.SolidityModalityKind.getKind(strSMB.opName);
            op = SModality.getModality(kind, strSMB.solidityBlock);
        }
        if (op == null) {
            semanticError(ctx, "Unknown modal operator: " + strSMB.opName);
        }

        return capsulateTf(ctx,
            () -> getTermFactory().createTerm(op, new Term[] { a1 }, null));
    }

    @Override
    public @Nullable Term visitUpdate_term(KeYSolidityDLParser.Update_termContext ctx) {
        Term t = oneOf(ctx.atom_prefix(), ctx.unary_formula());
        if (ctx.u.isEmpty()) {
            return t;
        }
        Term u = accept(ctx.u);
        return getTermFactory().createTerm(UpdateApplication.UPDATE_APPLICATION, u, t);
    }

    @Override
    public Object visitBoolean_literal(KeYSolidityDLParser.Boolean_literalContext ctx) {
        if (ctx.TRUE() != null) {
            return capsulateTf(ctx, () -> getTermFactory().createTerm(Junctor.TRUE));
        } else {
            return capsulateTf(ctx, () -> getTermFactory().createTerm(Junctor.FALSE));
        }
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

    @Override
    public List<Term> visitArgument_list(KeYSolidityDLParser.Argument_listContext ctx) {
        return mapOf(ctx.term());
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
        Namespace<@NonNull QuantifiableVariable> origVars;
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
            op = lookupVarfuncId(ctx, firstName, genericArgsCtxt);
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

    public List<@NonNull BoundVar> visitBound_variables(
            KeYSolidityDLParser.Bound_variablesContext ctx) {
        return mapOf(ctx.one_bound_variable());
    }

    @Override
    public Object visitOne_bound_variable(
            KeYSolidityDLParser.One_bound_variableContext ctx) {
        String id = accept(ctx.simple_ident());
        Sort sort = accept(ctx.sortId());

        assert id != null;
        SchemaVariable ts = schemaVariables().lookup(new Name(id));
        if (ts != null) {
            if (!(ts instanceof VariableSV)) {
                semanticError(ctx,
                    ts + " is not allowed in a quantifier. Note, that you can't "
                        + "use the normal syntax for quantifiers of the form \"\\exists int i;\""
                        + " in taclets. You have to define the variable as a schema variable"
                        + " and use the syntax \"\\exists i;\" instead.");
            }
            bindVar();
            return ts;
        }

        if (sort != null) {
            return bindVar(id, sort);
        }

        QuantifiableVariable result =
            doLookup(new Name(ctx.id.getText()), variables());

        if (result == null) {
            semanticError(ctx, "There is no schema variable or variable named " + id);
        }

        return result;
    }

    public Object visitFuncpred_name(KeYSolidityDLParser.Funcpred_nameContext ctx) {
        List<String> parts = mapOf(ctx.name.simple_ident());
        String varfuncid = ctx.name.getText();
        if (ctx.INT_LITERAL() != null) {// number
            return toZNotation(ctx.INT_LITERAL().getText());
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

    @Override
    public Object visitInteger(KeYSolidityDLParser.IntegerContext ctx) {
        return toZNotation(ctx.getText());
    }

    private Term toZNotation(String number) {
        var z = services.getTheoryInfo().getIntLDT().getNumberSymbol();
        return getTermFactory().createTerm(z, toNum(number));
    }

    private Term toNum(String number) {
        String s = number;
        final boolean negative = (s.charAt(0) == '-');
        if (negative) {
            s = number.substring(1, s.length());
        }
        if (s.startsWith("0x")) {
            try {
                BigInteger bi = new BigInteger(s.substring(2), 16);
                s = bi.toString();
            } catch (NumberFormatException nfe) {
                // Debug.fail("Not a hexadecimal constant (BTW, this should not have happened).");
            }
        }
        Term result = getTermFactory().createTerm(functions().lookup(new Name("#")));

        for (int i = 0; i < s.length(); i++) {
            result = getTermFactory()
                    .createTerm(functions().lookup(new Name(s.substring(i, i + 1))), result);
        }

        if (negative) {
            result = getTermFactory().createTerm(functions().lookup(new Name("neglit")), result);
        }

        return result;
    }

    @Override
    public @Nullable Object visitIfThenElseTerm(KeYSolidityDLParser.IfThenElseTermContext ctx) {
        Term condF = (Term) ctx.condF.accept(this);
        if (condF.sort() != FORMULA) {
            semanticError(ctx, "Condition of an \\if-then-else term has to be a formula.");
        }
        Term thenT = (Term) ctx.thenT.accept(this);
        Term elseT = (Term) ctx.elseT.accept(this);
        return capsulateTf(ctx,
            () -> getTermFactory().createTerm(IfThenElse.IF_THEN_ELSE, condF, thenT, elseT));
    }


    @Override
    public @Nullable Term visitParallel_term(KeYSolidityDLParser.Parallel_termContext ctx) {
        List<Term> t = mapOf(ctx.elementary_update_term());
        Term a = t.getFirst();
        for (int i = 1; i < t.size(); i++) {
            a = getTermFactory().createTerm(UpdateJunctor.PARALLEL_UPDATE, a, t.get(i));
        }
        return a;
    }

    @Override
    public @Nullable Term visitElementary_update_term(
            KeYSolidityDLParser.Elementary_update_termContext ctx) {
        Term a = accept(ctx.a);
        Term b = accept(ctx.b);
        if (b != null) {
            return getServices().getTermBuilder().elementary(Objects.requireNonNull(a), b);
        }
        return a;
    }

    @Override
    public @Nullable Term visitEquivalence_term(KeYSolidityDLParser.Equivalence_termContext ctx) {
        Term t = accept(ctx.a);
        for (var c : ctx.b) {
            t = binaryTerm(ctx, Equality.EQV, t, accept(c));
        }
        return t;
    }

    @Override
    public @Nullable Term visitImplication_term(KeYSolidityDLParser.Implication_termContext ctx) {
        final Term termL = accept(ctx.a);
        final Term termR = accept(ctx.b);
        return binaryTerm(ctx, Junctor.IMP, termL, termR);
    }

    @Override
    public @Nullable Term visitDisjunction_term(KeYSolidityDLParser.Disjunction_termContext ctx) {
        Term t = accept(ctx.a);
        for (var c : ctx.b) {
            t = binaryTerm(ctx, Junctor.OR, t, accept(c));
        }
        return t;
    }

    @Override
    public @Nullable Term visitConjunction_term(KeYSolidityDLParser.Conjunction_termContext ctx) {
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
    public Object visitStrong_arith_term_2(KeYSolidityDLParser.Strong_arith_term_2Context ctx) {
        if (ctx.b.isEmpty()) { // fast path
            return accept(ctx.a);
        }

        List<Term> termL = mapOf(ctx.b);
        // List<String> opName = ctx.op.stream().map(it -> it.getType()== KeYLexer.PERCENT ? "mod" :
        // "div").collect(Collectors.toList());

        Term term = accept(ctx.a);
        var sort = term.sort();
        if (sort == null) {
            semanticError(ctx, "No sort for term '%s'", term);
        }

        var ldt = services.getTheoryInfo().getLDTFor(sort);

        if (ldt == null) {
            // falling back to integer ldt (for instance for untyped schema variables)
            ldt = services.getTheoryInfo().getIntLDT();
        }

        assert ctx.op.size() == ctx.b.size();

        for (int i = 0; i < termL.size(); i++) {
            var opName = ctx.op.get(i).getType() == KeYSolidityDLLexer.PERCENT ? "mod" : "div";
            Function op = ldt.getFunctionFor(opName, services);
            if (op == null) {
                semanticError(ctx, "Could not find function symbol '%s' for sort '%s'.", opName,
                    sort);
            }
            term = binaryTerm(ctx, op, term, termL.get(i));
        }
        return term;
    }

    @Override
    public Object visitSubstitution_term(KeYSolidityDLParser.Substitution_termContext ctx) {
        SubstOp op = SubstOp.SUBST;
        Namespace<QuantifiableVariable> orig = variables();
        AbstractSortedOperator v = accept(ctx.bv);
        unbindVars(orig);
        if (v instanceof LogicVariable) {
            // bindVar((LogicVariable) v);
            throw new RuntimeException("TODO @ DD");
        } else {
            bindVar();
        }

        Term a1 = accept(ctx.replacement);
        Term a2 = oneOf(ctx.atom_prefix(), ctx.unary_formula());
        try {
            Term result =
                getServices().getTermBuilder().subst(op, (QuantifiableVariable) v, a1, a2);
            return result;
        } catch (Exception e) {
            throw new BuildingException(ctx, e);
        } finally {
            unbindVars(orig);
        }
    }


    @Override
    public Object visitUnary_minus_term(KeYSolidityDLParser.Unary_minus_termContext ctx) {
        Term result = accept(ctx.sub);
        assert result != null;
        if (ctx.MINUS() != null) {
            Operator Z = functions().lookup("Z");
            if (result.op() == Z) {
                // weigl: rewrite neg(Z(1(#)) to Z(neglit(1(#))
                // This mimics the old KeYSolidityDLParser behaviour. Unknown if necessary.
                final Function neglit =
                    services.getTheoryInfo().getIntLDT().getNegativeNumberSign();
                final Term num = result.sub(0);
                return capsulateTf(ctx,
                    () -> getTermFactory().createTerm(Z, getTermFactory().createTerm(neglit, num)));
            } else if (result.sort() != FORMULA) {
                Sort sort = result.sort();
                if (sort == null) {
                    semanticError(ctx, "No sort for %s", result);
                }
                LDT ldt = services.getTheoryInfo().getLDTFor(sort);
                if (ldt == null) {
                    // falling back to integer ldt (for instance for untyped schema variables)
                    ldt = services.getTheoryInfo().getIntLDT();
                }
                // TODO(DD): Can this be simplified?
                Function op = ldt.getFunctionFor("neg", services);
                if (op == null) {
                    semanticError(ctx, "Could not find function symbol 'neg' for sort '%s'.", sort);
                }
                return capsulateTf(ctx, () -> getTermFactory().createTerm(op, result));
            } else {
                semanticError(ctx, "Formulas cannot be prefixed with '-'");
            }
        }
        return result;
    }

    @Override
    public @Nullable Term visitNegation_term(KeYSolidityDLParser.Negation_termContext ctx) {
        Term termL = accept(ctx.sub);
        if (ctx.NOT() != null) {
            return capsulateTf(ctx, () -> getTermFactory().createTerm(Junctor.NOT, termL));
        } else {
            return termL;
        }
    }

    @Override
    public @Nullable Term visitEquality_term(KeYSolidityDLParser.Equality_termContext ctx) {
        Term termL = accept(ctx.a);
        Term termR = accept(ctx.b);
        Term eq = binaryTerm(ctx, Equality.EQUALS, termL, termR);
        if (ctx.NOT_EQUALS() != null) {
            return capsulateTf(ctx, () -> getTermFactory().createTerm(Junctor.NOT, eq));
        }
        return eq;
    }

    @Override
    public @Nullable Object visitComparison_term(KeYSolidityDLParser.Comparison_termContext ctx) {
        Term termL = accept(ctx.a);
        Term termR = accept(ctx.b);

        if (termR == null) {
            return termL;
        }

        String op_name = "";
        if (ctx.LESS() != null) {
            op_name = "lt";
        }
        if (ctx.LESSEQUAL() != null) {
            op_name = "leq";
        }
        if (ctx.GREATER() != null) {
            op_name = "gt";
        }
        if (ctx.GREATEREQUAL() != null) {
            op_name = "geq";
        }
        return binaryLDTSpecificTerm(ctx, op_name, termL, termR);
    }

    @Override
    public @Nullable Object visitWeak_arith_term(KeYSolidityDLParser.Weak_arith_termContext ctx) {
        Term termL = Objects.requireNonNull(accept(ctx.a));
        if (ctx.op.isEmpty()) {
            return termL;
        }

        List<Term> terms = mapOf(ctx.b);
        Term last = termL;
        for (int i = 0; i < terms.size(); i++) {
            String opname = "";
            switch (ctx.op.get(i).getType()) {
                case KeYSolidityDLLexer.UTF_INTERSECT -> opname = "intersect";
                case KeYSolidityDLLexer.UTF_SETMINUS -> opname = "setMinus";
                case KeYSolidityDLLexer.UTF_UNION -> opname = "union";
                case KeYSolidityDLLexer.PLUS -> opname = "add";
                case KeYSolidityDLLexer.MINUS -> opname = "sub";
                default -> semanticError(ctx, "Unexpected token: %s", ctx.op.get(i));
            }
            Term cur = terms.get(i);
            last = binaryLDTSpecificTerm(ctx, opname, last, cur);
        }
        return last;
    }

    private Term binaryLDTSpecificTerm(ParserRuleContext ctx, String opname, Term last, Term cur) {
        Sort sort = last.sort();
        if (sort == null) {
            semanticError(ctx, "No sort for %s", last);
        }
        LDT ldt = services.getTheoryInfo().getLDTFor(sort);
        if (ldt == null) {
            // falling back to integer ldt (for instance for untyped schema variables)
            ldt = services.getTheoryInfo().getIntLDT();
        }
        Function op = ldt.getFunctionFor(opname, services);
        if (op == null) {
            semanticError(ctx, "Could not find function symbol '%s' for sort '%s'.", opname, sort);
        }
        return binaryTerm(ctx, op, last, cur);
    }

    @Override
    public Object visitStrong_arith_term_1(KeYSolidityDLParser.Strong_arith_term_1Context ctx) {
        Term termL = accept(ctx.a);
        if (ctx.b.isEmpty()) {
            return termL;
        }
        List<Term> terms = mapOf(ctx.b);
        Term last = termL;
        for (Term cur : terms) {
            last = binaryLDTSpecificTerm(ctx, "mul", last, cur);
        }
        return last;
    }
}
