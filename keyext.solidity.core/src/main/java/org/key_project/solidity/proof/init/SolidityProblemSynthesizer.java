/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.proof.init;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.key_project.solidity.keyfile.Key;
import org.key_project.solidity.keyfile.KeyFormula;
import org.key_project.solidity.keyfile.KeyFormula.Assignment;
import org.key_project.solidity.keyfile.KeyFormula.Call;
import org.key_project.solidity.keyfile.KeyPrinter;
import org.key_project.solidity.keyfile.KeyProblem;
import org.key_project.solidity.keyfile.KeyTerm;
import org.key_project.solidity.program.parser.SolidityOutline;
import org.key_project.solidity.speclang.natspec.KeyNatspec;
import org.key_project.solidity.speclang.natspec.SpecCompiler;
import org.key_project.solidity.speclang.natspec.SpecException;
import org.key_project.solidity.speclang.natspec.SpecParser;

/// Builds the proof obligation for a Solidity function, so a `.sol` file can be verified
/// without a hand-written `.key` problem beside it.
///
/// The generated problem calls the function in a modality with postcondition `true`; the
/// specification is carried by the `assert` statements in the function body. A function may
/// select the box modality with a `/// @key box` natspec comment, which turns its leading
/// `require` statements into assumptions instead of obligations (see `docs/require-assert.md`).
///
/// Function parameters are declared as program variables and passed to the call; an integer,
/// address or enum parameter is constrained to the range of its type,
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
        SolidityOutline.Contract contract =
            outline.requireContract(requested == null ? null : requested.contract(), solFile);
        String function = requested == null ? null : requested.function();
        if (function == null) {
            List<SolidityOutline.Function> provable = contract.provableFunctions();
            if (provable.size() != 1) {
                throw new IllegalArgumentException("no function selected for " + solFile
                    + "; use --function with one of: " + names(contract, provable));
            }
            function = contract.id(provable.get(0));
        } else {
            String name = function;
            List<SolidityOutline.Function> overloads = contract.functions().stream()
                    .filter(f -> f.name().equals(name)).toList();
            SolidityOutline.Function declared = contract.function(name)
                    .orElseThrow(() -> new IllegalArgumentException("contract " + contract.name()
                        + " in " + solFile
                        + (overloads.isEmpty() ? " has no public function " + name
                                : " overloads " + name)
                        + "; candidates: " + names(contract,
                            overloads.isEmpty() ? contract.provableFunctions() : overloads)));
            String reason = declared.unsupportedReason().orElse(null);
            if (reason != null) {
                throw new IllegalArgumentException(contract.name() + "." + name + " in " + solFile
                    + " cannot be proved: " + reason);
            }
            function = contract.id(declared);
        }
        return new SolidityProblemSpec(contract.name(), function,
            requested == null ? List.of() : requested.choices());
    }

    /// Every function of `contract` an obligation can be generated for, in declaration order.
    public static List<String> provableFunctions(Path solFile, String contract)
            throws IOException {
        SolidityOutline.Contract declared =
            SolidityOutline.of(solFile).requireContract(contract, solFile);
        return names(declared, declared.provableFunctions());
    }

    public static String problemText(Path solFile, SolidityProblemSpec spec) throws IOException {
        return problemText(solFile, SolidityOutline.of(solFile), spec);
    }

    public static String problemText(Path solFile, SolidityOutline outline,
            SolidityProblemSpec spec) {
        return KeyPrinter.print(problem(solFile, outline, spec));
    }

    public static KeyProblem problem(Path solFile, SolidityOutline outline,
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
            contractSpec.allowOnly("contract " + contract.name(), CONTRACT_CLAUSES);
            functionSpec = function.natspec();
            functionSpec.allowOnly(function.isConstructor() ? "the constructor"
                    : "function " + function.name(),
                FUNCTION_CLAUSES);
            contract.otherDocumentation().forEach(
                (place, text) -> KeyNatspec.of(text).allowOnly(place, Set.of()));
        } catch (SpecException e) {
            throw new SpecException(where + ": " + e.getMessage());
        }
        Map<String, String> renamed = SpecCompiler.parameterVariables(function);
        List<String> arguments = function.parameters().stream()
                .map(parameter -> renamed.get(parameter.name())).toList();
        List<KeyProblem.Variable> variables = new ArrayList<>();
        for (SolidityOutline.Parameter parameter : function.parameters()) {
            variables.add(
                new KeyProblem.Variable(parameter.keySort(), renamed.get(parameter.name())));
        }
        List<String> results = new ArrayList<>();
        for (SolidityOutline.Parameter ret : function.returns()) {
            String variable = SpecCompiler.resultVariable(function, ret.name());
            variables.add(new KeyProblem.Variable(ret.keySort(), variable));
            results.add(variable);
        }
        Call call = Key.call(results, function.name(), arguments, contract.name());
        StorageLayout layout = new StorageLayout(contract);
        if (!contractSpec.isSpecified() && !functionSpec.isSpecified()) {
            KeyFormula obligation = Key.modality(functionSpec.box(), call, Key.TRUE);
            if (function.isConstructor()) {
                obligation = Key.update(List.of(Key.assign("storage", layout.emptyStorage()),
                    Key.assign("net", MT_ST)), obligation);
            } else if (!layout.isEmpty()) {
                obligation = Key.implies(LEN_INV, obligation);
            }
            return new KeyProblem(solFile.toAbsolutePath(), spec.choices(),
                List.of(), variables, layout.rules(),
                withParameterFacts(contract, function, obligation));
        }
        return specifiedProblem(solFile, contract, function, contractSpec, functionSpec,
            spec.choices(), variables, call, where, layout);
    }

    private static KeyFormula withParameterFacts(SolidityOutline.Contract contract,
            SolidityOutline.Function function, KeyFormula obligation) {
        KeyFormula facts = parameterFacts(contract, function);
        return facts.equals(Key.TRUE) ? obligation : Key.implies(facts, obligation);
    }

    private static KeyFormula parameterFacts(SolidityOutline.Contract contract,
            SolidityOutline.Function function) {
        SpecCompiler compiler = new SpecCompiler(contract, function);
        Map<String, String> renamed = SpecCompiler.parameterVariables(function);
        List<KeyFormula> facts = new ArrayList<>();
        for (SolidityOutline.Parameter parameter : function.parameters()) {
            SpecCompiler.Range range = compiler.range(parameter.type());
            KeyFormula fact = range == null ? Key.TRUE
                    : range.contains(Key.constant(renamed.get(parameter.name())));
            if (!fact.equals(Key.TRUE)) {
                facts.add(fact);
            }
        }
        return Key.and(facts);
    }

    private static final Set<KeyNatspec.Kind> CONTRACT_CLAUSES = Set.of(KeyNatspec.Kind.INVARIANT);
    private static final Set<KeyNatspec.Kind> FUNCTION_CLAUSES =
        Set.of(KeyNatspec.Kind.BOX, KeyNatspec.Kind.SKIP, KeyNatspec.Kind.REQUIRES,
            KeyNatspec.Kind.ENSURES, KeyNatspec.Kind.ASSIGNABLE);

    private static final KeyTerm STORAGE = Key.constant("storage");
    private static final KeyTerm NET = Key.constant("net");
    private static final KeyTerm MT_ST = Key.constant("mtSt");
    private static final KeyTerm MSG_SENDER = Key.constant("msgSender");
    private static final KeyTerm MSG_VALUE = Key.constant("msgValue");
    private static final KeyTerm SELF_BALANCE = Key.constant("selfBalance");
    private static final KeyFormula LEN_INV = Key.predicate("lenInv", STORAGE);
    private static final KeyFormula INVARIANT = Key.predicate("CInv", STORAGE, NET);

    private static KeyProblem specifiedProblem(Path solFile, SolidityOutline.Contract contract,
            SolidityOutline.Function function, KeyNatspec contractSpec, KeyNatspec functionSpec,
            List<String> options, List<KeyProblem.Variable> variables, Call call, String where,
            StorageLayout layout) {
        SpecCompiler compiler = new SpecCompiler(contract, function);
        boolean usesOld = functionSpec.ensures().stream()
                .anyMatch(e -> SpecParser.usesOld(SpecParser.parse(e)));
        List<KeyProblem.Variable> declared = new ArrayList<>(variables);
        if (usesOld) {
            declared.add(new KeyProblem.Variable("Struct", "old"));
            declared.add(new KeyProblem.Variable("Struct", "oldNet"));
        }
        KeyFormula invariant = labeled(contractSpec.invariants(),
            text -> compiler.formula(text, SpecCompiler.Context.invariant(),
                contract.name() + " invariant"));
        boolean quantified = contractSpec.invariants().stream()
                .anyMatch(text -> SpecParser.quantifies(SpecParser.parse(text)));
        boolean constructor = function.isConstructor();

        List<KeyFormula> precondition = new ArrayList<>();
        precondition.add(function.payable()
                ? Key.labeled("msg.value >= 0", Key.predicate("geq", MSG_VALUE, Key.num(0)))
                : Key.labeled("msg.value == 0", Key.eq(MSG_VALUE, Key.num(0))));
        precondition.add(Key.labeled("msg.sender != this",
            Key.not(Key.eq(MSG_SENDER, Key.constant("self")))));
        precondition.add(Key.labeled("msg.sender is an address",
            SpecCompiler.Range.ADDRESS.contains(MSG_SENDER)));
        KeyFormula facts = parameterFacts(contract, function);
        if (!facts.equals(Key.TRUE)) {
            precondition.add(facts);
        }
        precondition.addAll(labeledClauses(functionSpec.requires(),
            text -> compiler.formula(text, SpecCompiler.Context.requires(function),
                where + " requires")));
        if (!constructor) {
            if (!layout.isEmpty()) {
                precondition.add(LEN_INV);
            }
            precondition.add(INVARIANT);
        }

        List<KeyFormula> postcondition = new ArrayList<>();
        postcondition.add(INVARIANT);
        postcondition.addAll(labeledClauses(functionSpec.ensures(),
            text -> compiler.formula(text, SpecCompiler.Context.ensures(function),
                where + " ensures")));

        KeyTerm storage = constructor ? MT_ST : STORAGE;
        KeyTerm ledger = constructor ? MT_ST : NET;
        List<Assignment> update = new ArrayList<>();
        if (constructor) {
            update.add(Key.assign("storage", layout.emptyStorage()));
        }
        if (usesOld) {
            update.add(Key.assign("old", storage));
            update.add(Key.assign("oldNet", ledger));
        }
        KeyTerm sender = Key.apply("at", MSG_SENDER);
        update.add(Key.assign("net", Key.apply("storeSt", ledger, sender,
            Key.plus(Key.typed("selectSt", "int", ledger, sender), MSG_VALUE))));
        update.add(Key.assign("selfBalance",
            constructor ? MSG_VALUE : Key.plus(SELF_BALANCE, MSG_VALUE)));

        String s = SpecCompiler.Context.INVARIANT_STORAGE;
        String n = SpecCompiler.Context.INVARIANT_NET;
        KeyProblem.Taclet insertInvariant = new KeyProblem.Taclet("insertCInv", "Struct",
            List.of(s, n), quantified, Key.predicate("CInv", Key.constant(s), Key.constant(n)),
            invariant, "simplify");
        List<KeyProblem.Taclet> rules = new ArrayList<>();
        rules.add(insertInvariant);
        rules.addAll(layout.rules());

        KeyFormula obligation = Key.implies(Key.and(precondition),
            Key.update(update, Key.modality(true, call, Key.and(postcondition))));
        return new KeyProblem(solFile.toAbsolutePath(), options, List.of(),
            declared, rules, obligation);
    }

    private static KeyFormula labeled(List<String> clauses,
            Function<String, KeyFormula> compile) {
        return Key.and(labeledClauses(clauses, compile));
    }

    private static List<KeyFormula> labeledClauses(List<String> clauses,
            Function<String, KeyFormula> compile) {
        return clauses.stream().map(text -> Key.labeled(text, compile.apply(text))).toList();
    }

    /// A path identifying this obligation. It is never created; it only fixes the directory
    /// relative paths resolve against and keeps two obligations over one `.sol` distinct.
    public static Path anchor(Path solFile, SolidityProblemSpec spec) {
        return solFile.toAbsolutePath()
                .resolveSibling(spec.contract() + "." + spec.function() + ".generated.key");
    }

    private static List<String> names(SolidityOutline.Contract contract,
            List<SolidityOutline.Function> functions) {
        return functions.stream().map(contract::id).toList();
    }
}
