/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.rule.execution;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Stack;

import org.key_project.logic.Term;
import org.key_project.logic.Visitor;
import org.key_project.logic.op.Operator;
import org.key_project.logic.op.QuantifiableVariable;
import org.key_project.logic.op.UpdateableOperator;
import org.key_project.logic.op.sv.SchemaVariable;
import org.key_project.prover.rules.Rule;
import org.key_project.prover.rules.RuleApp;
import org.key_project.prover.sequent.PosInOccurrence;
import org.key_project.solidity.common.Services;
import org.key_project.solidity.logic.GenericArgument;
import org.key_project.solidity.logic.SolidityBlock;
import org.key_project.solidity.logic.TermBuilder;
import org.key_project.solidity.logic.op.ElementaryUpdate;
import org.key_project.solidity.logic.op.ParametricFunctionInstance;
import org.key_project.solidity.logic.op.SModality;
import org.key_project.solidity.logic.op.SubstOp;
import org.key_project.solidity.program.ast.SolidityProgramElement;
import org.key_project.solidity.program.ast.visitor.ProgramContextAdder;
import org.key_project.solidity.program.ast.visitor.ProgramReplaceVisitor;
import org.key_project.solidity.program.ext.ContextStatementBlock;
import org.key_project.solidity.proof.Goal;
import org.key_project.solidity.rule.TacletApp;
import org.key_project.solidity.rule.matching.inst.ContextInstantiationEntry;
import org.key_project.solidity.rule.matching.inst.SVInstantiations;
import org.key_project.solidity.rule.metaconstruct.TermTransformer;
import org.key_project.solidity.rule.sv.ModalOperatorSV;
import org.key_project.solidity.rule.sv.ProgramSV;
import org.key_project.util.collection.ImmutableArray;
import org.key_project.util.collection.ImmutableList;
import org.key_project.util.collection.ImmutableSLList;


/// visitor for <t> execPostOrder </t> of [Term]. Called with that method
/// on a term, the visitor builds a new term replacing SchemaVariables with their instantiations
/// that
/// are given as a SVInstantiations object.
public class SyntacticalReplaceVisitor implements Visitor<Term> {
    protected final SVInstantiations svInst;
    protected final Services services;
    /// the termbuilder used to construct terms
    protected final TermBuilder tb;
    private Term computedResult = null;
    protected final PosInOccurrence applicationPosInOccurrence;
    protected final Rule rule;
    protected final Goal goal;
    protected final RuleApp ruleApp;


    /// the stack contains the subterms that will be added in the next step of execPostOrder in Term
    /// in order to build the new term. A boolean value between or under the subterms on the stack
    /// indicate that a term using these subterms should build a new term instead of using the old
    /// one, because one of its subterms has been built, too.
    private final Stack<Object> subStack; // of Term (and Boolean)
    private final Boolean newMarker = Boolean.TRUE;
    private final Deque<Term> tacletTermStack = new ArrayDeque<>();


    /// constructs a term visitor replacing any occurrence of a schemavariable found in
    /// `svInst` by its instantiation
    ///
    /// @param applicationPosInOccurrence the application position
    /// @param svInst mapping of schemavariables to their instantiation
    /// @param goal the current goal
    /// @param rule the applied rule
    /// @param ruleApp the rule application
    /// @param services the Services
    /// @param termBuilder the TermBuilder to use (allows to use the non cached version)
    private SyntacticalReplaceVisitor(
            PosInOccurrence applicationPosInOccurrence, SVInstantiations svInst, Goal goal,
            Rule rule, RuleApp ruleApp, Services services, TermBuilder termBuilder) {
        this.services = services;
        this.tb = termBuilder;
        this.svInst = svInst;
        this.applicationPosInOccurrence = applicationPosInOccurrence;
        this.rule = rule;
        this.ruleApp = ruleApp;
        this.goal = goal;
        subStack = new Stack<>(); // of Term
    }

    /// constructs a term visitor replacing any occurrence of a schemavariable found in
    /// `svInst` by its instantiation
    ///
    /// @param applicationPosInOccurrence the application position
    /// @param svInst mapping of schemavariables to their instantiation
    /// @param goal the current goal
    /// @param rule the applied rule
    /// @param ruleApp the rule application
    /// @param services the Services
    public SyntacticalReplaceVisitor(
            PosInOccurrence applicationPosInOccurrence, SVInstantiations svInst, Goal goal,
            Rule rule, RuleApp ruleApp, Services services) {
        this(applicationPosInOccurrence, svInst, goal, rule, ruleApp,
            services, services.getTermBuilder());
    }

    public SyntacticalReplaceVisitor(
            PosInOccurrence applicationPosInOccurrence, Goal goal, Rule rule, RuleApp ruleApp,
            Services services, TermBuilder termBuilder) {
        this(applicationPosInOccurrence,
            SVInstantiations.EMPTY_SVINSTANTIATIONS, goal, rule, ruleApp, services);
    }

