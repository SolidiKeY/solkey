/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.runtime;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.key_project.solidity.program.parser.SolcWrapper;
import org.key_project.solidity.program.parser.SolidityOutline;

import org.apache.tuweni.bytes.Bytes;
import org.jspecify.annotations.Nullable;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/// Runs a contract's functions on an in-process EVM and reports what each one did.
///
/// This is what answers "does the `assert` actually fail?" without a proof: solc compiles the
/// contract, the runtime bytecode is deployed on a Besu EVM with all-zero storage, and every
/// function is called. A failing `assert` reaches the caller as `Panic(0x01)`.
///
/// It is a complement to a proof, not a substitute: one call with one set of arguments says
/// nothing about the other states the proof quantifies over. It is, however, immediate — and a
/// `Panic(0x01)` here is a counterexample that no proof can talk away.
public final class SolidityRuntimeCheck {

    private static final Bytes PANIC_SELECTOR = Bytes.fromHexString("0x4e487b71");

    private static final Map<Integer, String> PANIC_NAMES = Map.of(
        0x01, "assert failed",
        0x11, "arithmetic overflow",
        0x12, "division by zero",
        0x21, "invalid enum value",
        0x22, "corrupt storage byte array",
        0x31, "pop on empty array",
        0x32, "array index out of bounds",
        0x41, "allocation too large",
        0x51, "uninitialized function pointer");

    /// What became of one call.
    public enum Outcome {
        /// Ran to the end; every `assert` on the path held.
        OK,
        /// `Panic(0x01)`: an `assert` was violated.
        ASSERT_FAILED,
        /// Another `Panic`: checked arithmetic, a bad index, and so on.
        PANIC,
        /// A `require` (or a bare `revert`) stopped the call before any `assert` was reached.
        REVERTED,
        /// The EVM itself gave up — out of gas, a bad jump.
        HALTED,
        /// Not run, with the reason: no arguments could be synthesized, say.
        SKIPPED
    }

    /// One function's verdict. [#detail] is empty when [#outcome] says everything.
    public record Verdict(String function, Outcome outcome, String detail) {

        public String describe() {
            return detail.isEmpty() ? outcome.toString() : outcome + ": " + detail;
        }
    }

    private SolidityRuntimeCheck() {
    }

    /// Runs `function` of `contract`, or every provable function of it when `function` is null.
    ///
    /// @throws IOException if solc cannot compile `solFile`, or it declares no such contract
    public static List<Verdict> run(Path solFile, @Nullable String contract,
            @Nullable String function) throws IOException {
        SolidityOutline outline = SolidityOutline.of(solFile);
        SolidityOutline.Contract target = contractOf(outline, contract, solFile);

        String deploymentLogic = deploymentLogicOf(solFile, target.name());
        if (deploymentLogic != null) {
            return List.of(new Verdict(target.name(), Outcome.SKIPPED, deploymentLogic));
        }

        List<SolidityOutline.Function> functions = functionsOf(target, function);
        if (functions.isEmpty()) {
            return List.of();
        }
        EvmContractRunner runner =
            new EvmContractRunner(runtimeBytecode(solFile, target.name()));

        List<Verdict> verdicts = new ArrayList<>();
        for (SolidityOutline.Function fn : functions) {
            verdicts.add(verdictFor(runner, solFile, target.name(), fn));
        }
        return verdicts;
    }

    private static SolidityOutline.Contract contractOf(SolidityOutline outline,
            @Nullable String contract, Path solFile) throws IOException {
        if (contract != null) {
            return outline.contract(contract).orElseThrow(() -> new IOException(
                solFile + " declares no contract " + contract + "; candidates: "
                    + outline.contracts().stream().map(SolidityOutline.Contract::name).toList()));
        }
        if (outline.contracts().size() != 1) {
            throw new IOException(solFile + " declares "
                + (outline.contracts().isEmpty() ? "no contract" : "several contracts")
                + "; name one with --contract. Candidates: "
                + outline.contracts().stream().map(SolidityOutline.Contract::name).toList());
        }
        return outline.contracts().get(0);
    }

