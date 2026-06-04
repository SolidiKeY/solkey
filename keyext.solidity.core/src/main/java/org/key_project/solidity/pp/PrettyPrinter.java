/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.pp;

import org.key_project.logic.op.sv.SchemaVariable;
import org.key_project.solidity.common.Services;
import org.key_project.solidity.logic.op.ProgramVariable;
import org.key_project.solidity.program.ast.SolidityProgramElement;
import org.key_project.solidity.program.ast.declarations.FunctionEnums.DataLocation;
import org.key_project.solidity.program.ast.declarations.StatementVariableDeclaration;
import org.key_project.solidity.program.ast.expressions.*;
import org.key_project.solidity.program.ast.expressions.literals.BoolLiteral;
import org.key_project.solidity.program.ast.expressions.literals.Uint256Literal;
import org.key_project.solidity.program.ast.expressions.operators.*;
import org.key_project.solidity.program.ast.references.ContractReference;
import org.key_project.solidity.program.ast.references.EnumReference;
import org.key_project.solidity.program.ast.references.FunctionReference;
import org.key_project.solidity.program.ast.references.ModifierReference;
import org.key_project.solidity.program.ast.references.TypeReference;
import org.key_project.solidity.program.ast.references.UnresolvedReferenceException;
import org.key_project.solidity.program.ast.statement.*;
import org.key_project.solidity.program.ast.visitor.Visitor;
import org.key_project.solidity.program.ext.ContextStatementBlock;
import org.key_project.solidity.rule.matching.inst.SVInstantiations;
import org.key_project.solidity.rule.metaconstruct.ProgramTransformer;
import org.key_project.solidity.rule.sv.ProgramSV;
import org.key_project.util.collection.ImmutableArray;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PrettyPrinter implements Visitor {
    private static final Logger LOGGER = LoggerFactory.getLogger(PrettyPrinter.class.getName());
    private final PosTableLayouter layouter;

    private boolean startAlreadyMarked;
    private @Nullable Object firstStatement;
    private boolean endAlreadyMarked;

    private final SVInstantiations instantiations;
    private final @Nullable Services services;
    private boolean usePrettyPrinting;
    private boolean useUnicodeSymbols;

    /// creates a new PrettyPrinter
    public PrettyPrinter(PosTableLayouter out) {
        this(out, SVInstantiations.EMPTY_SVINSTANTIATIONS, null, true, true);
    }

    public PrettyPrinter(PosTableLayouter o, SVInstantiations svi, @Nullable Services services,
            boolean usePrettyPrinting, boolean useUnicodeSymbols) {
        this.layouter = o;
        this.instantiations = svi;
        this.services = services;
        this.usePrettyPrinting = usePrettyPrinting;
        this.useUnicodeSymbols = useUnicodeSymbols;
    }

    /// Creates a PrettyPrinter that does not create a position table.
    public static PrettyPrinter purePrinter() {
        return new PrettyPrinter(PosTableLayouter.pure());
    }

    /// @return the result
    public String result() {
        return layouter.result();
    }

    /// Alternative entry method for this class. Omits the trailing semicolon in the output.
    ///
    /// @param s source element to print
    public void printFragment(SolidityProgramElement s) {
        layouter.beginRelativeC(0);
        markStart(s);
        s.visit(this);
        markEnd(s);
        layouter.end();
    }

    protected void printUnaryOperator(String symbol, boolean prefix, SolidityProgramElement child) {
        if (prefix) {
            layouter.print(symbol);
            child.visit(this);
        } else {
            child.visit(this);
            layouter.print(symbol);
        }
    }

    /// Marks the start of the first executable statement ...
    ///
    /// @param stmt current statement;
    protected void markStart(Object stmt) {
        if (!startAlreadyMarked) {
            layouter.markStartFirstStatement();
            firstStatement = stmt;
            startAlreadyMarked = true;
        }
    }

    /// Marks the end of the first executable statement ...
    protected void markEnd(Object stmt) {
        if (!endAlreadyMarked && (firstStatement == stmt)) {
            layouter.markEndFirstStatement();
            endAlreadyMarked = true;
        }
    }

    /// Replace all unicode characters above ? by their explicit representation.
    ///
    /// @param str the input string.
    /// @return the encoded string.
    protected static String encodeUnicodeChars(String str) {
        int len = str.length();
        StringBuilder buf = new StringBuilder(len + 4);
        for (int i = 0; i < len; i += 1) {
            char c = str.charAt(i);
            if (c >= 0x0100) {
                if (c < 0x1000) {
                    buf.append("\\u0").append(Integer.toString(c, 16));
                } else {
                    buf.append("\\u").append(Integer.toString(c, 16));
                }
            } else {
                buf.append(c);
            }
        }
        return buf.toString();
    }

    /// Write separated list.
    ///
    /// @param list a program element list.
    protected void writeSeparatedList(ImmutableArray<? extends SolidityProgramElement> list,
            String sep) {
        for (int i = 0; i < list.size(); i++) {
            if (i != 0) {
                layouter.print(sep).brk();
            }
            list.get(i).visit(this);
        }
    }

    /// Write comma list.
    ///
    /// @param list a program element list.
    protected void writeCommaList(ImmutableArray<? extends SolidityProgramElement> list) {
        writeSeparatedList(list, ",");
    }

    @Override
    public void performActionOnBinaryExpression(BinaryExpression x) {
        Expression left = x.getLeft();
        Expression right = x.getRight();

        maybeParens(left, x.getOperator().precedence());

        layouter.brk();

        x.getOperator().visit(this);

        layouter.brk();

        maybeParens(right, x.getOperator().precedence());
    }

    @Override
    public void performActionOnOperator(Operator x) {
        layouter.print(x.symbol());
    }

    @Override
    public void performActionOnUnaryExpression(UnaryExpression x) {
        final Operator operator = x.getOperator();
        if (operator.isPrefix()) {
            layouter.print(operator.symbol());
        }
        maybeParens(x.getExp(), operator.precedence());
        if (operator.isPostfix()) {
            layouter.print(operator.symbol());
        }
    }

    private void maybeParens(Expression exp, int precedence) {
        boolean closeParens = false;
        if (exp instanceof BinaryExpression bexp &&
                bexp.getOperator().precedence() <= precedence) {
            layouter.print("(");
            closeParens = true;
        }
        exp.visit(this);
        if (closeParens) {
            layouter.print(")");
        }
    }

    @Override
    public void performActionOnTernaryExpression(TernaryExpression x) {
        maybeParens(x.getCondition(), x.getPrecedence());
        layouter.brk().print("?").brk();
        maybeParens(x.getTrueExpression(), x.getPrecedence());
        layouter.brk().print(":").brk();
        maybeParens(x.getFalseExpression(), x.getPrecedence());
    }

    @Override
    public void performActionOnContractReference(ContractReference x) {
        layouter.print(x.getType().name().toString());
    }

    @Override
    public void performActionOnEnumReference(EnumReference x) {
        layouter.print(
            "PRETTY PRINTING OF " + x.getClass() + " PROGRAM ELEMENTS NOT YET IMPLEMENTED.");
    }

    @Override
    public void performActionOnFunctionReference(FunctionReference x) {
        layouter.print(
            "PRETTY PRINTING OF " + x.getClass() + " PROGRAM ELEMENTS NOT YET IMPLEMENTED.");
    }

    @Override
    public void performActionOnModifierReference(ModifierReference x) {
        layouter.print(
            "PRETTY PRINTING OF " + x.getClass() + " PROGRAM ELEMENTS NOT YET IMPLEMENTED.");
    }

    @Override
    public void performActionOnTypeReference(TypeReference x) {
        layouter.print(
            "PRETTY PRINTING OF " + x.getClass() + " PROGRAM ELEMENTS NOT YET IMPLEMENTED.");
    }

    @Override
    public void performActionOnUnresolvedReferenceException(UnresolvedReferenceException x) {

    }

    private void beginBlock() {
        layouter.print("{");
        layouter.beginRelativeC();
    }

    private void endBlock() {
        layouter.end().nl().print("}");
    }

    @Override
    public void performActionOnBlock(Block x) {
        if (x.getChildCount() == 0) {
            markStart(x);
            layouter.print("{}");
            markEnd(x);
        } else {
            beginBlock();
            for (Statement stmt : x.getStatements()) {
                layouter.nl();
                stmt.visit(this);
            }
            endBlock();
        }
    }

    @Override
    public void performActionOnCatchClause(CatchClause catchClause) {

    }

    @Override
    public void performActionOnBoolLiteral(BoolLiteral x) {
        layouter.keyWord(x.getValue() ? "true" : "false");
    }

    @Override
    public void performActionOnUint256Literal(Uint256Literal x) {
        layouter.print(x.getValue().toString());
    }

    @Override
    public void performActionOnContextStatementBlock(ContextStatementBlock x) {
        if (x.getStatements().isEmpty()) {
            layouter.print("{c# #c}");
        } else {
            layouter.beginRelativeC();
            layouter.print("{ c#");
            for (Statement stmt : x.getStatements()) {
                layouter.nl();
                stmt.visit(this);
            }
            layouter.end().nl();
            layouter.print("#c }");
        }
    }

    @Override
    public void performActionOnBreakStatement(BreakStatement x) {
        layouter.print("break;");
    }

    @Override
    public void performActionOnConditionStatement(ConditionStatement x) {
        layouter.print("if");
        layouter.print("(");
        x.getCondition().visit(this);
        layouter.print(")");
        layouter.beginRelativeC().brk(1);
        x.getThenBody().visit(this);
        layouter.end();
        if (x.getElseBody() != null) {
            layouter.print("else");
            layouter.beginRelativeC().brk(1);
            x.getElseBody().visit(this);
            layouter.end();
        }
    }

    @Override
    public void performActionOnContinueStatement(ContinueStatement x) {
        layouter.print("continue;");
    }

    @Override
    public void performActionOnDeclarationStatement(DeclarationStatement x) {
        layouter.print(
            "PRETTY PRINTING OF " + x.getClass() + " PROGRAM ELEMENTS NOT YET IMPLEMENTED.");
    }

    @Override
    public void performActionOnExpressionStatement(ExpressionStatement x) {
        x.getExpression().visit(this);
        layouter.print(";");
    }

    @Override
    public void performActionOnForStatement(ForStatement x) {
        layouter.print(
            "PRETTY PRINTING OF " + x.getClass() + " PROGRAM ELEMENTS NOT YET IMPLEMENTED.");
    }

    @Override
    public void performActionOnForInit(ForInit x) {
        layouter.print(
            "PRETTY PRINTING OF " + x.getClass() + " PROGRAM ELEMENTS NOT YET IMPLEMENTED.");
    }

    @Override
    public void performActionOnForUpdate(ForUpdate x) {
        layouter.print(
            "PRETTY PRINTING OF " + x.getClass() + " PROGRAM ELEMENTS NOT YET IMPLEMENTED.");
    }


    @Override
    public void performActionOnDoWhileStatement(DoWhileStatement x) {
        layouter.print("do");
        layouter.beginRelativeC().brk(1);
        x.getBody().visit(this);
        layouter.end();
        layouter.print("while").brk();
        layouter.print("(");
        x.getCondition().visit(this);
        layouter.print(")");
    }

    @Override
    public void performActionOnWhileStatement(WhileStatement x) {
        layouter.print("while").brk();
        layouter.print("(");
        x.getCondition().visit(this);
        layouter.print(")");
        layouter.beginRelativeC().brk(1);
        x.getBody().visit(this);
        layouter.end();
    }


    @Override
    public void performActionOnPlaceholdStatement(PlaceholdStatement x) {
        layouter.print("_").print(";");
    }

    @Override
    public void performActionOnReturnStatment(ReturnStatement x) {
        layouter.print("return").brk();
        if (x.getChildCount() > 0) {
            x.getReturnExp().visit(this);
        }
        layouter.print(";");
    }

    @Override
    public void performActionOnTryStatement(TryStatement x) {
        layouter.print(
            "PRETTY PRINTING OF " + x.getClass() + " PROGRAM ELEMENTS NOT YET IMPLEMENTED.");
    }


    @Override
    public void performActionOnDataLocation(DataLocation x) {
        layouter.print(x.getLabel());
    }

    @Override
    public void performActionOnSchemaVariable(SchemaVariable x) {
        if (!(x instanceof ProgramSV)) {
            throw new UnsupportedOperationException(
                "Don't know how to pretty print non program SV in programs.");
        }

        Object o = instantiations.getInstantiation(x);
        if (o == null) {
            layouter.print("s#" + x.name());
        } else {
            if (o instanceof SolidityProgramElement pe) {
                pe.visit(this);
            } else if (o instanceof ImmutableArray) {
                // noinspection unchecked
                for (SolidityProgramElement e : (ImmutableArray<SolidityProgramElement>) o) {
                    e.visit(this);
                }
            } else {
                LOGGER.warn("No PrettyPrinting available for {}", o.getClass().getName());
            }
        }
    }

    @Override
    public void performActionOnProgramMetaConstruct(ProgramTransformer programTransformer) {
        layouter.print(programTransformer.toString());
    }

    @Override
    public void performActionOnStatementVariableDeclaration(StatementVariableDeclaration x) {
        layouter.print(
            "PRETTY PRINTING OF " + x.getClass() + " PROGRAM ELEMENTS NOT YET IMPLEMENTED.");
    }

    @Override
    public void performActionOnElementaryExpression(ElementaryExpression x) {
        layouter.print(x.getType().name().toString());
    }

    @Override
    public void performActionOnFunctionCallExpression(FunctionCallExpression x) {
        x.functionExp.visit(this);
        layouter.print("(");
        for (int i = 0; i < x.getArguments().size(); i++) {
            final Expression arg = x.getArguments().get(i);
            arg.visit(this);
            if (i < x.getArguments().size() - 1) {
                layouter.print(",").brk();
            }
        }
        layouter.print(")");
    }

    @Override
    public void performActionOnIndexExpression(IndexExpression x) {
        layouter.print(
            "PRETTY PRINTING OF " + x.getClass() + " PROGRAM ELEMENTS NOT YET IMPLEMENTED.");
    }

    @Override
    public void performActionOnIndexRangeExpression(IndexRangeExpression x) {
        layouter.print(
            "PRETTY PRINTING OF " + x.getClass() + " PROGRAM ELEMENTS NOT YET IMPLEMENTED.");
    }

    @Override
    public void performActionOnMemberExp(MemberExp x) {
        layouter.print(
            "PRETTY PRINTING OF " + x.getClass() + " PROGRAM ELEMENTS NOT YET IMPLEMENTED.");
    }

    @Override
    public void performActionOnTupleExpression(TupleExpression x) {
        layouter.print(
            "PRETTY PRINTING OF " + x.getClass() + " PROGRAM ELEMENTS NOT YET IMPLEMENTED.");
    }

    @Override
    public void performActionOnNewExpression(NewExpression x) {
        layouter.print(
            "PRETTY PRINTING OF " + x.getClass() + " PROGRAM ELEMENTS NOT YET IMPLEMENTED.");
    }

    @Override
    public void performActionOnUnresolvedTypeException(UnresolvedTypeException x) {
        layouter.print(
            "PRETTY PRINTING OF " + x.getClass() + " PROGRAM ELEMENTS NOT YET IMPLEMENTED.");
    }

    @Override
    public void performActionOnProgramVariable(ProgramVariable x) {
        layouter.print(x.name().toString());
    }


    private void beginMultilineParen() {
        layouter.print("(").beginRelativeC(0).beginRelativeC().brk(0);
    }

    private void endMultilineParen() {
        layouter.end().brk(0).end();
        layouter.print(")");
    }

    private void printArguments(ImmutableArray<? extends Expression> args) {
        beginMultilineParen();
        if (args != null) {
            writeCommaList(args);
        }
        endMultilineParen();
    }


}
