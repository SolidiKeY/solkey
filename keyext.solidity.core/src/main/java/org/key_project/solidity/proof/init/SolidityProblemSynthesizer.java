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

    /// The natspec tag carrying this loader's directives. `@custom:` is solc's extension prefix;
    /// any other tag is rejected as invalid documentation.
    public static final String KEY_TAG = "@custom:key";

    /// Natspec directive selecting the box modality.
    public static final String BOX_DIRECTIVE = KEY_TAG + " box";

    /// Natspec directive assuming that the initial storage matches the contract's declared
    /// layout. Written in the same `@custom:key` tag as [#BOX_DIRECTIVE] when both are wanted
    /// (`/// @custom:key box wellformed`), since solc keeps one tag of a name per function.
    /// See [WellFormedTacletGenerator] for what the assumption says.
    public static final String WELLFORMED_DIRECTIVE = "wellformed";

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
        SolidityOutline.Contract contract =
            resolveContract(solFile, SolidityOutline.of(solFile), spec.contract());
        SolidityOutline.Function function = contract.function(spec.function())
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
        String rules = "";
        String obligation = modality;
        if (hasDirective(function.documentation(), WELLFORMED_DIRECTIVE)) {
            rules = WellFormedTacletGenerator.rulesBlock(contract);
            obligation = "wellFormed(storage) -> " + modality;
        }
        return """
                \\programSource "%s";

                %s%s\\problem {
                    %s
                }
                """.formatted(solFile.toAbsolutePath(), programVariables, rules, obligation);
    }

    /// Whether the function's `@custom:key` natspec tag lists `directive`. One tag carries all
    /// directives of a function (`/// @custom:key box wellformed`), because solc keeps a single
    /// tag per name.
    private static boolean hasDirective(String documentation, String directive) {
        int tag = documentation.indexOf(KEY_TAG);
        if (tag < 0) {
            return false;
        }
        int lineEnd = documentation.indexOf('\n', tag);
        String line = lineEnd < 0 ? documentation.substring(tag)
                : documentation.substring(tag, lineEnd);
        return List.of(line.substring(KEY_TAG.length()).trim().split("\\s+")).contains(directive);
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
