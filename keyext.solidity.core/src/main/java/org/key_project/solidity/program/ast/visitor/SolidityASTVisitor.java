/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.visitor;

import org.key_project.logic.op.sv.SchemaVariable;
import org.key_project.solidity.common.Services;
import org.key_project.solidity.logic.op.ProgramVariable;
import org.key_project.solidity.program.ast.SolidityProgramElement;
import org.key_project.solidity.program.ast.declarations.*;
import org.key_project.solidity.program.ast.declarations.FunctionEnums.DataLocation;
import org.key_project.solidity.program.ast.expressions.*;
import org.key_project.solidity.program.ast.expressions.literals.*;
import org.key_project.solidity.program.ast.expressions.operators.*;
import org.key_project.solidity.program.ast.references.*;
import org.key_project.solidity.program.ast.statement.*;
import org.key_project.solidity.program.ext.ContextStatementBlock;
import org.key_project.solidity.rule.metaconstruct.ProgramTransformer;

public abstract class SolidityASTVisitor extends SolidityASTWalker implements Visitor {
    protected final Services services;

    public SolidityASTVisitor(SolidityProgramElement root, Services services) {
        super(root);
        this.services = services;
    }

    /// the action that is performed just before leaving the node the last time
    @Override
    protected void doAction(SolidityProgramElement node) {
        node.visit(this);
    }

    @Override
    protected void walk(SolidityProgramElement node) {
        super.walk(node);
    }

    protected abstract void doDefaultAction(SolidityProgramElement node);

    @Override
    public void performActionOnDataLocation(DataLocation x) {
        doDefaultAction(x);
    }

    @Override
    public void performActionOnProgramVariable(ProgramVariable x) {
        doDefaultAction(x);
    }

    @Override
    public void performActionOnStatementVariableDeclaration(StatementVariableDeclaration x) {
        doDefaultAction(x);
    }

    @Override
    public void performActionOnElementaryExpression(ElementaryExpression x) {
        doDefaultAction(x);
    }

    @Override
    public void performActionOnFunctionCallExpression(FunctionCallExpression x) {
        doDefaultAction(x);
    }

    @Override
    public void performActionOnIndexExpression(IndexExpression x) {
        doDefaultAction(x);
    }

    @Override
    public void performActionOnIndexRangeExpression(IndexRangeExpression x) {
        doDefaultAction(x);
    }

    @Override
    public void performActionOnMemberExp(MemberExp x) {
        doDefaultAction(x);
    }

    @Override
    public void performActionOnTupleExpression(TupleExpression x) {
        doDefaultAction(x);
    }

    @Override
    public void performActionOnNewExpression(NewExpression x) {
        doDefaultAction(x);
    }

    @Override
    public void performActionOnUnresolvedTypeException(UnresolvedTypeException x) {
        doDefaultAction(x);
    }

    @Override
    public void performActionOnBoolLiteral(BoolLiteral x) {
        doDefaultAction(x);
    }

    @Override
    public void performActionOnUint256Literal(Uint256Literal x) {
        doDefaultAction(x);
    }

    @Override
    public void performActionOnUnaryExpression(UnaryExpression x) {
        doDefaultAction(x);
    }

    @Override
    public void performActionOnAssignExpression(AssignExpression x) {
        doDefaultAction(x);
    }

    @Override
    public void performActionOnBinaryExpression(BinaryExpression x) {
        doDefaultAction(x);
    }

    @Override
    public void performActionOnOperator(Operator x) {
        doDefaultAction(x);
    }

    @Override
    public void performActionOnTernaryExpression(TernaryExpression x) {
        doDefaultAction(x);
    }

    @Override
    public void performActionOnContractReference(ContractReference x) {
        doDefaultAction(x);
    }

    @Override
    public void performActionOnEnumReference(EnumReference x) {
        doDefaultAction(x);
    }

    @Override
    public void performActionOnFieldReference(FieldReference x) {
        doDefaultAction(x);
    }

    @Override
    public void performActionOnFunctionReference(FunctionReference x) {
        doDefaultAction(x);
    }

    @Override
    public void performActionOnModifierReference(ModifierReference x) {
        doDefaultAction(x);
    }

    @Override
    public void performActionOnTypeReference(TypeReference x) {
        doDefaultAction(x);
    }

    @Override
    public void performActionOnUnresolvedReferenceException(UnresolvedReferenceException x) {
        doDefaultAction(x);
    }

    @Override
    public void performActionOnBlock(Block x) {
        doDefaultAction(x);
    }

    @Override
    public void performActionOnCatchClause(CatchClause x) {
        doDefaultAction(x);
    }

    @Override
    public void performActionOnContextStatementBlock(ContextStatementBlock x) {
        doDefaultAction(x);
    }

    @Override
    public void performActionOnBreakStatement(BreakStatement x) {
        doDefaultAction(x);
    }

    @Override
    public void performActionOnConditionStatement(ConditionStatement x) {
        doDefaultAction(x);
    }

    @Override
    public void performActionOnContinueStatement(ContinueStatement x) {
        doDefaultAction(x);
    }

    @Override
    public void performActionOnDeclarationStatement(DeclarationStatement x) {
        doDefaultAction(x);
    }

    @Override
    public void performActionOnDoWhileStatement(DoWhileStatement x) {
        doDefaultAction(x);
    }

    @Override
    public void performActionOnExpressionStatement(ExpressionStatement x) {
        doDefaultAction(x);
    }

    @Override
    public void performActionOnForStatement(ForStatement x) {
        doDefaultAction(x);
    }

    @Override
    public void performActionOnForInit(ForInit x) {
        doDefaultAction(x);
    }

    @Override
    public void performActionOnForUpdate(ForUpdate x) {
        doDefaultAction(x);
    }

    @Override
    public void performActionOnPlaceholdStatement(PlaceholdStatement x) {
        doDefaultAction(x);
    }

    @Override
    public void performActionOnFunctionBodyStatement(FunctionBodyStatement x) {
        doDefaultAction(x);
    }

    @Override
    public void performActionOnReturnStatment(ReturnStatement x) {
        doDefaultAction(x);
    }

    @Override
    public void performActionOnTryStatement(TryStatement x) {
        doDefaultAction(x);
    }

    @Override
    public void performActionOnWhileStatement(WhileStatement x) {
        doDefaultAction(x);
    }

    @Override
    public void performActionOnSchemaVariable(SchemaVariable x) {
        doDefaultAction((SolidityProgramElement) x);
    }

    @Override
    public void performActionOnProgramMetaConstruct(ProgramTransformer x) {
        doDefaultAction(x);
    }


}
