/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.proof.init;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.key_project.solidity.program.parser.SolidityOutline;
import org.key_project.solidity.speclang.natspec.KeyNatspec;
import org.key_project.solidity.speclang.natspec.SpecCompiler;
import org.key_project.solidity.speclang.natspec.SpecException;
import org.key_project.solidity.speclang.natspec.SpecParser;
import org.key_project.solidity.speclang.natspec.SpecType;

/// Builds the proof obligation for a Solidity function, so a `.sol` file can be verified
/// without a hand-written `.key` problem beside it.
///
/// The generated problem calls the function in a modality with postcondition `true`; the
/// specification is carried by the `assert` statements in the function body. A function may
/// select the box modality with a `/// @key box` natspec comment, which turns its leading
/// `require` statements into assumptions instead of obligations (see `docs/require-assert.md`).
///
/// Function parameters are declared as unconstrained program variables and passed to the call,
/// so a parameterized function is normally box-tagged and assumes the values its asserts rely
/// on with a leading `require(x == 5 && y == 7)`.
public final class SolidityProblemSynthesizer {

    /// Natspec directive selecting the box modality. `@custom:` is solc's extension prefix; any
    /// other tag is rejected as invalid documentation.
    public static final String BOX_DIRECTIVE = "@custom:key box";

    private SolidityProblemSynthesizer() {}

    /// Fills in whatever the caller left open, and fails with the available candidates listed
    /// when the request cannot be met.
    public static SolidityProblemSpec resolve(Path solFile, SolidityProblemSpec requested)
            throws IOException {
        return resolve(solFile, SolidityOutline.of(solFile), requested);
    }

    /// Resolves against an outline the caller already read, so a GUI that has just shown what the
    /// file declares does not fork solc a second time.
    public static SolidityProblemSpec resolve(Path solFile, SolidityOutline outline,
            SolidityProblemSpec requested) {
        SolidityOutline.Contract contract = resolveContract(solFile, outline,
            requested == null ? null : requested.contract());
        String function = requested == null ? null : requested.function();
        if (function == null) {
            List<SolidityOutline.Function> provable = contract.provableFunctions();
            if (provable.size() != 1) {
                throw new IllegalArgumentException("no function selected for " + solFile
                    + "; use --function with one of: " + names(provable));
            }
            function = provable.get(0).name();
        } else {
            String name = function;
            SolidityOutline.Function declared = contract.function(name)
                    .orElseThrow(() -> new IllegalArgumentException("contract " + contract.name()
                        + " in " + solFile + " has no public function " + name + "; candidates: "
                        + names(contract.provableFunctions())));
            String reason = declared.unsupportedReason().orElse(null);
            if (reason != null) {
                throw new IllegalArgumentException(contract.name() + "." + name + " in " + solFile
                    + " cannot be proved: " + reason);
            }
        }
        return new SolidityProblemSpec(contract.name(), function,
            requested == null ? List.of() : requested.choices());
    }

    /// Every function of `contract` an obligation can be generated for, in declaration order.
    public static List<String> provableFunctions(Path solFile, String contract)
            throws IOException {
        return names(resolveContract(solFile, SolidityOutline.of(solFile), contract)
                .provableFunctions());
    }

    public static String problemText(Path solFile, SolidityProblemSpec spec) throws IOException {
        SolidityOutline.Contract contract =
            resolveContract(solFile, SolidityOutline.of(solFile), spec.contract());
        SolidityOutline.Function function = contract.function(spec.function())
                .orElseThrow(() -> new IllegalArgumentException(
                    "no public function " + spec.function() + " in " + solFile));
        String where = contract.name() + "." + function.name();
        KeyNatspec contractSpec;
        KeyNatspec functionSpec;
        try {
            contractSpec = KeyNatspec.of(contract.documentation());
            functionSpec = function.natspec();
        } catch (SpecException e) {
            throw new SpecException(where + ": " + e.getMessage());
        }
        List<SolidityOutline.Parameter> parameters = function.parameters();
        String arguments = parameters.stream().map(SolidityOutline.Parameter::name)
                .collect(Collectors.joining(", "));
        List<String> variables = new ArrayList<>();
        for (SolidityOutline.Parameter parameter : parameters) {
            variables.add(parameter.keySort() + " " + parameter.name());
        }
        String result = "";
        if (!function.returns().isEmpty()) {
            variables.add(function.returns().get(0).keySort() + " result");
            result = "result = ";
        }
        String call = result + spec.function() + "(" + arguments + ")@" + spec.contract() + ";";
        String options = spec.choices().isEmpty() ? ""
                : spec.choices().stream()
                        .collect(Collectors.joining(", ", "\\withOptions ", ";\n\n"));
        if (!contractSpec.isSpecified() && !functionSpec.isSpecified()) {
            String modality = functionSpec.box() ? "\\[{ " + call + " }\\](true)"
                    : "\\<{ " + call + " }\\>(true)";
            return """
                    \\programSource "%s";

                    %s%s\\problem {
                        %s
                    }
                    """.formatted(solFile.toAbsolutePath(), options,
                programVariables(variables), modality);
        }
        return specifiedProblemText(solFile, contract, function, contractSpec, functionSpec,
            options, variables, call, where);
    }

