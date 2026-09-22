/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.speclang.natspec;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.key_project.solidity.parser.SolSpecBaseVisitor;
import org.key_project.solidity.parser.SolSpecParser;
import org.key_project.solidity.program.parser.SolidityOutline;

import org.jspecify.annotations.Nullable;

/// Compiles a specification expression into the text of a `.key` formula over the `storage` and
/// `net` structs, the way the hand-written problems in `keyext.solidity.examples/net/` spell it:
/// a state variable `x` is `find<[int]>(S, cons1(C$x))`, a mapping entry `m[k]` is
/// `find<[int]>(S, cons2(C$m, at(k)))`, `net(a)` is `selectSt<[int]>(N, at(a))`, `msg.sender`
/// and `msg.value` are the `msgSender` and `msgValue` program variables, `address(this)` is
/// `self`.
///
/// A visitor over the `SolSpec.g4` parse tree; each visit yields a [Value], the emitted text
/// with its type and whether it is a formula or a term.
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

    public record Value(String text, SpecType type, boolean formula) {
    }

    private record Path(List<String> segments, SpecType type) {
    }

    private final SolidityOutline.Contract contract;
    private final @Nullable SpecType resultType;
    private Context ctx = Context.invariant();

    public SpecCompiler(SolidityOutline.Contract contract, SolidityOutline.Function function) {
        this.contract = contract;
        this.resultType =
            function.returns().size() == 1 && function.returns().get(0).keySort() != null
                    ? SpecType.of(function.returns().get(0).type())
                    : null;
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
    public String formula(String text, Context context, String where) {
        try {
            ctx = context;
            return asFormula(visit(SpecParser.parse(text)));
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
        return new Value(node.INT().getText(), SpecType.INT, false);
    }

    @Override
    public Value visitBoolLit(SolSpecParser.BoolLitContext node) {
        return new Value(node.getText(), SpecType.BOOL, true);
    }

    @Override
    public Value visitResult(SolSpecParser.ResultContext node) {
        if (!ctx.ensures()) {
            throw new SpecException("\\result is only allowed in ensures");
        }
        if (resultType == null) {
            throw new SpecException("\\result needs a function with one named return"
                + " value of an int or bool type");
        }
        return new Value("result", resultType, false);
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
        return new Value("selectSt<[int]>(" + ctx.net() + ", at(" + term(node.expr()) + "))",
            SpecType.INT, false);
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
            return new Value(name, local, false);
        }
        if (name.equals("this") && stateVariable("this") == null) {
            return new Value("self", SpecType.INT, false);
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
                return switch (member) {
                    case "sender" -> new Value("msgSender", SpecType.INT, false);
                    case "value" -> new Value("msgValue", SpecType.INT, false);
                    default -> throw new SpecException("unknown member msg." + member
                        + "; only msg.sender and msg.value are modeled");
                };
            }
            List<String> members = contract.enums().get(base.getText());
            if (members != null) {
                int ordinal = members.indexOf(member);
                if (ordinal < 0) {
                    throw new SpecException(
                        "enum " + base.getText() + " has no member " + member);
                }
                return new Value(Integer.toString(ordinal), SpecType.INT, false);
            }
        }
        return read(path(node));
    }

    @Override
    public Value visitUnary(SolSpecParser.UnaryContext node) {
        if (node.op.getText().equals("!")) {
            return new Value("!(" + formula(node.expr()) + ")", SpecType.BOOL, true);
        }
        if (node.expr() instanceof SolSpecParser.IntLitContext literal) {
            return new Value("-" + literal.getText(), SpecType.INT, false);
        }
        return new Value("neg(" + term(node.expr()) + ")", SpecType.INT, false);
    }

    @Override
    public Value visitMul(SolSpecParser.MulContext node) {
        String left = term(node.expr(0));
        String right = term(node.expr(1));
        return switch (node.op.getText()) {
            case "/" -> new Value("div(" + left + ", " + right + ")", SpecType.INT, false);
            case "%" -> new Value("mod(" + left + ", " + right + ")", SpecType.INT, false);
            default -> new Value("(" + left + " * " + right + ")", SpecType.INT, false);
        };
    }

    @Override
    public Value visitAdd(SolSpecParser.AddContext node) {
        return new Value("(" + term(node.expr(0)) + " " + node.op.getText() + " "
            + term(node.expr(1)) + ")", SpecType.INT, false);
    }

    @Override
    public Value visitRel(SolSpecParser.RelContext node) {
        return new Value("(" + term(node.expr(0)) + " " + node.op.getText() + " "
            + term(node.expr(1)) + ")", SpecType.BOOL, true);
    }

    @Override
    public Value visitEq(SolSpecParser.EqContext node) {
        Value left = visit(node.expr(0));
        Value right = visit(node.expr(1));
        boolean bools = left.type() instanceof SpecType.Bool
                || right.type() instanceof SpecType.Bool;
        String equality = bools
                ? "(" + asFormula(left) + " <-> " + asFormula(right) + ")"
                : "(" + asTerm(left) + " = " + asTerm(right) + ")";
        return new Value(node.op.getText().equals("==") ? equality : "!" + equality,
            SpecType.BOOL, true);
    }

    @Override
    public Value visitAnd(SolSpecParser.AndContext node) {
        return connective(node.expr(0), "&", node.expr(1));
    }

    @Override
    public Value visitOr(SolSpecParser.OrContext node) {
        return connective(node.expr(0), "|", node.expr(1));
    }

    @Override
    public Value visitImpl(SolSpecParser.ImplContext node) {
        return connective(node.expr(0), "->", node.expr(1));
    }

    @Override
    public Value visitIff(SolSpecParser.IffContext node) {
        return connective(node.expr(0), "<->", node.expr(1));
    }

    @Override
    public Value visitQuantifier(SolSpecParser.QuantifierContext node) {
        SpecType type = node.sort().getText().equals("bool") ? SpecType.BOOL : SpecType.INT;
        String variable = node.var.getText();
        String body = asFormula(in(ctx.bind(variable, type), node.expr()));
        String quantifier = node.q.getText().equals("\\forall") ? "\\forall " : "\\exists ";
        return new Value("(" + quantifier + type.sort() + " " + variable + "; " + body + ")",
            SpecType.BOOL, true);
    }

    private Value connective(SolSpecParser.ExprContext left, String op,
            SolSpecParser.ExprContext right) {
        return new Value("(" + formula(left) + " " + op + " " + formula(right) + ")",
            SpecType.BOOL, true);
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
                List<String> segments = new ArrayList<>();
                segments.add(contract.name() + "$" + ident.getText());
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
                yield new Path(extend(base.segments(), "at(" + term(index.expr(1)) + ")"),
                    element);
            }
            case SolSpecParser.MemberContext member -> {
                Path base = path(member.expr());
                String name = member.IDENT().getText();
                if (base.type() instanceof SpecType.Array && name.equals("length")) {
                    yield new Path(extend(base.segments(), "size"), SpecType.INT);
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
                        extend(base.segments(),
                            contract.name() + "$" + struct.name() + "$" + name),
                        SpecType.of(field.type()));
                }
                throw new SpecException(
                    "cannot access member " + name + " of " + member.expr().getText());
            }
            default -> throw new SpecException(node.getText() + " is not a storage location");
        };
    }

    private static List<String> extend(List<String> segments, String segment) {
        List<String> extended = new ArrayList<>(segments);
        extended.add(segment);
        return extended;
    }

    private Value read(Path path) {
        String sort = path.type().sort();
        if (sort == null) {
            throw new SpecException("a mapping, array or struct cannot be used as a value: "
                + String.join(".", path.segments()));
        }
        return new Value("find<[" + sort + "]>(" + ctx.storage() + ", " + list(path.segments())
            + ")", path.type(), false);
    }

    private static String list(List<String> segments) {
        return switch (segments.size()) {
            case 1 -> "cons1(" + segments.get(0) + ")";
            case 2 -> "cons2(" + segments.get(0) + ", " + segments.get(1) + ")";
            case 3 -> "cons3(" + segments.get(0) + ", " + segments.get(1) + ", "
                + segments.get(2) + ")";
            default -> {
                String list = "nil";
                for (int i = segments.size() - 1; i >= 0; i--) {
                    list = "cons(" + segments.get(i) + ", " + list + ")";
                }
                yield list;
            }
        };
    }

    private SolidityOutline.@Nullable Variable stateVariable(String name) {
        return contract.stateVariables().stream().filter(v -> v.name().equals(name)).findFirst()
                .orElse(null);
    }

    private String term(SolSpecParser.ExprContext node) {
        return asTerm(visit(node));
    }

    private String formula(SolSpecParser.ExprContext node) {
        return asFormula(visit(node));
    }

    private static String asTerm(Value value) {
        if (!value.formula()) {
            return value.text();
        }
        return "\\if (" + value.text() + ") \\then (TRUE) \\else (FALSE)";
    }

    private static String asFormula(Value value) {
        if (value.formula()) {
            return value.text();
        }
        if (value.type() instanceof SpecType.Bool) {
            return "(" + value.text() + " = TRUE)";
        }
        throw new SpecException("expected a boolean but got " + value.text());
    }
}
