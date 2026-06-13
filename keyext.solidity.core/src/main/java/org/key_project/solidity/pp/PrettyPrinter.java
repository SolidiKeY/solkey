/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.pp;

import org.key_project.logic.op.sv.SchemaVariable;
import org.key_project.solidity.common.Services;
import org.key_project.solidity.logic.op.ProgramVariable;
import org.key_project.solidity.program.ast.SolidityProgramElement;
import org.key_project.solidity.program.ast.declarations.FunctionDeclaration;
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

        layouter.print(" ");

        x.getOperator().visit(this);

        layouter.print(" ");

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
        layouter.print(" ").print("?").brk();
        maybeParens(x.getTrueExpression(), x.getPrecedence());
        layouter.print(" ").print(":").brk();
        maybeParens(x.getFalseExpression(), x.getPrecedence());
    }

    @Override
    public void performActionOnContractReference(ContractReference x) {
        layouter.print(x.getType().name().toString());
    }

    @Override
    public void performActionOnEnumReference(EnumReference x) {
        layouter.print(x.mainProgramElement().name().toString());
    }

    @Override
    public void performActionOnFunctionReference(FunctionReference x) {
        if (x.referencedDeclaration != null) {
            layouter.print(x.referencedDeclaration.name().toString());
        } else {
            // Not yet resolved; fall back to the numeric reference.
            layouter.print("fn#" + x.id);
        }
    }

    @Override
    public void performActionOnModifierReference(ModifierReference x) {
        layouter.print(x.name);
    }

    @Override
    public void performActionOnTypeReference(TypeReference x) {
        layouter.print(x.getTypeName().toString());
    }

    @Override
    public void performActionOnUnresolvedReferenceException(UnresolvedReferenceException x) {
        layouter.print("<unresolved: " + x.getMessage() + ">");
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
    public void performActionOnCatchClause(CatchClause x) {
        layouter.keyWord("catch");
        // Derive the catch kind from the declared variable's type the same way the
        // CatchClause constructor does (its Kind enum is not visible here):
        // uint -> catch Panic(uint code)
        // string-> catch Error(string memory reason)
        // bytes -> catch (bytes memory data) (low-level)
        // none -> catch
        StatementVariableDeclaration decl = null;
        try {
            decl = x.getCatchDeclaration();
        } catch (RuntimeException ignored) {
            // no declaration -> catch-all
        }
        if (decl != null) {
            var type = decl.getProgramVariable().getType();
            String typeName = type != null ? type.toString() : "";
            if ("uint".equals(typeName) || "uint256".equals(typeName)) {
                layouter.print(" ").print("Panic");
            } else if ("string".equals(typeName)) {
                layouter.print(" ").print("Error");
            }
            layouter.print("(");
            decl.visit(this);
            layouter.print(")");
        }
        layouter.print(" ");
        x.getBody().visit(this);
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
        markStart(x);
        layouter.keyWord("break");
        layouter.print(";");
        markEnd(x);
    }

    @Override
    public void performActionOnConditionStatement(ConditionStatement x) {
        markStart(x);
        layouter.keyWord("if").print(" ");
        layouter.print("(");
        x.getCondition().visit(this);
        layouter.print(")");
        layouter.beginRelativeC().brk(1);
        x.getThenBody().visit(this);
        layouter.end();
        if (x.getElseBody() != null) {
            layouter.brk(1);
            layouter.keyWord("else");
            layouter.beginRelativeC().brk(1);
            x.getElseBody().visit(this);
            layouter.end();
        }
        markEnd(x);
    }

    @Override
    public void performActionOnContinueStatement(ContinueStatement x) {
        markStart(x);
        layouter.keyWord("continue");
        layouter.print(";");
        markEnd(x);
    }

    @Override
    public void performActionOnDeclarationStatement(DeclarationStatement x) {
        markStart(x);
        var decls = x.getDeclarations();
        boolean tuple = decls.size() > 1;
        if (tuple) {
            layouter.print("(");
        }
        for (int i = 0; i < decls.size(); i++) {
            if (i != 0) {
                layouter.print(",").brk();
            }
            if (decls.get(i) instanceof SolidityProgramElement pe) {
                pe.visit(this);
            } else {
                layouter.print(decls.get(i).toString());
            }
        }
        if (tuple) {
            layouter.print(")");
        }
        if (x.getInitialValue() != null) {
            layouter.print(" = ");
            x.getInitialValue().visit(this);
        }
        layouter.print(";");
        markEnd(x);
    }

    @Override
    public void performActionOnExpressionStatement(ExpressionStatement x) {
        markStart(x);
        x.getExpression().visit(this);
        layouter.print(";");
        markEnd(x);
    }

    @Override
    public void performActionOnForStatement(ForStatement x) {
        markStart(x);
        layouter.keyWord("for").print(" ");
        layouter.print("(");
        if (x.getInit() != null) {
            x.getInit().visit(this);
        }
        layouter.print(";");
        if (x.getCondition() != null) {
            layouter.brk();
            x.getCondition().visit(this);
        }
        layouter.print(";");
        if (x.getUpdate() != null) {
            layouter.brk();
            x.getUpdate().visit(this);
        }
        layouter.print(")");
        layouter.beginRelativeC().brk(1);
        x.getBody().visit(this);
        layouter.end();
        markEnd(x);
    }

    @Override
    public void performActionOnForInit(ForInit x) {
        x.getInit().visit(this);
    }

    @Override
    public void performActionOnForUpdate(ForUpdate x) {
        x.getUpdate().visit(this);
    }


    @Override
    public void performActionOnDoWhileStatement(DoWhileStatement x) {
        markStart(x);
        layouter.keyWord("do");
        layouter.beginRelativeC().brk(1);
        x.getBody().visit(this);
        layouter.end();
        layouter.brk(1);
        layouter.keyWord("while").print(" ");
        layouter.print("(");
        x.getCondition().visit(this);
        layouter.print(")").print(";");
        markEnd(x);
    }

    @Override
    public void performActionOnWhileStatement(WhileStatement x) {
        markStart(x);
        layouter.keyWord("while").print(" ");
        layouter.print("(");
        x.getCondition().visit(this);
        layouter.print(")");
        layouter.beginRelativeC().brk(1);
        x.getBody().visit(this);
        layouter.end();
        markEnd(x);
    }


    @Override
    public void performActionOnPlaceholdStatement(PlaceholdStatement x) {
        markStart(x);
        layouter.print("_").print(";");
        markEnd(x);
    }

    @Override
    public void performActionOnReturnStatment(ReturnStatement x) {
        markStart(x);
        layouter.keyWord("return");
        if (x.getChildCount() > 0) {
            layouter.print(" ");
            x.getReturnExp().visit(this);
        }
        layouter.print(";");
        markEnd(x);
    }

    @Override
    public void performActionOnTryStatement(TryStatement x) {
        markStart(x);
        layouter.keyWord("try").print(" ");
        x.getExpression().visit(this);
        if (x.getReturnCount() > 0) {
            layouter.print(" ");
            layouter.keyWord("returns");
            layouter.print(" ").print("(");
            var rets = x.getReturnDeclaration();
            for (int i = 0; i < rets.size(); i++) {
                if (i != 0) {
                    layouter.print(",").brk();
                }
                rets.get(i).visit(this);
            }
            layouter.print(")");
        }
        layouter.print(" ");
        x.getBody().visit(this);
        for (CatchClause cc : x.getCatchClauses()) {
            layouter.print(" ");
            cc.visit(this);
        }
        markEnd(x);
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
        var pv = x.getProgramVariable();
        layouter.print(pv.getType().toString()).print(" ");
        var loc = pv.getDataLocation();
        if (loc != null && loc != DataLocation.Default) {
            loc.visit(this);
            layouter.print(" ");
        }
        pv.visit(this);
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
        x.getLeftExp().visit(this);
        layouter.print("[");
        x.getIndexExp().visit(this);
        layouter.print("]");
    }

    @Override
    public void performActionOnIndexRangeExpression(IndexRangeExpression x) {
        x.getBaseExp().visit(this);
        layouter.print("[");
        if (x.getStartExp() != null) {
            x.getStartExp().visit(this);
        }
        layouter.print(":");
        if (x.getEndExp() != null) {
            x.getEndExp().visit(this);
        }
        layouter.print("]");
    }

    @Override
    public void performActionOnMemberExp(MemberExp x) {
        x.getLeftExp().visit(this);
        layouter.print(".");
        var right = x.getRightExp();
        if (right instanceof FunctionDeclaration fd) {
            layouter.print(fd.name().toString());
        } else if (right instanceof SolidityProgramElement pe) {
            pe.visit(this);
        } else {
            layouter.print(String.valueOf(right));
        }
    }

    @Override
    public void performActionOnTupleExpression(TupleExpression x) {
        layouter.print("(");
        for (int i = 0; i < x.getChildCount(); i++) {
            if (i != 0) {
                layouter.print(",").brk();
            }
            x.getExpression(i).visit(this);
        }
        layouter.print(")");
    }

    @Override
    public void performActionOnNewExpression(NewExpression x) {
        layouter.keyWord("new").print(" ");
        layouter.print(x.getType().name().toString());
    }

    @Override
    public void performActionOnUnresolvedTypeException(UnresolvedTypeException x) {
        layouter.print("<unresolved type: " + x.getMessage() + ">");
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