    public SyntacticalReplaceVisitor(PosInOccurrence applicationPosInOccurrence, Services services,
            Rule rule, RuleApp ruleApp) {
        this(applicationPosInOccurrence, ruleApp instanceof TacletApp ta ? ta.instantiations()
                : SVInstantiations.EMPTY_SVINSTANTIATIONS,
            null, rule, ruleApp, services);
    }

    public SyntacticalReplaceVisitor(SVInstantiations svInst, Services services) {
        this(null, services, null, null);
    }

    /// performs the syntactic replacement of schemavariables with their instantiations
    @Override
    public void visit(final Term visited) {
        // Sort equality has to be ensured before calling this method
        Operator visitedOp = visited.op();
        if (visitedOp instanceof org.key_project.logic.op.sv.SchemaVariable sv
                && visitedOp.arity() == 0
                && svInst.isInstantiated(sv)
                && (!(visitedOp instanceof ProgramSV psv && psv.isListSV()))) {
            final Term newTerm = toTerm(svInst.getTermInstantiation(sv,
                /* svInst.getExecutionContext(), */ services));
            pushNew(newTerm);
        } else {
            // instantiation of Rust block
            boolean rBlockChanged = false;

            if (visitedOp instanceof SModality mod) {
                var block = mod.programBlock();
                var olfRb = block;
                block = replacePrg(svInst, block);
                if (block != olfRb) {
                    rBlockChanged = true;
                }

                visitedOp = instantiateModality(mod, block);
            }

            final Operator newOp = instantiateOperator(visitedOp);

            // instantiate bound variables
            final var boundVars =
                instantiateBoundVariables(visited);

            // instantiate sub terms
            final Term[] neededsubs = neededSubs(newOp != null ? newOp.arity() : 0);
            if (boundVars != visited.boundVars() || rBlockChanged || (newOp != visitedOp)
                    || (!subStack.empty() && subStack.peek() == newMarker)) {
                final Term newTerm = tb.tf().createTerm(newOp, neededsubs,
                    (ImmutableArray<QuantifiableVariable>) boundVars);
                pushNew(resolveSubst(newTerm));
            } else {
                Term t;
                t = visited;
                t = resolveSubst(t);
                if (t == visited) {
                    subStack.push(t);
                } else {
                    pushNew(t);
                }
            }
        }
    }

    private SolidityProgramElement addContext(ContextStatementBlock pe) {
        final ContextInstantiationEntry cie = svInst.getContextInstantiation();
        if (cie == null) {
            throw new IllegalStateException("Context should also be instantiated");
        }

        if (cie.prefix() != null) {
            return ProgramContextAdder.INSTANCE.start(
                cie.contextProgram(), pe, cie.getInstantiation());
        }

        return pe;
    }

    private SolidityBlock replacePrg(SVInstantiations svInst, SolidityBlock sb) {
        if (svInst.isEmpty()) {
            return sb;
        }

        ProgramReplaceVisitor trans;
        SolidityProgramElement result = null;

        if (sb.program() instanceof ContextStatementBlock cbe) {
            trans = new ProgramReplaceVisitor(
                cbe,
                services, svInst);
            trans.start();
            result = addContext((ContextStatementBlock) trans.result());
        } else {
            trans = new ProgramReplaceVisitor(sb.program(), services, svInst);
            trans.start();
            result = trans.result();
        }
        return (result == sb.program()) ? sb : new SolidityBlock(result);
    }

    protected void pushNew(Object t) {
        if (subStack.empty() || subStack.peek() != newMarker) {
            subStack.push(newMarker);
        }
        subStack.push(t);
    }

    /// the method is only still invoked to allow the
    /// TODO to recursively replace meta variables
    protected Term toTerm(Term o) {
        return o;
    }

    private Term resolveSubst(Term t) {
        if (t.op() instanceof SubstOp) {
            Term resolved = ((SubstOp) t.op()).apply(t, tb);
            return resolved;
        } else {

            return t;
        }
    }

    private Term[] neededSubs(int n) {
        boolean newTerm = false;
        Term[] result = new Term[n];
        for (int i = n - 1; i >= 0; i--) {
            Object top = subStack.pop();
            if (top == newMarker) {
                newTerm = true;
                top = subStack.pop();
            }
            result[i] = (Term) top;
        }
        if (newTerm && (subStack.empty() || subStack.peek() != newMarker)) {
            subStack.push(newMarker);
        }
        return result;
    }

