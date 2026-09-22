/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.visitor;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;
import java.util.function.Function;

import org.key_project.solidity.common.Services;
import org.key_project.solidity.program.ast.SolidityProgramElement;
import org.key_project.solidity.program.ast.declarations.*;
import org.key_project.solidity.program.ast.declarations.FunctionEnums.DataLocation;
import org.key_project.solidity.program.ast.expressions.*;
import org.key_project.solidity.program.ast.expressions.literals.*;
import org.key_project.solidity.program.ast.expressions.operators.*;
import org.key_project.solidity.program.ast.references.*;
import org.key_project.solidity.program.ast.statement.*;
import org.key_project.solidity.program.ext.ContextStatementBlock;
import org.key_project.util.ExtList;

import org.jspecify.annotations.Nullable;

public class CreatingASTVisitor extends SolidityASTVisitor {
    protected static final Boolean CHANGED = Boolean.TRUE;
    protected final Deque<ExtList> stack = new ArrayDeque<>();

    public CreatingASTVisitor(SolidityProgramElement root, Services services) {
        super(root, services);
    }

    @Override
    protected void walk(SolidityProgramElement node) {
        stack.push(new ExtList());
        super.walk(node);
    }

    @Override
    public String toString() {
        return getTop().toString();
    }

    @Override
    protected void doDefaultAction(SolidityProgramElement x) {
        addChild(x);
    }

    protected ExtList getTop() {
        return Objects.requireNonNull(stack.peek());
    }

    protected void addToTopOfStack(@Nullable SolidityProgramElement x) {
        if (x != null) {
            ExtList list = getTop();
            list.add(x);
        }
    }

    protected void addChild(@Nullable SolidityProgramElement x) {
        stack.pop();
        addToTopOfStack(x);
    }

    protected void changed() {
        ExtList list = getTop();
        if (list.isEmpty() || list.getFirst() != CHANGED) {
            list.addFirst(CHANGED);
        }
    }

    /// Pops the change list of `x` from the stack; if any child changed, replaces `x` by the
    /// element the factory creates from the change list, otherwise keeps `x` unchanged.
    protected void rebuild(SolidityProgramElement x,
            Function<ExtList, SolidityProgramElement> factory) {
        ExtList changeList = getTop();
        if (!changeList.isEmpty() && changeList.getFirst() == CHANGED) {
            changeList.removeFirst();
            addChild(factory.apply(changeList));
            changed();
        } else {
            doDefaultAction(x);
        }
    }

    @Override
    public void performActionOnDataLocation(DataLocation x) {
        rebuild(x, changeList -> x);
    }

    @Override
    public void performActionOnStatementVariableDeclaration(StatementVariableDeclaration x) {
        rebuild(x, StatementVariableDeclaration::new);
    }

    @Override
    public void performActionOnElementaryExpression(ElementaryExpression x) {
        rebuild(x, ElementaryExpression::new);
    }

    @Override
    public void performActionOnFunctionCallExpression(FunctionCallExpression x) {
        rebuild(x, changeList -> new FunctionCallExpression(changeList, x.getType()));
    }

    @Override
    public void performActionOnIndexExpression(IndexExpression x) {
        rebuild(x, changeList -> new IndexExpression(changeList, x.getType()));
    }

    @Override
    public void performActionOnIndexRangeExpression(IndexRangeExpression x) {
        rebuild(x, changeList -> new IndexRangeExpression(changeList, x.getType()));
    }

    @Override
    public void performActionOnMemberExp(MemberExp x) {
        rebuild(x, changeList -> {
            changeList.add(x.getType());
            return new MemberExp(changeList);
        });
    }

    @Override
    public void performActionOnTupleExpression(TupleExpression x) {
        rebuild(x, TupleExpression::new);
    }

    @Override
    public void performActionOnNewExpression(NewExpression x) {
        rebuild(x, changeList -> new NewExpression(x.getType()));
    }

    @Override
    public void performActionOnUnresolvedTypeException(UnresolvedTypeException x) {
        rebuild(x, UnresolvedTypeException::new);
    }

    @Override
    public void performActionOnBoolLiteral(BoolLiteral x) {
        rebuild(x, BoolLiteral::new);
    }