    private static String programVariables(List<String> variables) {
        return variables.isEmpty() ? ""
                : variables.stream().map(v -> "    " + v + ";")
                        .collect(Collectors.joining("\n", "\\programVariables {\n", "\n}\n\n"));
    }

    private static String specifiedProblemText(Path solFile, SolidityOutline.Contract contract,
            SolidityOutline.Function function, KeyNatspec contractSpec, KeyNatspec functionSpec,
            String options, List<String> variables, String call, String where) {
        SpecCompiler compiler = new SpecCompiler(contract, function);
        Map<String, SpecType> parameters = SpecCompiler.parameterTypes(function);
        boolean usesOld = functionSpec.ensures().stream()
                .anyMatch(e -> SpecParser.usesOld(SpecParser.parse(e)));
        List<String> declared = new ArrayList<>(variables);
        if (usesOld) {
            declared.add("Struct old");
            declared.add("Struct oldNet");
        }
        String invariant = contractSpec.invariants().isEmpty() ? "            true"
                : conjunction(contractSpec.invariants(),
                    text -> compiler.formula(text, SpecCompiler.Context.invariant(),
                        contract.name() + " invariant"),
                    "            ");
        boolean quantified = contractSpec.invariants().stream()
                .anyMatch(SolidityProblemSynthesizer::quantifies);
        String precondition = "    // " + (function.payable() ? "msg.value >= 0" : "msg.value == 0")
            + " :\n    " + (function.payable() ? "geq(msgValue, 0)" : "msgValue = 0")
            + functionSpec.requires().stream()
                    .map(text -> "\n    // " + text + " :\n    & "
                        + compiler.formula(text, SpecCompiler.Context.requires(parameters),
                            where + " requires"))
                    .collect(Collectors.joining());
        String postcondition = "CInv(storage, net)" + functionSpec.ensures().stream()
                .map(text -> "\n         // " + text + " :\n         & "
                    + compiler.formula(text, SpecCompiler.Context.ensures(parameters),
                        where + " ensures"))
                .collect(Collectors.joining());
        String update = (usesOld ? "old := storage || oldNet := net\n     || " : "")
            + "net := storeSt(net, at(msgSender), selectSt<[int]>(net, at(msgSender)) + msgValue)"
            + "\n     || selfBalance := selfBalance + msgValue";
        return """
                \\programSource "%s";

                %s%s\\rules {
                    insertCInv {
                        \\schemaVar \\term Struct s, n;
                        \\find(CInv(s, n))
                %s        \\replacewith(
                %s)
                        \\heuristics(simplify)
                    };
                }

                \\problem {
                %s
                    & CInv(storage, net) ->
                    {%s}
                    \\[{ %s }\\]
                        (%s)
                }
                """.formatted(solFile.toAbsolutePath(), options, programVariables(declared),
            !quantified ? ""
                    : "        \\varcond(\\noFreeVarIn(s), \\noFreeVarIn(n))\n",
            invariant, precondition, update, call, postcondition);
    }

    private static boolean quantifies(String invariant) {
        boolean[] found = { false };
        SpecParser.forEachQuantifier(SpecParser.parse(invariant), quantifier -> found[0] = true);
        return found[0];
    }

    private static String conjunction(List<String> clauses, Function<String, String> compile,
            String indent) {
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < clauses.size(); i++) {
            text.append(indent).append("// ").append(clauses.get(i)).append(" :\n")
                    .append(indent).append(i == 0 ? "" : "& ")
                    .append(compile.apply(clauses.get(i)));
            if (i + 1 < clauses.size()) {
                text.append("\n");
            }
        }
        return text.toString();
    }

    /// A path identifying this obligation. It is never created; it only fixes the directory
    /// relative paths resolve against and keeps two obligations over one `.sol` distinct.
    public static Path anchor(Path solFile, SolidityProblemSpec spec) {
        return solFile.toAbsolutePath()
                .resolveSibling(spec.contract() + "." + spec.function() + ".generated.key");
    }

    private static SolidityOutline.Contract resolveContract(Path solFile,
            SolidityOutline outline, String requested) {
        if (requested != null) {
            return outline.contract(requested)
                    .orElseThrow(() -> new IllegalArgumentException(solFile
                        + " declares no contract " + requested + "; candidates: "
                        + outline.contracts().stream().map(SolidityOutline.Contract::name)
                                .toList()));
        }
        if (outline.contracts().size() != 1) {
            throw new IllegalArgumentException("no contract selected for " + solFile
                + "; use --contract with one of: "
                + outline.contracts().stream().map(SolidityOutline.Contract::name).toList());
        }
        return outline.contracts().get(0);
    }

    private static List<String> names(List<SolidityOutline.Function> functions) {
        return functions.stream().map(SolidityOutline.Function::name).toList();
    }
}
