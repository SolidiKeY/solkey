/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.expressions.operators;

import org.key_project.logic.SyntaxElement;
import org.key_project.solidity.program.ast.SolidityProgramElement;
import org.key_project.solidity.program.ast.visitor.Visitor;

import static org.key_project.solidity.program.ast.expressions.operators.Operator.OperatorPos.POSTFIX;
import static org.key_project.solidity.program.ast.expressions.operators.Operator.OperatorPos.PREFIX;

public enum Operator implements SolidityProgramElement {
    POST_INC("++", 15, 0, POSTFIX),
    POST_DEC("--", 15, 0, POSTFIX),
    PRE_INC("++", 14, 0, PREFIX),
    PRE_DEC("--", 14, 0, PREFIX),
    DELETE("delete", 14, 0, PREFIX),
    UNARY_MINUS("-", 14, 0, PREFIX),
    LOGICAL_NOT("!", 14, 0, PREFIX),
    BITWISE_NOT("~", 14, 0, PREFIX),
    EXPO("**", 13, 0),
    MULT("*", 12, 0),
    DIV("/", 12, 0),
    MOD("%", 12, 0),
    ADD("+", 11, 0),
    SUB("-", 11, 0),
    BITWISE_SHIFT_RIGHT(">>", 10, 0),
    BITWISE_SHIFT_LEFT("<<", 10, 0),
    BITWISE_AND("&", 9, 0),
    BITWISE_XOR("^", 8, 0),
    BITWISE_OR("|", 7, 0),
    LESS_THAN("<", 6, 0),
    GREATER_THAN(">", 6, 0),
    LESS_EQUAL("<=", 6, 0),
    GREATER_EQUAL(">=", 6, 0),
    EQUAL("==", 5, 0),
    NOT_EQUAL("!=", 5, 0),
    LOGICAL_AND("&&", 4, 0),
    LOGICAL_OR("||", 3, 0),
    COPY_ASSIGN("=", 2, 0),
    OR_ASSIGN("|=", 2, 0),
    XOR_ASSIGN("^=", 2, 0),
    AND_ASSIGN("&=", 2, 0),
    BITWISE_SHIFT_LEFT_ASSIGN("<<=", 2, 0),
    BITWISE_SHIFT_RIGHT_ASSIGN(">>=", 2, 0),
    ADD_ASSIGN("+=", 2, 0),
    SUB_ASSIGN("-=", 2, 0),
    MULT_ASSIGN("*=", 2, 0),
    DIV_ASSIGN("/=", 2, 0),
    MOD_ASSIGN("%=", 2, 0);

    public enum OperatorPos {
        PREFIX, INFIX, POSTFIX;
    }


    public static boolean isAssignment(OperatorExpression e) {
        return switch (e.getOperator()) {
            case POST_INC, POST_DEC, PRE_INC, PRE_DEC, COPY_ASSIGN, OR_ASSIGN, XOR_ASSIGN,
                    AND_ASSIGN,
                    BITWISE_SHIFT_LEFT_ASSIGN, BITWISE_SHIFT_RIGHT_ASSIGN, ADD_ASSIGN, SUB_ASSIGN,
                    MULT_ASSIGN, DIV_ASSIGN, MOD_ASSIGN ->
                true;
            default -> false;
        };
    }

    public static boolean isAssignmentOperator(Operator op) {
        return switch (op) {
            case COPY_ASSIGN, OR_ASSIGN, XOR_ASSIGN, AND_ASSIGN,
                    BITWISE_SHIFT_LEFT_ASSIGN, BITWISE_SHIFT_RIGHT_ASSIGN,
                    ADD_ASSIGN, SUB_ASSIGN, MULT_ASSIGN, DIV_ASSIGN, MOD_ASSIGN ->
                true;
            default -> false;
        };
    }

    private final String symbol;
    private final int precedence;
    private final int associativity;
    private final OperatorPos operatorPos;

    Operator(String symbol, int precedence, int assoc, OperatorPos operatorPos) {
        this.symbol = symbol;
        this.precedence = precedence;
        this.associativity = assoc;
        this.operatorPos = operatorPos;
    }

    Operator(String symbol, int precedence, int assoc) {
        this(symbol, precedence, assoc, OperatorPos.INFIX);
    }


    @Override
    public SyntaxElement getChild(int n) {
        throw new IndexOutOfBoundsException("Operators have no children");
    }

    @Override
    public int getChildCount() {
        return 0;
    }

    @Override
    public void visit(Visitor v) {
        v.performActionOnOperator(this);
    }

    public String symbol() {
        return symbol;
    }

    public int precedence() {
        return precedence;
    }

    public int associativity() {
        return associativity;
    }

    public boolean isPrefix() {
        return operatorPos == OperatorPos.PREFIX;
    }

    public boolean isPostfix() {
        return operatorPos == OperatorPos.POSTFIX;
    }

    public boolean isInfix() {
        return operatorPos == OperatorPos.INFIX;
    }



}