    @Override
    public void performActionOnUint256Literal(Uint256Literal x) {
        rebuild(x, Uint256Literal::new);
    }

    @Override
    public void performActionOnAssignExpression(AssignExpression x) {
        rebuild(x, AssignExpression::new);
    }

    @Override
    public void performActionOnBinaryExpression(BinaryExpression x) {
        rebuild(x, BinaryExpression::new);
    }

    @Override
    public void performActionOnOperator(Operator x) {
        doDefaultAction(x);
    }

    @Override
    public void performActionOnUnaryExpression(UnaryExpression x) {
        rebuild(x, UnaryExpression::new);
    }

    @Override
    public void performActionOnTernaryExpression(TernaryExpression x) {
        rebuild(x, changeList -> new TernaryExpression(changeList, x.getType()));
    }

    @Override
    public void performActionOnContractReference(ContractReference x) {
        rebuild(x,
            changeList -> new ContractReference(x.getContractDeclaration(), x.getType(), x.id));
    }

    @Override
    public void performActionOnEnumReference(EnumReference x) {
        rebuild(x, changeList -> new EnumReference(changeList, x.getType()));
    }

    @Override
    public void performActionOnFieldReference(FieldReference x) {
        rebuild(x, changeList -> new FieldReference(changeList, x.getType()));
    }

    @Override
    public void performActionOnFunctionReference(FunctionReference x) {
        rebuild(x, changeList -> new FunctionReference(x.getReferencedDeclaration(), x.getType()));
    }

    @Override
    public void performActionOnModifierReference(ModifierReference x) {
        rebuild(x, changeList -> new ModifierReference(x.name));
    }

    @Override
    public void performActionOnTypeReference(TypeReference x) {
        rebuild(x, changeList -> new TypeReference(x.getReferencedType()));
    }

    @Override
    public void performActionOnUnresolvedReferenceException(UnresolvedReferenceException x) {
        rebuild(x, changeList -> new UnresolvedReferenceException());
    }

    @Override
    public void performActionOnBlock(Block x) {
        rebuild(x, Block::new);
    }

    @Override
    public void performActionOnContextStatementBlock(ContextStatementBlock x) {
        rebuild(x, ContextStatementBlock::new);
    }

    @Override
    public void performActionOnCatchClause(CatchClause x) {
        rebuild(x, CatchClause::new);
    }

    @Override
    public void performActionOnBreakStatement(BreakStatement x) {
        rebuild(x, changeList -> new BreakStatement());
    }

    @Override
    public void performActionOnConditionStatement(ConditionStatement x) {
        rebuild(x, ConditionStatement::new);
    }

    @Override
    public void performActionOnContinueStatement(ContinueStatement x) {
        rebuild(x, changeList -> new ContinueStatement());
    }

    @Override
    public void performActionOnDeclarationStatement(DeclarationStatement x) {
        rebuild(x, DeclarationStatement::new);
    }

    @Override
    public void performActionOnDoWhileStatement(DoWhileStatement x) {
        rebuild(x, DoWhileStatement::new);
    }

    @Override
    public void performActionOnExpressionStatement(ExpressionStatement x) {
        rebuild(x, ExpressionStatement::new);
    }

    @Override
    public void performActionOnForStatement(ForStatement x) {
        rebuild(x, ForStatement::new);
    }

    @Override
    public void performActionOnForInit(ForInit x) {
        rebuild(x, ForInit::new);
    }

    @Override
    public void performActionOnForUpdate(ForUpdate x) {
        rebuild(x, ForUpdate::new);
    }

    @Override
    public void performActionOnPlaceholdStatement(PlaceholdStatement x) {
        rebuild(x, changeList -> new PlaceholdStatement());
    }

    @Override
    public void performActionOnReturnStatement(ReturnStatement x) {
        rebuild(x, ReturnStatement::new);
    }

    @Override
    public void performActionOnTryStatement(TryStatement x) {
        rebuild(x, TryStatement::new);
    }

    @Override
    public void performActionOnWhileStatement(WhileStatement x) {
        rebuild(x, WhileStatement::new);
    }
}
