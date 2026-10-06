/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.rule.metaconstruct;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.key_project.logic.Name;
import org.key_project.solidity.common.Services;
import org.key_project.solidity.logic.op.ProgramVariable;
import org.key_project.solidity.program.ast.abstractions.KeYSolidityType;
import org.key_project.solidity.program.ast.declarations.FunctionEnums.DataLocation;
import org.key_project.solidity.program.ast.declarations.StatementVariableDeclaration;
import org.key_project.solidity.program.ast.expressions.Expression;
import org.key_project.solidity.program.ast.expressions.literals.BoolLiteral;
import org.key_project.solidity.program.ast.expressions.operators.AssignExpression;
import org.key_project.solidity.program.ast.expressions.operators.BinaryExpression;
import org.key_project.solidity.program.ast.expressions.operators.Operator;
import org.key_project.solidity.program.ast.expressions.operators.UnaryExpression;
import org.key_project.solidity.program.ast.statement.Block;
import org.key_project.solidity.program.ast.statement.BreakStatement;
import org.key_project.solidity.program.ast.statement.ConditionStatement;
import org.key_project.solidity.program.ast.statement.ContinueStatement;
import org.key_project.solidity.program.ast.statement.DeclarationStatement;
import org.key_project.solidity.program.ast.statement.DoWhileStatement;
import org.key_project.solidity.program.ast.statement.ExpressionStatement;
import org.key_project.solidity.program.ast.statement.ForInit;
import org.key_project.solidity.program.ast.statement.ForStatement;
import org.key_project.solidity.program.ast.statement.ForUpdate;
import org.key_project.solidity.program.ast.statement.LoopStatement;
import org.key_project.solidity.program.ast.statement.ReturnStatement;
import org.key_project.solidity.program.ast.statement.Statement;
import org.key_project.solidity.program.ast.statement.TryStatement;
import org.key_project.solidity.program.ast.statement.WhileStatement;

import org.jspecify.annotations.Nullable;

final class LoopLowering {

    private enum Abrupt {
        BREAK, CONTINUE, RETURN
    }

    private record Lowered(Statement statement, Set<Abrupt> abrupt) {
    }

    private static final class ReturnFlag {
        private @Nullable ProgramVariable variable;
    }

    private final class Flags {
        private final ReturnFlag ret;
        private @Nullable ProgramVariable brk;
        private @Nullable ProgramVariable cnt;

        private Flags(ReturnFlag ret) {
            this.ret = ret;
        }

        private ProgramVariable of(Abrupt kind) {
            return switch (kind) {
                case BREAK -> brk = brk == null ? fresh("brk") : brk;
                case CONTINUE -> cnt = cnt == null ? fresh("cnt") : cnt;
                case RETURN -> ret.variable = ret.variable == null ? fresh("ret") : ret.variable;
            };
        }
    }

    private final Services services;

    private LoopLowering(Services services) {
        this.services = services;
    }

    static Block lower(Block body, Services services) {
        return new Block(new LoopLowering(services).lowerList(statements(body), null).stmts);
    }

    private record LoweredList(List<Statement> stmts, Set<Abrupt> abrupt) {
    }

    private LoweredList lowerList(List<Statement> stmts, @Nullable Flags flags) {
        List<Statement> out = new ArrayList<>(stmts.size());
        Set<Abrupt> abrupt = EnumSet.noneOf(Abrupt.class);
        for (int i = 0; i < stmts.size(); i++) {
            Statement statement = stmts.get(i);
            Lowered lowered = lowerStatement(statement, flags);
            out.add(lowered.statement());
            abrupt.addAll(lowered.abrupt());
            if (lowered.abrupt().isEmpty()) {
                continue;
            }
            if (!isJump(statement)) {
                LoweredList rest = lowerList(stmts.subList(i + 1, stmts.size()), flags);
                if (!rest.stmts().isEmpty()) {
                    out.add(new ConditionStatement(
                        notAny(lowered.abrupt(), Objects.requireNonNull(flags)),
                        new Block(rest.stmts())));
                }
                abrupt.addAll(rest.abrupt());
            }
            break;
        }
        return new LoweredList(out, abrupt);
    }

    private static boolean isJump(Statement statement) {
        return statement instanceof BreakStatement || statement instanceof ContinueStatement
                || statement instanceof ReturnStatement;
    }

    private Lowered lowerStatement(Statement statement, @Nullable Flags flags) {
        if (statement instanceof LoopStatement loop) {
            return lowerLoop(loop, flags);
        }
        if (flags == null) {
            return switch (statement) {
                case Block block ->
                    new Lowered(new Block(lowerList(statements(block), null).stmts()),
                        Set.of());
                case ConditionStatement cond -> lowerCondition(cond, null);
                default -> new Lowered(statement, Set.of());
            };
        }
        return switch (statement) {
            case BreakStatement ignored -> jump(Abrupt.BREAK, flags);
            case ContinueStatement ignored -> jump(Abrupt.CONTINUE, flags);
            case ReturnStatement ignored -> jump(Abrupt.RETURN, flags);
            case Block block -> {
                LoweredList list = lowerList(statements(block), flags);
                yield new Lowered(new Block(list.stmts()), list.abrupt());
            }
            case ConditionStatement cond -> lowerCondition(cond, flags);
            case TryStatement ignored when containsJump(statement) ->
                throw new IllegalStateException(
                    "break, continue or return inside try inside a loop is not supported");
            default -> new Lowered(statement, Set.of());
        };
    }

