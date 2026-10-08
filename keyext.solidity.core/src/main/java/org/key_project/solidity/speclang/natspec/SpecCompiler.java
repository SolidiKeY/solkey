/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.speclang.natspec;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.key_project.solidity.keyfile.Key;
import org.key_project.solidity.keyfile.KeyFormula;
import org.key_project.solidity.keyfile.KeyFormula.Compare.Relation;
import org.key_project.solidity.keyfile.KeyPrinter;
import org.key_project.solidity.keyfile.KeyTerm;
import org.key_project.solidity.keyfile.KeyTerm.Arithmetic.Operator;
import org.key_project.solidity.parser.SolSpecBaseVisitor;
import org.key_project.solidity.parser.SolSpecParser;
import org.key_project.solidity.program.parser.ParserUtils;
import org.key_project.solidity.program.parser.SolidityOutline;
import org.key_project.solidity.theory.StructLDT;

import org.jspecify.annotations.Nullable;

/// Compiles a specification expression into a [KeyFormula] over the `storage` and
/// `net` structs, the way the hand-written problems in `keyext.solidity.examples/net/` spell it:
/// a state variable `x` is `find<[int]>(S, cons1(C$x))`, a mapping entry `m[k]` is
/// `find<[int]>(S, cons2(C$m, at(k)))`, `net(a)` is `selectSt<[int]>(N, at(a))`, `msg.sender`
/// and `msg.value` are the `msgSender` and `msgValue` program variables, `address(this)` is
/// `self`.
///
/// A visitor over the `SolSpec.g4` parse tree; each visit yields a [Value], either a term with
/// its type or a formula.
public final class SpecCompiler extends SolSpecBaseVisitor<SpecCompiler.Value> {

    /// Where the expression is evaluated: which struct terms stand for storage and the ledger,
    /// whether `\old` and `\result` are meaningful, and the variables in scope.
    public record Context(String storage, String net, boolean ensures,
            Map<String, SpecType> locals) {

        public static Context invariant() {
            return new Context("s", "n", false, Map.of());
        }

        public static Context requires(Map<String, SpecType> parameters) {
            return new Context("storage", "net", false, parameters);
        }

        public static Context ensures(Map<String, SpecType> parameters) {
            return new Context("storage", "net", true, parameters);
        }

        Context old() {
            return new Context("old", "oldNet", false, locals);
        }

        Context bind(String variable, SpecType type) {
            Map<String, SpecType> extended = new HashMap<>(locals);
            extended.put(variable, type);
            return new Context(storage, net, ensures, extended);
        }
    }

    public sealed interface Value {
        SpecType type();

        record OfTerm(KeyTerm term, SpecType type) implements Value {
        }

        record OfFormula(KeyFormula formula) implements Value {
            @Override
            public SpecType type() {
                return SpecType.BOOL;
            }
        }
    }

    private record Path(List<KeyTerm> segments, SpecType type) {
    }

    private final SolidityOutline.Contract contract;
    private final SolidityOutline.Function function;
    private final @Nullable SpecType resultType;
    private Context ctx = Context.invariant();

    public SpecCompiler(SolidityOutline.Contract contract, SolidityOutline.Function function) {
        this.contract = contract;
        this.function = function;
        this.resultType =
            function.returns().size() == 1 && function.returns().get(0).keySort() != null
                    ? SpecType.of(function.returns().get(0).type())
                    : null;
    }

    public static String resultVariable(SolidityOutline.Function function, String name) {
        return function.returns().size() == 1 ? "result" : "result_" + name;
    }

    public static Map<String, SpecType> parameterTypes(SolidityOutline.Function function) {
        Map<String, SpecType> types = new HashMap<>();
        for (SolidityOutline.Parameter parameter : function.parameters()) {
            if (parameter.keySort() != null) {
                types.put(parameter.name(), SpecType.of(parameter.type()));
            }
        }
        return types;
    }