    private static List<SolidityOutline.Function> functionsOf(SolidityOutline.Contract contract,
            @Nullable String function) throws IOException {
        if (function == null) {
            return contract.provableFunctions();
        }
        return List.of(contract.function(function).orElseThrow(() -> new IOException(
            "contract " + contract.name() + " declares no function " + function)));
    }

    private static Verdict verdictFor(EvmContractRunner runner, Path solFile, String contract,
            SolidityOutline.Function fn) throws IOException {
        List<BigInteger> arguments = List.of();
        if (!fn.parameters().isEmpty()) {
            Optional<List<BigInteger>> pinned = PinnedArguments.of(solFile, contract, fn);
            if (pinned.isEmpty()) {
                return new Verdict(fn.name(), Outcome.SKIPPED,
                    "takes parameters no leading require pins to a value");
            }
            arguments = pinned.get();
        }
        EvmContractRunner.CallResult result = runner.call(Abi.signatureOf(fn), arguments);
        return switch (result.status()) {
            case SUCCESS -> new Verdict(fn.name(), Outcome.OK, "");
            case EXCEPTIONAL_HALT ->
                new Verdict(fn.name(), Outcome.HALTED, result.haltReason());
            case REVERT -> revertVerdict(fn.name(), result.revertData());
        };
    }

    private static Verdict revertVerdict(String function, Bytes revertData) {
        int panic = panicCode(revertData);
        if (panic < 0) {
            return new Verdict(function, Outcome.REVERTED,
                "a require stopped the call before the end");
        }
        String named = "Panic(0x%02x %s)".formatted(panic,
            PANIC_NAMES.getOrDefault(panic, "unknown"));
        return panic == 0x01
                ? new Verdict(function, Outcome.ASSERT_FAILED, named)
                : new Verdict(function, Outcome.PANIC, named);
    }

    /// The `Panic` code carried by `revertData`, or `-1` when it is not a `Panic` payload.
    private static int panicCode(Bytes revertData) {
        if (revertData.size() != 36 || !revertData.slice(0, 4).equals(PANIC_SELECTOR)) {
            return -1;
        }
        return revertData.slice(4).toUnsignedBigInteger().intValueExact();
    }

    private static String runtimeBytecode(Path solFile, String contract) throws IOException {
        JsonNode contracts =
            new ObjectMapper().readTree(SolcWrapper.getBinJson(solFile)).get("contracts");
        for (Map.Entry<String, JsonNode> unit : contracts.properties()) {
            JsonNode compiled = unit.getValue().get(contract);
            if (compiled != null) {
                return compiled.get("evm").get("deployedBytecode").get("object").asString();
            }
        }
        throw new IOException("no contract " + contract + " compiled from " + solFile);
    }

    /// Why running this contract would be misleading, or null when it is safe to run.
    ///
    /// [EvmContractRunner] installs the runtime bytecode without executing the creation code,
    /// which is only equivalent while the contract has no constructor and no initialized state
    /// variable — otherwise the run would start from an all-zero storage the real contract never
    /// has, and every verdict from it would be about a state that cannot occur.
    private static @Nullable String deploymentLogicOf(Path solFile, String contract)
            throws IOException {
        JsonNode root = new ObjectMapper().readTree(SolcWrapper.getJsonSolidity(solFile));
        for (JsonNode node : root.get("nodes").values()) {
            if (!contract.equals(text(node, "name"))) {
                continue;
            }
            for (JsonNode member : node.get("nodes").values()) {
                if ("FunctionDefinition".equals(text(member, "nodeType"))
                        && "constructor".equals(text(member, "kind"))) {
                    return "has a constructor, so a run from all-zero storage would not be the "
                        + "contract you deploy";
                }
                if ("VariableDeclaration".equals(text(member, "nodeType"))
                        && member.has("value") && !member.get("value").isNull()) {
                    return "initializes " + text(member, "name") + " at declaration, so a run "
                        + "from all-zero storage would not be the contract you deploy";
                }
            }
        }
        return null;
    }

    private static String text(JsonNode node, String field) {
        return node != null && node.has(field) ? node.get(field).asString() : "";
    }
}