    private ImmutableArray<? extends QuantifiableVariable> instantiateBoundVariables(Term visited) {
        var vBoundVars = visited.boundVars();
        if (!vBoundVars.isEmpty()) {
            final QuantifiableVariable[] newVars = new QuantifiableVariable[vBoundVars.size()];
            boolean varsChanged = false;

            for (int j = 0, size = vBoundVars.size(); j < size; j++) {
                QuantifiableVariable boundVar = vBoundVars.get(j);
                if (boundVar instanceof org.key_project.logic.op.sv.SchemaVariable boundSchemaVariable) {
                    final Term instantiationForBoundSchemaVariable =
                        (Term) svInst.getInstantiation(boundSchemaVariable);
                    if (instantiationForBoundSchemaVariable != null) {
                        boundVar = (QuantifiableVariable) instantiationForBoundSchemaVariable.op();
                    } else {
                        // this case may happen for PO generation of taclets
                        boundVar = (QuantifiableVariable) boundSchemaVariable;
                    }
                    varsChanged = true;
                }
                newVars[j] = boundVar;
            }

            if (varsChanged) {
                vBoundVars = new ImmutableArray<>(newVars);
            }
        }
        return vBoundVars;
    }

    private Operator instantiateOperator(Operator p_operatorToBeInstantiated) {
        Operator instantiatedOp = p_operatorToBeInstantiated;
        /*
         * if (p_operatorToBeInstantiated instanceof SortDependingFunction) {
         * instantiatedOp =
         * handleSortDependingSymbol((SortDependingFunction) p_operatorToBeInstantiated);
         * } else
         */
        if (p_operatorToBeInstantiated instanceof ParametricFunctionInstance pfi) {
            instantiatedOp = handleParametricFunction(pfi);
        } else if (p_operatorToBeInstantiated instanceof ElementaryUpdate eu) {
            instantiatedOp =
                instantiateElementaryUpdate(eu);
        } else if (p_operatorToBeInstantiated instanceof SchemaVariable) {
            if (!(p_operatorToBeInstantiated instanceof ProgramSV)
                    || !((ProgramSV) p_operatorToBeInstantiated).isListSV()) {
                instantiatedOp =
                    (Operator) svInst.getInstantiation(
                        (org.key_project.logic.op.sv.SchemaVariable) p_operatorToBeInstantiated);
            }
        }
        assert instantiatedOp != null;

        return instantiatedOp;
    }

    private Operator handleParametricFunction(ParametricFunctionInstance pfi) {
        ImmutableList<GenericArgument> args = ImmutableSLList.nil();

        for (int i = pfi.getArgs().size() - 1; i >= 0; i--) {
            args = args.prepend(pfi.getArgs().get(i).instantiate(svInst, services));
        }

        return ParametricFunctionInstance.get(pfi.getBase(), args, services);
    }

    private ElementaryUpdate instantiateElementaryUpdate(ElementaryUpdate op) {
        final UpdateableOperator originalLhs = op.lhs();
        if (originalLhs instanceof SchemaVariable) {
            Object lhsInst =
                svInst.getInstantiation((org.key_project.logic.op.sv.SchemaVariable) originalLhs);
            if (lhsInst instanceof Term) {
                lhsInst = ((Term) lhsInst).op();
            }

            final UpdateableOperator newLhs;
            if (lhsInst instanceof UpdateableOperator) {
                newLhs = (UpdateableOperator) lhsInst;
            } else {
                assert false : "not updateable: " + lhsInst;
                throw new IllegalStateException("Encountered non-updateable operator " + lhsInst
                    + " on left-hand side of update.");
            }
            return newLhs == originalLhs ? op : ElementaryUpdate.getInstance(newLhs);
        } else {
            return op;
        }
    }

    private Operator instantiateModality(SModality op, SolidityBlock sb) {
        SModality.SolidityModalityKind kind = op.kind();
        if (op.kind() instanceof ModalOperatorSV) {
            kind = (SModality.SolidityModalityKind) svInst.getInstantiation(op.kind());
        }
        if (sb != op.programBlock() || kind != op.kind()) {
            return SModality.getModality(kind, sb);
        }
        return op;
    }

    /// delivers the new built term
    public Term getTerm() {
        if (computedResult == null) {
            Object o = null;
            do {
                o = subStack.pop();
            } while (o == newMarker);
            Term t = (Term) o;
            // CollisionDeletingSubstitutionTermApplier substVisit
            // = new CollisionDeletingSubstitutionTermApplier();
            // t.execPostOrder(substVisit);
            // t=substVisit.getTerm();
            computedResult = t;
        }
        return computedResult;
    }

    /// {@inheritDoc}
    @Override
    public void subtreeEntered(Term subtreeRoot) {
        tacletTermStack.push(subtreeRoot);
    }

    /// this method is called in execPreOrder and execPostOrder in class Term when leaving the
    /// subtree rooted in the term subtreeRoot. Default implementation is to do nothing. Subclasses
    /// can override this method when the visitor behaviour depends on information bound to
    /// subtrees.
    ///
    /// @param subtreeRoot root of the subtree which the visitor leaves.
    @Override
    public void subtreeLeft(Term subtreeRoot) {
        tacletTermStack.pop();
        if (subtreeRoot.op() instanceof TermTransformer mop) {
            final Term newTerm = mop.transform((Term) subStack.pop(), svInst, services);
            pushNew(newTerm);
        }
    }
}
