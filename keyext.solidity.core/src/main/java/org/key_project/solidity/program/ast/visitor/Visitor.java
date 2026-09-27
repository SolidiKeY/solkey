/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.visitor;

import org.key_project.logic.op.sv.SchemaVariable;
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

public interface Visitor {
    default void performActionOnDefault(SolidityProgramElement x) {}

    default void performActionOnProgramVariable(ProgramVariable x) {
        performActionOnDefault(x);
    }

    default void performActionOnSchemaVariable(SchemaVariable x) {}

    default void performActionOnProgramMetaConstruct(ProgramTransformer programTransformer) {
        performActionOnDefault(programTransformer);
    }

    default void performActionOnStatementVariableDeclaration(StatementVariableDeclaration x) {
        performActionOnDefault(x);
    }

    default void performActionOnFieldDeclaration(FieldDeclaration x) {
        performActionOnDefault(x);
    }

    default void performActionOnFunctionDeclaration(FunctionDeclaration x) {
        performActionOnDefault(x);
    }

    default void performActionOnElementaryExpression(ElementaryExpression x) {
        performActionOnDefault(x);
    }

    default void performActionOnFunctionCallExpression(FunctionCallExpression x) {
        performActionOnDefault(x);
    }

    default void performActionOnIndexExpression(IndexExpression x) {
        performActionOnDefault(x);
    }

    default void performActionOnIndexRangeExpression(IndexRangeExpression x) {
        performActionOnDefault(x);
    }

    default void performActionOnMemberExp(MemberExp x) {
        performActionOnDefault(x);
    }

    default void performActionOnTupleExpression(TupleExpression x) {
        performActionOnDefault(x);
    }

    default void performActionOnNewExpression(NewExpression x) {
        performActionOnDefault(x);
    }

    default void performActionOnUnresolvedTypeException(UnresolvedTypeException x) {
        performActionOnDefault(x);
    }

    default void performActionOnBoolLiteral(BoolLiteral x) {
        performActionOnDefault(x);
    }

    default void performActionOnUint256Literal(Uint256Literal x) {
        performActionOnDefault(x);
    }

    default void performActionOnTernaryExpression(TernaryExpression x) {
        performActionOnDefault(x);
    }

    default void performActionOnContractReference(ContractReference x) {
        performActionOnDefault(x);
    }

    default void performActionOnEnumReference(EnumReference x) {
        performActionOnDefault(x);
    }

    default void performActionOnFieldReference(FieldReference x) {
        performActionOnDefault(x);
    }

    default void performActionOnFunctionReference(FunctionReference x) {
        performActionOnDefault(x);
    }

    default void performActionOnModifierReference(ModifierReference x) {
        performActionOnDefault(x);
    }

    default void performActionOnTypeReference(TypeReference x) {
        performActionOnDefault(x);
    }

    default void performActionOnUnresolvedReferenceException(UnresolvedReferenceException x) {
        performActionOnDefault(x);
    }

    default void performActionOnBlock(Block x) {
        performActionOnDefault(x);
    }

    default void performActionOnCatchClause(CatchClause catchClause) {
        performActionOnDefault(catchClause);
    }

    default void performActionOnContextStatementBlock(ContextStatementBlock x) {
        performActionOnDefault(x);
    }

    default void performActionOnBreakStatement(BreakStatement x) {
        performActionOnDefault(x);
    }

    default void performActionOnConditionStatement(ConditionStatement x) {
        performActionOnDefault(x);
    }

    default void performActionOnContinueStatement(ContinueStatement x) {
        performActionOnDefault(x);
    }

    default void performActionOnDeclarationStatement(DeclarationStatement x) {
        performActionOnDefault(x);
    }

    default void performActionOnDoWhileStatement(DoWhileStatement x) {
        performActionOnDefault(x);
    }

    default void performActionOnExpressionStatement(ExpressionStatement x) {
        performActionOnDefault(x);
    }

    default void performActionOnForStatement(ForStatement x) {
        performActionOnDefault(x);
    }

    default void performActionOnForInit(ForInit x) {
        performActionOnDefault(x);
    }

    default void performActionOnForUpdate(ForUpdate x) {
        performActionOnDefault(x);
    }

    default void performActionOnPlaceholdStatement(PlaceholdStatement x) {
        performActionOnDefault(x);
    }

    default void performActionOnFunctionBodyStatement(FunctionBodyStatement x) {
        performActionOnDefault(x);
    }

    default void performActionOnReturnStatement(ReturnStatement x) {
        performActionOnDefault(x);
    }

    default void performActionOnTryStatement(TryStatement x) {
        performActionOnDefault(x);
    }

    default void performActionOnWhileStatement(WhileStatement x) {
        performActionOnDefault(x);
    }

    default void performActionOnDataLocation(DataLocation x) {
        performActionOnDefault(x);
    }

    default void performActionOnAssignExpression(AssignExpression x) {
        performActionOnDefault(x);
    }

    default void performActionOnBinaryExpression(BinaryExpression x) {
        performActionOnDefault(x);
    }

    default void performActionOnOperator(Operator x) {
        performActionOnDefault(x);
    }

    default void performActionOnUnaryExpression(UnaryExpression x) {
        performActionOnDefault(x);
    }
}
