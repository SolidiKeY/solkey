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

    private static final String EMPTY_STORAGE = "storage := mtSt || net := mtSt";

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
        SolidityOutline.Contract contract =
            outline.requireContract(requested == null ? null : requested.contract(), solFile);
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
        return names(SolidityOutline.of(solFile).requireContract(contract, solFile)
                .provableFunctions());
    }

    public static String problemText(Path solFile, SolidityProblemSpec spec) throws IOException {
        return problemText(solFile, SolidityOutline.of(solFile), spec);
    }

    public static String problemText(Path solFile, SolidityOutline outline,
            SolidityProblemSpec spec) {
        SolidityOutline.Contract contract = outline.requireContract(spec.contract(), solFile);
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
        List<String> results = new ArrayList<>();
        for (SolidityOutline.Parameter ret : function.returns()) {
            String variable = SpecCompiler.resultVariable(function, ret.name());
            variables.add(ret.keySort() + " " + variable);
            results.add(variable);
        }
        String result = results.isEmpty() ? ""
                : results.size() == 1 ? results.get(0) + " = "
                        : "(" + String.join(", ", results) + ") = ";
        String call = result + spec.function() + "(" + arguments + ")@" + spec.contract() + ";";
        String options = spec.choices().isEmpty() ? ""
                : spec.choices().stream()
                        .collect(Collectors.joining(", ", "\\withOptions ", ";\n\n"));
        if (!contractSpec.isSpecified() && !functionSpec.isSpecified()) {
            String modality = functionSpec.box() ? "\\[{ " + call + " }\\](true)"
                    : "\\<{ " + call + " }\\>(true)";
            if (function.isConstructor()) {
                modality = "{" + EMPTY_STORAGE + "} " + modality;
            }
            return """
                    %s\\problem {
                        %s
                    }
                    """.formatted(header(solFile, options, variables), modality);
        }
        return specifiedProblemText(solFile, contract, function, contractSpec, functionSpec,
            options, variables, call, where);
    }

    private static String header(Path solFile, String options, List<String> variables) {
        String declarations = variables.isEmpty() ? ""
                : variables.stream().map(v -> "    " + v + ";")
                        .collect(Collectors.joining("\n", "\\programVariables {\n", "\n}\n\n"));
        return "\\programSource \"" + solFile.toAbsolutePath() + "\";\n\n" + options + declarations;
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
                    "            ", false);
        boolean quantified = contractSpec.invariants().stream()
                .anyMatch(text -> SpecParser.quantifies(SpecParser.parse(text)));
        String precondition = "    // " + (function.payable() ? "msg.value >= 0" : "msg.value == 0")
            + " :\n    " + (function.payable() ? "geq(msgValue, 0)" : "msgValue = 0")
            + "\n    // msg.sender != this :\n    & !(msgSender = self)"
            + conjunction(functionSpec.requires(),
                text -> compiler.formula(text, SpecCompiler.Context.requires(parameters),
                    where + " requires"),
                "    ", true);
        String postcondition = "CInv(storage, net)" + conjunction(functionSpec.ensures(),
            text -> compiler.formula(text, SpecCompiler.Context.ensures(parameters),
                where + " ensures"),
            "         ", true);
        boolean constructor = function.isConstructor();
        String storage = constructor ? "mtSt" : "storage";
        String ledger = constructor ? "mtSt" : "net";
        String update = (constructor ? "storage := mtSt" + "\n     || " : "")
            + (usesOld ? "old := " + storage + " || oldNet := " + ledger + "\n     || " : "")
            + "net := storeSt(" + ledger + ", at(msgSender), selectSt<[int]>(" + ledger
            + ", at(msgSender)) + msgValue)\n     || selfBalance := "
            + (constructor ? "" : "selfBalance + ") + "msgValue";
        return """
                %s\\rules {
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
                    %s->
                    {%s}
                    \\[{ %s }\\]
                        (%s)
                }
                """.formatted(header(solFile, options, declared),
            !quantified ? ""
                    : "        \\varcond(\\noFreeVarIn(s), \\noFreeVarIn(n))\n",
            invariant, precondition, constructor ? "" : "& CInv(storage, net) ", update, call,
            postcondition);
    }

    private static String conjunction(List<String> clauses, Function<String, String> compile,
            String indent, boolean continued) {
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < clauses.size(); i++) {
            boolean conjunct = continued || i > 0;
            if (conjunct) {
                text.append("\n");
            }
            text.append(indent).append("// ").append(clauses.get(i)).append(" :\n")
                    .append(indent).append(conjunct ? "& " : "")
                    .append(compile.apply(clauses.get(i)));
        }
        return text.toString();
    }

    /// A path identifying this obligation. It is never created; it only fixes the directory
    /// relative paths resolve against and keeps two obligations over one `.sol` distinct.
    public static Path anchor(Path solFile, SolidityProblemSpec spec) {
        return solFile.toAbsolutePath()
                .resolveSibling(spec.contract() + "." + spec.function() + ".generated.key");
    }

    private static List<String> names(List<SolidityOutline.Function> functions) {
        return functions.stream().map(SolidityOutline.Function::name).toList();
    }
}
