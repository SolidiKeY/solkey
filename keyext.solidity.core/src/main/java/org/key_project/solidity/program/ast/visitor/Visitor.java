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
    default void performActionOnProgramVariable(ProgramVariable x) {}

    default void performActionOnSchemaVariable(SchemaVariable x) {}

    default void performActionOnProgramMetaConstruct(ProgramTransformer programTransformer) {}

    default void performActionOnStatementVariableDeclaration(StatementVariableDeclaration x) {}

    default void performActionOnFieldDeclaration(FieldDeclaration x) {}

    default void performActionOnFunctionDeclaration(FunctionDeclaration x) {}

    default void performActionOnElementaryExpression(ElementaryExpression x) {}

    default void performActionOnFunctionCallExpression(FunctionCallExpression x) {}

    default void performActionOnIndexExpression(IndexExpression x) {}

    default void performActionOnIndexRangeExpression(IndexRangeExpression x) {}

    default void performActionOnMemberExp(MemberExp x) {}

    default void performActionOnTupleExpression(TupleExpression x) {}

    default void performActionOnNewExpression(NewExpression x) {}

    default void performActionOnUnresolvedTypeException(UnresolvedTypeException x) {}

    default void performActionOnBoolLiteral(BoolLiteral x) {}

    default void performActionOnUint256Literal(Uint256Literal x) {}

    default void performActionOnTernaryExpression(TernaryExpression x) {}

    default void performActionOnContractReference(ContractReference x) {}

    default void performActionOnEnumReference(EnumReference x) {}

    default void performActionOnFieldReference(FieldReference x) {}

    default void performActionOnFunctionReference(FunctionReference x) {}

    default void performActionOnModifierReference(ModifierReference x) {}

    default void performActionOnTypeReference(TypeReference x) {}

    default void performActionOnUnresolvedReferenceException(UnresolvedReferenceException x) {}

    default void performActionOnBlock(Block x) {}

    default void performActionOnCatchClause(CatchClause catchClause) {}

    default void performActionOnContextStatementBlock(ContextStatementBlock x) {}

    default void performActionOnBreakStatement(BreakStatement x) {}

    default void performActionOnConditionStatement(ConditionStatement x) {}

    default void performActionOnContinueStatement(ContinueStatement x) {}

    default void performActionOnDeclarationStatement(DeclarationStatement x) {}

    default void performActionOnDoWhileStatement(DoWhileStatement x) {}

    default void performActionOnExpressionStatement(ExpressionStatement x) {}

    default void performActionOnForStatement(ForStatement x) {}

    default void performActionOnForInit(ForInit x) {}

    default void performActionOnForUpdate(ForUpdate x) {}

    default void performActionOnPlaceholdStatement(PlaceholdStatement x) {}

    default void performActionOnFunctionBodyStatement(FunctionBodyStatement x) {}

    default void performActionOnReturnStatement(ReturnStatement x) {}

    default void performActionOnTryStatement(TryStatement x) {}

    default void performActionOnWhileStatement(WhileStatement x) {}

    default void performActionOnDataLocation(DataLocation x) {}

    default void performActionOnAssignExpression(AssignExpression x) {}

    default void performActionOnBinaryExpression(BinaryExpression x) {}

    default void performActionOnOperator(Operator x) {}

    default void performActionOnUnaryExpression(UnaryExpression x) {}
}