    private Lowered jump(Abrupt kind, Flags flags) {
        return new Lowered(assign(flags.of(kind), BoolLiteral.TRUE), EnumSet.of(kind));
    }

    private Lowered lowerCondition(ConditionStatement cond, @Nullable Flags flags) {
        Lowered then = lowerStatement(cond.getThenBody(), flags);
        Statement elseBody = cond.getElseBody();
        if (elseBody == null) {
            return new Lowered(new ConditionStatement(cond.getCondition(), then.statement()),
                then.abrupt());
        }
        Lowered otherwise = lowerStatement(elseBody, flags);
        Set<Abrupt> abrupt = EnumSet.noneOf(Abrupt.class);
        abrupt.addAll(then.abrupt());
        abrupt.addAll(otherwise.abrupt());
        return new Lowered(new ConditionStatement(cond.getCondition(), then.statement(),
            otherwise.statement()), abrupt);
    }

    private Lowered lowerLoop(LoopStatement loop, @Nullable Flags outer) {
        Flags flags = new Flags(outer == null ? new ReturnFlag() : outer.ret);
        LoweredList body = lowerList(statements(loop.getBody()), flags);
        Set<Abrupt> exits = EnumSet.noneOf(Abrupt.class);
        for (Abrupt kind : body.abrupt()) {
            if (kind != Abrupt.CONTINUE) {
                exits.add(kind);
            }
        }

        List<Statement> before = new ArrayList<>();
        List<Statement> iteration = new ArrayList<>();
        Expression condition = Objects.requireNonNullElse(loop.getCondition(), BoolLiteral.TRUE);

        if (loop instanceof ForStatement forLoop) {
            ForInit init = forLoop.getInit();
            if (init != null) {
                before.add(init.getInit());
            }
        } else if (loop instanceof DoWhileStatement) {
            ProgramVariable first = fresh("first");
            before.add(declare(first, BoolLiteral.TRUE));
            iteration.add(assign(first, BoolLiteral.FALSE));
            condition = new BinaryExpression(Operator.LOGICAL_OR, first, condition);
        }
        if (flags.cnt != null) {
            iteration.add(declare(flags.cnt, BoolLiteral.FALSE));
        }
        iteration.addAll(body.stmts());
        if (loop instanceof ForStatement forLoop && forLoop.getUpdate() != null) {
            ForUpdate update = Objects.requireNonNull(forLoop.getUpdate());
            Statement step = new ExpressionStatement(update.getUpdate());
            iteration.add(exits.isEmpty() ? step
                    : new ConditionStatement(notAny(exits, flags), step));
        }
        if (!exits.isEmpty()) {
            condition = new BinaryExpression(Operator.LOGICAL_AND, notAny(exits, flags), condition);
        }
        if (flags.brk != null) {
            before.add(declare(flags.brk, BoolLiteral.FALSE));
        }
        boolean returns = exits.contains(Abrupt.RETURN);
        if (returns && outer == null) {
            before.add(declare(flags.of(Abrupt.RETURN), BoolLiteral.FALSE));
        }

        Statement whileLoop = new WhileStatement(condition, new Block(iteration), loop.getSpec());
        if (before.isEmpty() && !(returns && outer == null)) {
            return new Lowered(whileLoop, returns ? EnumSet.of(Abrupt.RETURN) : Set.of());
        }
        List<Statement> block = new ArrayList<>(before);
        block.add(whileLoop);
        if (returns && outer == null) {
            block.add(new ConditionStatement(flags.of(Abrupt.RETURN),
                new ReturnStatement((Expression) null)));
            return new Lowered(new Block(block), Set.of());
        }
        return new Lowered(new Block(block), returns ? EnumSet.of(Abrupt.RETURN) : Set.of());
    }

    private static Expression notAny(Set<Abrupt> kinds, Flags flags) {
        Expression guard = null;
        for (Abrupt kind : kinds) {
            Expression not = new UnaryExpression(Operator.LOGICAL_NOT, flags.of(kind));
            guard = guard == null ? not : new BinaryExpression(Operator.LOGICAL_AND, guard, not);
        }
        return Objects.requireNonNull(guard);
    }

    private static boolean containsJump(org.key_project.logic.SyntaxElement element) {
        if (element instanceof BreakStatement || element instanceof ContinueStatement
                || element instanceof ReturnStatement) {
            return true;
        }
        for (int i = 0; i < element.getChildCount(); i++) {
            if (containsJump(element.getChild(i))) {
                return true;
            }
        }
        return false;
    }

    private static List<Statement> statements(Statement statement) {
        return statement instanceof Block block ? block.getStatements().toList()
                : List.of(statement);
    }

    private ProgramVariable fresh(String name) {
        KeYSolidityType boolType = Objects.requireNonNull(
            services.getSolidityInfo().getKeYSolidityType("bool"), "no bool type");
        return new ProgramVariable(new Name(name), boolType, DataLocation.Default);
    }

    private static Statement declare(ProgramVariable variable, Expression value) {
        return new DeclarationStatement(List.of(new StatementVariableDeclaration(variable)), value);
    }

    private static Statement assign(ProgramVariable variable, Expression value) {
        return new ExpressionStatement(
            new AssignExpression(Operator.COPY_ASSIGN, variable, value));
    }
}
