/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.keyfile;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.key_project.solidity.keyfile.KeyFormula.And;
import org.key_project.solidity.keyfile.KeyFormula.Assignment;
import org.key_project.solidity.keyfile.KeyFormula.Binary;
import org.key_project.solidity.keyfile.KeyFormula.Call;
import org.key_project.solidity.keyfile.KeyFormula.Compare;
import org.key_project.solidity.keyfile.KeyFormula.Equals;
import org.key_project.solidity.keyfile.KeyFormula.Labeled;
import org.key_project.solidity.keyfile.KeyFormula.Modality;
import org.key_project.solidity.keyfile.KeyFormula.Not;
import org.key_project.solidity.keyfile.KeyFormula.Predicate;
import org.key_project.solidity.keyfile.KeyFormula.Quantified;
import org.key_project.solidity.keyfile.KeyFormula.Truth;
import org.key_project.solidity.keyfile.KeyFormula.Update;

public final class KeyPrinter {

    private static final String INDENT = "    ";

    private KeyPrinter() {}

    public static String print(KeyProblem problem) {
        StringBuilder out = new StringBuilder();
        out.append("\\programSource \"").append(problem.source()).append("\";\n\n");
        if (!problem.options().isEmpty()) {
            out.append("\\withOptions ").append(String.join(", ", problem.options()))
                    .append(";\n\n");
        }
        out.append(section("\\functions", problem.functions(),
            f -> INDENT + (f.unique() ? "\\unique " : "") + f.sort() + " " + f.name() + ";\n"));
        out.append(section("\\programVariables", problem.programVariables(),
            v -> INDENT + v.sort() + " " + v.name() + ";\n"));
        out.append(section("\\rules", problem.rules(), KeyPrinter::taclet));
        return out.append("\\problem {\n").append(INDENT)
                .append(block(problem.problem(), INDENT)).append("\n}\n").toString();
    }

    public static String print(KeyFormula formula) {
        return formula(formula);
    }

    public static String print(KeyTerm term) {
        return term(term);
    }

    private static <T> String section(String keyword, List<T> entries,
            Function<T, String> entry) {
        if (entries.isEmpty()) {
            return "";
        }
        return entries.stream().map(entry)
                .collect(Collectors.joining("", keyword + " {\n", "}\n\n"));
    }

    private static String taclet(KeyProblem.Taclet taclet) {
        String body = INDENT.repeat(3);
        String replacewith = block(taclet.replacewith(), body);
        if (replacewith.startsWith("//")) {
            replacewith = "\n" + body + replacewith;
        }
        String inner = INDENT.repeat(2);
        StringBuilder out = new StringBuilder();
        out.append(INDENT).append(taclet.name()).append(" {\n");
        out.append(inner).append("\\schemaVar \\term ").append(taclet.schemaSort()).append(" ")
                .append(String.join(", ", taclet.schemaVariables())).append(";\n");
        out.append(inner).append("\\find(").append(formula(taclet.find())).append(")\n");
        if (taclet.noFreeVariables()) {
            out.append(inner).append(taclet.schemaVariables().stream()
                    .map(v -> "\\noFreeVarIn(" + v + ")")
                    .collect(Collectors.joining(", ", "\\varcond(", ")\n")));
        }
        out.append(inner).append("\\replacewith(").append(replacewith).append(")\n");
        out.append(inner).append("\\heuristics(").append(taclet.heuristic()).append(")\n");
        return out.append(INDENT).append("};\n").toString();
    }

    private static String block(KeyFormula formula, String indent) {
        return switch (formula) {
            case Labeled labeled -> comment(labeled, indent) + block(labeled.formula(), indent);
            case And and -> {
                StringBuilder out = new StringBuilder();
                List<KeyFormula> conjuncts = and.conjuncts();
                for (int i = 0; i < conjuncts.size(); i++) {
                    KeyFormula conjunct = conjuncts.get(i);
                    if (i > 0) {
                        out.append("\n").append(indent);
                    }
                    if (conjunct instanceof Labeled labeled) {
                        out.append(comment(labeled, indent));
                    }
                    out.append(i > 0 ? "& " : "").append(operand(conjunct));
                }
                yield out.toString();
            }
            case Binary(Binary.Connective connective, KeyFormula left, KeyFormula right) when connective == Binary.Connective.IMPLIES ->
                (left instanceof And
                        || left instanceof Labeled ? block(left, indent) : operand(left))
                    + " ->\n" + indent + block(right, indent);
            case Update update -> update.assignments().stream().map(KeyPrinter::assignment)
                    .collect(Collectors.joining("\n" + indent + " || ", "{", "}"))
                + "\n" + indent + block(update.target(), indent);
            case Modality modality when modality.post() instanceof And
                    || modality.post() instanceof Labeled ->
                modalityPrefix(modality) + "\n"
                    + indent + INDENT + "(" + block(modality.post(), indent + INDENT + " ")
                    + ")";
            default -> formula(formula);
        };
    }