    /// The formula for `text`; `where` names the clause in error messages.
    public KeyFormula formula(String text, Context context, String where) {
        try {
            ctx = context;
            return asFormula(visit(SpecParser.parse(text)));
        } catch (SpecException e) {
            throw new SpecException(where + ": " + e.getMessage());
        }
    }

    /// The integer term for `text`; `where` names the clause in error messages.
    public KeyTerm term(String text, Context context, String where) {
        try {
            ctx = context;
            if (!(visit(
                SpecParser.parse(text)) instanceof Value.OfTerm(KeyTerm term, SpecType type))
                    || !(type instanceof SpecType.Int)) {
                throw new SpecException("expected an integer but got " + text);
            }
            return term;
        } catch (SpecException e) {
            throw new SpecException(where + ": " + e.getMessage());
        }
    }

    @Override
    public Value visitParens(SolSpecParser.ParensContext node) {
        return visit(node.expr());
    }

    @Override
    public Value visitIntLit(SolSpecParser.IntLitContext node) {
        return integer(Key.num(node.INT().getText()));
    }

    @Override
    public Value visitBoolLit(SolSpecParser.BoolLitContext node) {
        return new Value.OfFormula(node.getText().equals("true") ? Key.TRUE : Key.FALSE);
    }

    @Override
    public Value visitResult(SolSpecParser.ResultContext node) {
        if (!ctx.ensures()) {
            throw new SpecException("\\result is only allowed in ensures");
        }
        if (resultType == null) {
            throw new SpecException("\\result needs a function with one named return"
                + " value of an int or bool type; refer to several return values by name");
        }
        return new Value.OfTerm(Key.constant("result"), resultType);
    }

    @Override
    public Value visitOld(SolSpecParser.OldContext node) {
        if (!ctx.ensures()) {
            throw new SpecException("\\old is only allowed in ensures, and not nested");
        }
        return in(ctx.old(), node.expr());
    }

    @Override
    public Value visitNet(SolSpecParser.NetContext node) {
        return integer(Key.typed("selectSt", "int", Key.constant(ctx.net()),
            Key.apply("at", term(node.expr()))));
    }

    @Override
    public Value visitCast(SolSpecParser.CastContext node) {
        return visit(node.expr());
    }

    @Override
    public Value visitIdent(SolSpecParser.IdentContext node) {
        String name = node.getText();
        SpecType local = ctx.locals().get(name);
        if (local != null) {
            return new Value.OfTerm(Key.constant(name), local);
        }
        if (ctx.ensures()) {
            for (SolidityOutline.Parameter ret : function.returns()) {
                if (ret.name().equals(name) && ret.keySort() != null) {
                    return new Value.OfTerm(Key.constant(resultVariable(function, name)),
                        SpecType.of(ret.type()));
                }
            }
        }
        if (name.equals("this") && stateVariable("this") == null) {
            return integer(Key.constant("self"));
        }
        SolidityOutline.Variable variable = stateVariable(name);
        if (variable != null && variable.constantValue() != null) {
            return new Value.OfTerm(Key.num(variable.constantValue()),
                SpecType.of(variable.type()));
        }
        return read(path(node));
    }

    @Override
    public Value visitIndex(SolSpecParser.IndexContext node) {
        return read(path(node));
    }

    @Override
    public Value visitMember(SolSpecParser.MemberContext node) {
        String member = node.IDENT().getText();
        if (node.expr() instanceof SolSpecParser.IdentContext base
                && !ctx.locals().containsKey(base.getText())
                && stateVariable(base.getText()) == null) {
            if (base.getText().equals("msg")) {
                String variable = ParserUtils.msgMemberVariable(member);
                if (variable == null) {
                    throw new SpecException(ParserUtils.unsupportedMsgMember(member));
                }
                return integer(Key.constant(variable));
            }
            List<String> members = contract.enums().get(base.getText());
            if (members != null) {
                int ordinal = members.indexOf(member);
                if (ordinal < 0) {
                    throw new SpecException(
                        "enum " + base.getText() + " has no member " + member);
                }
                return integer(Key.num(ordinal));
            }
        }
        return read(path(node));
    }

