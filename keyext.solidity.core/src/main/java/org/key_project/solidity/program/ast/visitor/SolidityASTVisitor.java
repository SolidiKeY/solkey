/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.visitor;

import org.key_project.logic.op.sv.SchemaVariable;
import org.key_project.solidity.common.Services;
import org.key_project.solidity.logic.op.ProgramVariable;
import org.key_project.solidity.program.ast.SolidityProgramElement;
import org.key_project.solidity.program.ast.abstractions.*;
import org.key_project.solidity.program.ast.declarations.*;
import org.key_project.solidity.program.ast.declarations.FunctionEnums.DataLocation;
import org.key_project.solidity.program.ast.expressions.*;
import org.key_project.solidity.program.ast.expressions.literals.*;
import org.key_project.solidity.program.ast.expressions.operators.*;
import org.key_project.solidity.program.ast.references.*;
import org.key_project.solidity.program.ast.statement.*;
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
        super.walk(node);
    }

    @Override
    protected void walk(SolidityProgramElement node) {
        super.walk(node);
        if (services != null) {
        }
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
    public void performActionOnArrayType(ArrayType x) {
        doDefaultAction(x);
    }

    @Override
    public void performActionOnEnumType(EnumType x) {
        doDefaultAction(x);
    }

    @Override
    public void performActionOnKeYSolidityType(KeYSolidityType x) {
        doDefaultAction(x);
    }

    @Override
    public void performActionOnMappingType(MappingType x) {
        doDefaultAction(x);
    }

    @Override
    public void performActionOnPrimitiveType(PrimitiveType x) {
        doDefaultAction(x);
    }

    @Override
    public void performActionOnArrayDeclaration(ArrayDeclaration x) {
        doDefaultAction(x);
    }

    @Override
    public void performActionOnContractDeclaration(ContractDeclaration x) {
        doDefaultAction(x);
    }

    @Override
    public void performActionOnEnumDeclaration(EnumDeclaration x) {
        doDefaultAction(x);
    }

    @Override
    public void performActionOnFieldDeclaration(FieldDeclaration x) {
        doDefaultAction(x);
    }

    @Override
    public void performActionOnFunctionDeclaration(FunctionDeclaration x) {
        doDefaultAction(x);
    }

    @Override
    public void performActionOnMemberEnumDeclaration(MemberEnumDeclaration x) {
        doDefaultAction(x);
    }

    @Override
    public void performActionOnModifierDeclaration(ModifierDeclaration x) {
        doDefaultAction(x);
    }

    @Override
    public void performActionOnParameterDeclaration(ParameterDeclaration x) {
        doDefaultAction(x);
    }

    @Override
    public void performActionOnStateVariableDeclaration(StateVariableDeclaration x) {
        doDefaultAction(x);
    }

    @Override
    public void performActionOnStatementVariableDeclaration(StatementVariableDeclaration x) {
        doDefaultAction(x);
    }

    @Override
    public void performActionOnStructDeclaration(StructDeclaration x) {
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
    public void performActionOnAddOperator(AddOperator x) {
        doDefaultAction(x);
    }

    @Override
    public void performActionOnAndEqualOperator(AndEqualOperator x) {
        doDefaultAction(x);
    }

    @Override
    public void performActionOnAndOperator(AndOperator x) {
        doDefaultAction(x);
    }

    @Override
    public void performActionOnAssignmentExpression(AssignmentExpression x) {
        doDefaultAction(x);
    }

    @Override
    public void performActionOnBitwiseAndOperator(BitwiseAndOperator x) {
        doDefaultAction(x);
    }

    @Override
    public void performActionOnBitwiseEqualOperator(BitwiseEqualOperator x) {
        doDefaultAction(x);
    }

    @Override
    public void performActionOnBitwiseNotOperator(BitwiseNotOperator x) {
        doDefaultAction(x);
    }

    @Override
    public void performActionOnBitwiseOrOperator(BitwiseOrOperator x) {
        doDefaultAction(x);
    }

    @Override
    public void performActionOnDeleteOperator(DeleteOperator x) {
        doDefaultAction(x);
    }

    @Override
    public void performActionOnDivOperator(DivOperator x) {
        doDefaultAction(x);
    }

    @Override
    public void performActionOnDivisionEqualOperator(DivisionEqualOperator x) {
        doDefaultAction(x);
    }

    @Override
    public void performActionOnEqualOperator(EqualOperator x) {
        doDefaultAction(x);
    }

    @Override
    public void performActionOnExponentialOperator(ExponentialOperator x) {
        doDefaultAction(x);
    }

    @Override
    public void performActionOnGreaterEqualOperator(GreaterEqualOperator x) {
        doDefaultAction(x);
    }

    @Override
    public void performActionOnGreaterOperator(GreaterOperator x) {
        doDefaultAction(x);
    }

    @Override
    public void performActionOnLeftShiftEqualOperator(LeftShiftEqualOperator x) {
        doDefaultAction(x);
    }

    @Override
    public void performActionOnLeftShiftOperator(LeftShiftOperator x) {
        doDefaultAction(x);
    }

    @Override
    public void performActionOnLessEqualOperator(LessEqualOperator x) {
        doDefaultAction(x);
    }

    @Override
    public void performActionOnLessOperator(LessOperator x) {
        doDefaultAction(x);
    }

    @Override
    public void performActionOnLogicalRightShiftEqualOperator(LogicalRightShiftEqualOperator x) {
        doDefaultAction(x);
    }

    @Override
    public void performActionOnLogicalRightShiftOperator(LogicalRightShiftOperator x) {
        doDefaultAction(x);
    }

    @Override
    public void performActionOnMinusEqualOperator(MinusEqualOperator x) {
        doDefaultAction(x);
    }

    @Override
    public void performActionOnMinusMinusOperator(MinusMinusOperator x) {
        doDefaultAction(x);
    }

    @Override
    public void performActionOnModEqualOperator(ModEqualOperator x) {
        doDefaultAction(x);
    }

    @Override
    public void performActionOnModOperator(ModOperator x) {
        doDefaultAction(x);
    }

    @Override
    public void performActionOnMultiplicationEqualOperator(MultiplicationEqualOperator x) {
        doDefaultAction(x);
    }

    @Override
    public void performActionOnMultiplicationOperator(MultiplicationOperator x) {
        doDefaultAction(x);
    }

    @Override
    public void performActionOnNegateOperator(NegateOperator x) {
        doDefaultAction(x);
    }

    @Override
    public void performActionOnNotOperator(NotOperator x) {
        doDefaultAction(x);
    }

    @Override
    public void performActionOnOrEqualOperator(OrEqualOperator x) {
        doDefaultAction(x);
    }

    @Override
    public void performActionOnOrOperator(OrOperator x) {
        doDefaultAction(x);
    }

    @Override
    public void performActionOnPlusEqualOperator(PlusEqualOperator x) {
        doDefaultAction(x);
    }

    @Override
    public void performActionOnPlusPlusOperator(PlusPlusOperator x) {
        doDefaultAction(x);
    }

    @Override
    public void performActionOnRightShiftEqualOperator(RightShiftEqualOperator x) {
        doDefaultAction(x);
    }

    @Override
    public void performActionOnRightShiftOperator(RightShiftOperator x) {
        doDefaultAction(x);
    }

    @Override
    public void performActionOnSubtractionOperator(SubtractionOperator x) {
        doDefaultAction(x);
    }

    @Override
    public void performActionOnTernaryOperator(TernaryOperator x) {
        doDefaultAction(x);
    }

    @Override
    public void performActionOnUnequalOperator(UnequalOperator x) {
        doDefaultAction(x);
    }

    @Override
    public void performActionOnXorEqualOperator(XorEqualOperator x) {
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
    public void performActionOnFunctionReference(FunctionReference x) {
        doDefaultAction(x);
    }

    @Override
    public void performActionOnModifierReference(ModifierReference x) {
        doDefaultAction(x);
    }

    @Override
    public void performActionOnParameterVariableReference(ParameterVariableReference x) {
        doDefaultAction(x);
    }

    @Override
    public void performActionOnStateVariableReference(StateVariableReference x) {
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
    public void performActionOnPlaceholdStatement(PlaceholdStatement x) {
        doDefaultAction(x);
    }

    @Override
    public void performActionOnReturnStatment(ReturnStatment x) {
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