    private static String comment(Labeled labeled, String indent) {
        return "// " + labeled.label() + " :\n" + indent;
    }

    private static String formula(KeyFormula formula) {
        return switch (formula) {
            case Truth truth -> truth.value() ? "true" : "false";
            case Predicate predicate -> predicate.name() + arguments(predicate.arguments());
            case Equals(KeyTerm left, KeyTerm right) -> infix(term(left), "=", term(right));
            case Compare compare -> infix(term(compare.left()), compare.relation().symbol,
                term(compare.right()));
            case Not not -> "!" + operand(not.operand());
            case And and -> and.conjuncts().stream().map(KeyPrinter::operand)
                    .collect(Collectors.joining(" & ", "(", ")"));
            case Binary binary -> infix(operand(binary.left()), binary.connective().symbol,
                operand(binary.right()));
            case Quantified q -> "(" + (q.universal() ? "\\forall " : "\\exists ") + q.sort()
                + " " + q.variable() + "; " + formula(q.body()) + ")";
            case Update update -> update.assignments().stream().map(KeyPrinter::assignment)
                    .collect(Collectors.joining(" || ", "{", "}"))
                    + operand(update.target());
            case Modality modality -> modalityPrefix(modality) + parenthesized(modality.post());
            case Labeled labeled -> formula(labeled.formula());
        };
    }

    private static String operand(KeyFormula formula) {
        return switch (formula) {
            case Update update -> "(" + formula(update) + ")";
            case Labeled labeled -> operand(labeled.formula());
            default -> formula(formula);
        };
    }

    private static String parenthesized(KeyFormula formula) {
        return switch (formula) {
            case Equals e -> formula(e);
            case Compare c -> formula(c);
            case And a -> formula(a);
            case Binary b -> formula(b);
            case Quantified q -> formula(q);
            case Labeled labeled -> parenthesized(labeled.formula());
            default -> "(" + formula(formula) + ")";
        };
    }

    private static String modalityPrefix(Modality modality) {
        return (modality.box() ? "\\[{ " : "\\<{ ") + call(modality.call())
                + (modality.box() ? " }\\]" : " }\\>");
    }

    private static String call(Call call) {
        String results = switch (call.results().size()) {
            case 0 -> "";
            case 1 -> call.results().get(0) + " = ";
            default -> "(" + String.join(", ", call.results()) + ") = ";
        };
        return results + call.function() + "(" + String.join(", ", call.arguments()) + ")@"
            + call.contract() + ";";
    }

    private static String assignment(Assignment assignment) {
        return assignment.variable() + " := " + term(assignment.value());
    }

    private static String term(KeyTerm term) {
        return switch (term) {
            case KeyTerm.Constant constant -> constant.name();
            case KeyTerm.IntLiteral literal -> literal.value().toString();
            case KeyTerm.Apply apply -> apply.function()
                    + (apply.sorts().isEmpty() ? ""
                            : "<[" + String.join(", ", apply.sorts()) + "]>")
                    + arguments(apply.arguments());
            case KeyTerm.Arithmetic arithmetic -> infix(term(arithmetic.left()),
                arithmetic.operator().symbol, term(arithmetic.right()));
            case KeyTerm.IfThenElse ite -> "\\if (" + formula(ite.condition()) + ") \\then ("
                + term(ite.then()) + ") \\else (" + term(ite.otherwise()) + ")";
        };
    }

    private static String arguments(List<KeyTerm> arguments) {
        return arguments.stream().map(KeyPrinter::term)
                .collect(Collectors.joining(", ", "(", ")"));
    }

    private static String infix(String left, String operator, String right) {
        return "(" + left + " " + operator + " " + right + ")";
    }
}
