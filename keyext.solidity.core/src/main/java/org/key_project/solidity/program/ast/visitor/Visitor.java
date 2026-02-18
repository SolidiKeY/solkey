/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.visitor;

import org.key_project.logic.op.sv.SchemaVariable;
import org.key_project.solidity.logic.op.ProgramVariable;
import org.key_project.solidity.program.ast.declarations.*;
import org.key_project.solidity.program.ast.declarations.FunctionEnums.DataLocation;
import org.key_project.solidity.program.ast.expressions.*;
import org.key_project.solidity.program.ast.expressions.literals.*;
import org.key_project.solidity.program.ast.expressions.operators.*;
import org.key_project.solidity.program.ast.references.*;
import org.key_project.solidity.program.ast.statement.*;
import org.key_project.solidity.program.ext.ContextStatementBlock;
import org.key_project.solidity.rule.metaconstruct.ProgramTransformer;

public interface Visitor {
    void performActionOnProgramVariable(ProgramVariable x);

    void performActionOnSchemaVariable(SchemaVariable x);

    void performActionOnProgramMetaConstruct(ProgramTransformer programTransformer);

    void performActionOnArrayDeclaration(ArrayDeclaration x);

    void performActionOnStatementVariableDeclaration(StatementVariableDeclaration x);

    void performActionOnElementaryExpression(ElementaryExpression x);

    void performActionOnFunctionCallExpression(FunctionCallExpression x);

    void performActionOnIndexExpression(IndexExpression x);

    void performActionOnIndexRangeExpression(IndexRangeExpression x);

    void performActionOnMemberExp(MemberExp x);

    void performActionOnTupleExpression(TupleExpression x);

    void performActionOnUnresolvedTypeException(UnresolvedTypeException x);

    void performActionOnBoolLiteral(BoolLiteral x);

    void performActionOnUint256Literal(Uint256Literal x);

    void performActionOnAddOperator(AddOperator x);

    void performActionOnAndEqualOperator(AndEqualOperator x);

    void performActionOnAndOperator(AndOperator x);

    void performActionOnAssignmentExpression(AssignmentExpression x);

    void performActionOnBitwiseAndOperator(BitwiseAndOperator x);

    void performActionOnBitwiseEqualOperator(BitwiseEqualOperator x);

    void performActionOnBitwiseNotOperator(BitwiseNotOperator x);

    void performActionOnBitwiseOrOperator(BitwiseOrOperator x);

    void performActionOnDeleteOperator(DeleteOperator x);

    void performActionOnDivOperator(DivOperator x);

    void performActionOnDivisionEqualOperator(DivisionEqualOperator x);

    void performActionOnEqualOperator(EqualOperator x);

    void performActionOnExponentialOperator(ExponentialOperator x);

    void performActionOnGreaterEqualOperator(GreaterEqualOperator x);

    void performActionOnGreaterOperator(GreaterOperator x);

    void performActionOnLeftShiftEqualOperator(LeftShiftEqualOperator x);

    void performActionOnLeftShiftOperator(LeftShiftOperator x);

    void performActionOnLessEqualOperator(LessEqualOperator x);

    void performActionOnLessOperator(LessOperator x);

    void performActionOnLogicalRightShiftEqualOperator(LogicalRightShiftEqualOperator x);

    void performActionOnLogicalRightShiftOperator(LogicalRightShiftOperator x);

    void performActionOnMinusEqualOperator(MinusEqualOperator x);

    void performActionOnMinusMinusOperator(MinusMinusOperator x);

    void performActionOnModEqualOperator(ModEqualOperator x);

    void performActionOnModOperator(ModOperator x);

    void performActionOnMultiplicationEqualOperator(MultiplicationEqualOperator x);

    void performActionOnMultiplicationOperator(MultiplicationOperator x);

    void performActionOnNegateOperator(NegateOperator x);

    void performActionOnNotOperator(NotOperator x);

    void performActionOnOrEqualOperator(OrEqualOperator x);

    void performActionOnOrOperator(OrOperator x);

    void performActionOnPlusEqualOperator(PlusEqualOperator x);

    void performActionOnPlusPlusOperator(PlusPlusOperator x);

    void performActionOnRightShiftEqualOperator(RightShiftEqualOperator x);

    void performActionOnRightShiftOperator(RightShiftOperator x);

    void performActionOnSubtractionOperator(SubtractionOperator x);

    void performActionOnTernaryOperator(TernaryOperator x);

    void performActionOnUnequalOperator(UnequalOperator x);

    void performActionOnXorEqualOperator(XorEqualOperator x);

    void performActionOnContractReference(ContractReference x);

    void performActionOnEnumReference(EnumReference x);

    void performActionOnFunctionReference(FunctionReference x);

    void performActionOnModifierReference(ModifierReference x);

    void performActionOnParameterVariableReference(ParameterVariableReference x);

    void performActionOnStateVariableReference(StateVariableReference x);

    void performActionOnTypeReference(TypeReference x);

    void performActionOnUnresolvedReferenceException(UnresolvedReferenceException x);

    void performActionOnBlock(Block x);

    void performActionOnCatchClause(CatchClause catchClause);

    void performActionOnContextStatementBlock(ContextStatementBlock x);

    void performActionOnBreakStatement(BreakStatement x);

    void performActionOnConditionStatement(ConditionStatement x);

    void performActionOnContinueStatement(ContinueStatement x);

    void performActionOnDeclarationStatement(DeclarationStatement x);

    void performActionOnDoWhileStatement(DoWhileStatement x);

    void performActionOnExpressionStatement(ExpressionStatement x);

    void performActionOnForStatement(ForStatement x);

    void performActionOnPlaceholdStatement(PlaceholdStatement x);

    void performActionOnReturnStatment(ReturnStatment x);

    void performActionOnTryStatement(TryStatement x);

    void performActionOnWhileStatement(WhileStatement x);

    void performActionOnDataLocation(DataLocation x);

}