    @Override
    public Value visitUnary(SolSpecParser.UnaryContext node) {
        if (node.op.getText().equals("!")) {
            return new Value.OfFormula(Key.not(formula(node.expr())));
        }
        if (node.expr() instanceof SolSpecParser.IntLitContext literal) {
            return integer(Key.num("-" + literal.getText()));
        }
        return integer(Key.apply("neg", term(node.expr())));
    }

    @Override
    public Value visitMul(SolSpecParser.MulContext node) {
        KeyTerm left = term(node.expr(0));
        KeyTerm right = term(node.expr(1));
        return integer(switch (node.op.getText()) {
            case "/" -> Key.apply("div", left, right);
            case "%" -> Key.apply("mod", left, right);
            default -> Key.arithmetic(Operator.TIMES, left, right);
        });
    }

    @Override
    public Value visitAdd(SolSpecParser.AddContext node) {
        return integer(Key.arithmetic(Operator.of(node.op.getText()), term(node.expr(0)),
            term(node.expr(1))));
    }

    @Override
    public Value visitRel(SolSpecParser.RelContext node) {
        return new Value.OfFormula(Key.compare(Relation.of(node.op.getText()),
            term(node.expr(0)), term(node.expr(1))));
    }

    @Override
    public Value visitEq(SolSpecParser.EqContext node) {
        Value left = visit(node.expr(0));
        Value right = visit(node.expr(1));
        boolean bools = left.type() instanceof SpecType.Bool
                || right.type() instanceof SpecType.Bool;
        KeyFormula equality = bools
                ? Key.equivalent(asFormula(left), asFormula(right))
                : Key.eq(asTerm(left), asTerm(right));
        return new Value.OfFormula(
            node.op.getText().equals("==") ? equality : Key.not(equality));
    }

    @Override
    public Value visitAnd(SolSpecParser.AndContext node) {
        return new Value.OfFormula(Key.and(formula(node.expr(0)), formula(node.expr(1))));
    }

    @Override
    public Value visitOr(SolSpecParser.OrContext node) {
        return new Value.OfFormula(Key.or(formula(node.expr(0)), formula(node.expr(1))));
    }

    @Override
    public Value visitImpl(SolSpecParser.ImplContext node) {
        return new Value.OfFormula(Key.implies(formula(node.expr(0)), formula(node.expr(1))));
    }

    @Override
    public Value visitIff(SolSpecParser.IffContext node) {
        return new Value.OfFormula(
            Key.equivalent(formula(node.expr(0)), formula(node.expr(1))));
    }

    @Override
    public Value visitQuantifier(SolSpecParser.QuantifierContext node) {
        SpecType type = node.sort().getText().equals("bool") ? SpecType.BOOL : SpecType.INT;
        String variable = node.var.getText();
        KeyFormula body = asFormula(in(ctx.bind(variable, type), node.expr()));
        return new Value.OfFormula(Key.quantified(node.q.getText().equals("\\forall"),
            type.sort(), variable, body));
    }

    private static Value integer(KeyTerm term) {
        return new Value.OfTerm(term, SpecType.INT);
    }

    private Value in(Context inner, SolSpecParser.ExprContext node) {
        Context outer = ctx;
        try {
            ctx = inner;
            return visit(node);
        } finally {
            ctx = outer;
        }
    }

