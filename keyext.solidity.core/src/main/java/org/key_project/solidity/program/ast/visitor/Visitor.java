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

    void performActionOnStatementVariableDeclaration(StatementVariableDeclaration x);

    void performActionOnElementaryExpression(ElementaryExpression x);

    void performActionOnFunctionCallExpression(FunctionCallExpression x);

    void performActionOnIndexExpression(IndexExpression x);

    void performActionOnIndexRangeExpression(IndexRangeExpression x);

    void performActionOnMemberExp(MemberExp x);

    void performActionOnTupleExpression(TupleExpression x);

    void performActionOnNewExpression(NewExpression x);

    void performActionOnUnresolvedTypeException(UnresolvedTypeException x);

    void performActionOnBoolLiteral(BoolLiteral x);

    void performActionOnUint256Literal(Uint256Literal x);

    void performActionOnTernaryExpression(TernaryExpression x);

    void performActionOnContractReference(ContractReference x);

    void performActionOnEnumReference(EnumReference x);

    void performActionOnFunctionReference(FunctionReference x);

    void performActionOnModifierReference(ModifierReference x);

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

    void performActionOnForInit(ForInit x);

    void performActionOnForUpdate(ForUpdate x);

    void performActionOnPlaceholdStatement(PlaceholdStatement x);

    void performActionOnFunctionBodyStatement(FunctionBodyStatement x);

    void performActionOnReturnStatment(ReturnStatement x);

    void performActionOnTryStatement(TryStatement x);

    void performActionOnWhileStatement(WhileStatement x);

    void performActionOnDataLocation(DataLocation x);

    void performActionOnAssignExpression(AssignExpression x);

    void performActionOnBinaryExpression(BinaryExpression x);

    void performActionOnOperator(Operator x);

    void performActionOnUnaryExpression(UnaryExpression x);
}
