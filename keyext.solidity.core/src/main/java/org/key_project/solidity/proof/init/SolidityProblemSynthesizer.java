/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.proof.init;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import org.key_project.solidity.program.parser.SolidityOutline;

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
        SolidityOutline outline = SolidityOutline.of(solFile);
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
        } else if (contract.function(function).isEmpty()) {
            throw new IllegalArgumentException("contract " + contract.name() + " in " + solFile
                + " has no public function " + function + "; candidates: "
                + names(contract.provableFunctions()));
        }
        return SolidityProblemSpec.of(contract.name(), function);
    }

    /// Every function of `contract` an obligation can be generated for, in declaration order.
    public static List<String> provableFunctions(Path solFile, String contract)
            throws IOException {
        return names(resolveContract(solFile, SolidityOutline.of(solFile), contract)
                .provableFunctions());
    }

    public static String problemText(Path solFile, SolidityProblemSpec spec) throws IOException {
        SolidityOutline.Function function =
            resolveContract(solFile, SolidityOutline.of(solFile), spec.contract())
                    .function(spec.function())
                    .orElseThrow(() -> new IllegalArgumentException(
                        "no public function " + spec.function() + " in " + solFile));
        boolean box = function.documentation().contains(BOX_DIRECTIVE);
        List<SolidityOutline.Parameter> parameters = function.parameters();
        String arguments = parameters.stream().map(SolidityOutline.Parameter::name)
                .collect(Collectors.joining(", "));
        String call = spec.function() + "(" + arguments + ")@" + spec.contract() + ";";
        String modality = box ? "\\[{ " + call + " }\\](true)" : "\\<{ " + call + " }\\>(true)";
        String programVariables = parameters.isEmpty() ? ""
                : parameters.stream().map(p -> "    " + p.keySort() + " " + p.name() + ";")
                        .collect(Collectors.joining("\n", "\\programVariables {\n", "\n}\n\n"));
        return """
                \\programSource "%s";

                %s\\problem {
                    %s
                }
                """.formatted(solFile.toAbsolutePath(), programVariables, modality);
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