    private Path path(SolSpecParser.ExprContext node) {
        return switch (node) {
            case SolSpecParser.ParensContext parens -> path(parens.expr());
            case SolSpecParser.IdentContext ident -> {
                SolidityOutline.Variable variable = stateVariable(ident.getText());
                if (variable == null) {
                    throw new SpecException("unknown identifier " + ident.getText());
                }
                List<KeyTerm> segments = new ArrayList<>();
                segments.add(
                    Key.constant(StructLDT.fieldConstantName(contract.name(), ident.getText())));
                yield new Path(segments, SpecType.of(variable.type()));
            }
            case SolSpecParser.IndexContext index -> {
                Path base = path(index.expr(0));
                SpecType element = switch (base.type()) {
                    case SpecType.Mapping mapping -> mapping.value();
                    case SpecType.Array array -> array.element();
                    default -> throw new SpecException(index.expr(0).getText()
                        + " is neither a mapping nor an array and cannot be indexed");
                };
                yield new Path(extend(base.segments(), Key.apply("at", term(index.expr(1)))),
                    element);
            }
            case SolSpecParser.MemberContext member -> {
                Path base = path(member.expr());
                String name = member.IDENT().getText();
                if (base.type() instanceof SpecType.Array && name.equals("length")) {
                    yield new Path(extend(base.segments(), Key.constant("size")), SpecType.INT);
                }
                if (base.type() instanceof SpecType.Struct struct) {
                    List<SolidityOutline.Variable> members = contract.structs().get(struct.name());
                    SolidityOutline.Variable field = members == null ? null
                            : members.stream().filter(m -> m.name().equals(name)).findFirst()
                                    .orElse(null);
                    if (field == null) {
                        throw new SpecException(
                            "struct " + struct.name() + " has no member " + name);
                    }
                    yield new Path(
                        extend(base.segments(), Key.constant(
                            StructLDT.fieldConstantName(contract.name(), struct.name(), name))),
                        SpecType.of(field.type()));
                }
                throw new SpecException(
                    "cannot access member " + name + " of " + member.expr().getText());
            }
            default -> throw new SpecException(node.getText() + " is not a storage location");
        };
    }

    private static List<KeyTerm> extend(List<KeyTerm> segments, KeyTerm segment) {
        List<KeyTerm> extended = new ArrayList<>(segments);
        extended.add(segment);
        return extended;
    }

    private Value read(Path path) {
        String sort = path.type().sort();
        if (sort == null) {
            throw new SpecException("a mapping, array or struct cannot be used as a value: "
                + path.segments().stream().map(KeyPrinter::print)
                        .collect(Collectors.joining(".")));
        }
        return new Value.OfTerm(
            Key.typed("find", sort, Key.constant(ctx.storage()), list(path.segments())),
            path.type());
    }

    private static KeyTerm list(List<KeyTerm> segments) {
        return switch (segments.size()) {
            case 1, 2, 3 -> Key.apply("cons" + segments.size(), segments);
            default -> {
                KeyTerm list = Key.constant("nil");
                for (int i = segments.size() - 1; i >= 0; i--) {
                    list = Key.apply("cons", segments.get(i), list);
                }
                yield list;
            }
        };
    }

    private SolidityOutline.@Nullable Variable stateVariable(String name) {
        return contract.stateVariables().stream().filter(v -> v.name().equals(name)).findFirst()
                .orElse(null);
    }

    private KeyTerm term(SolSpecParser.ExprContext node) {
        return asTerm(visit(node));
    }

    private KeyFormula formula(SolSpecParser.ExprContext node) {
        return asFormula(visit(node));
    }

    private static KeyTerm asTerm(Value value) {
        return switch (value) {
            case Value.OfTerm(KeyTerm term, SpecType ignored) -> term;
            case Value.OfFormula(KeyFormula formula) -> Key.ifThenElse(formula,
                Key.constant("TRUE"), Key.constant("FALSE"));
        };
    }

    private static KeyFormula asFormula(Value value) {
        return switch (value) {
            case Value.OfFormula(KeyFormula formula) -> formula;
            case Value.OfTerm(KeyTerm term, SpecType type) when type instanceof SpecType.Bool ->
                Key.eq(term, Key.constant("TRUE"));
            case Value.OfTerm(KeyTerm term, SpecType ignored) -> throw new SpecException(
                "expected a boolean but got " + KeyPrinter.print(term));
        };
    }
}
