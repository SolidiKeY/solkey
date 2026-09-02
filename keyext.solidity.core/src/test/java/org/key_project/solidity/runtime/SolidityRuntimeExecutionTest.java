/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.runtime;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import org.key_project.solidity.program.parser.SolcWrapper;
import org.key_project.solidity.program.parser.SolidityOutline;
import org.key_project.solidity.proof.init.SolidityProblemSynthesizer;
import org.key_project.solidity.testutil.SolidityExampleTests;

import org.apache.tuweni.bytes.Bytes;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/// Cross-checks the calculus against a real EVM: every provable example function of
/// `TestSuite.sol` and `solc/*.sol` is compiled with solc, deployed on an in-process Besu EVM,
/// and executed. A function whose proof closes must not hit a failing `assert` — Panic(0x01) —
/// when actually run.
///
/// Runs and proofs do not agree everywhere, and the verdicts reflect that:
/// - A box-tagged function whose `require` reverts on the fresh all-zero storage is vacuous at
/// runtime, exactly as the box modality treats it, and is skipped.
/// - Functions proved with KeY's unbounded integers may hit a checked-arithmetic Panic (0x11
/// overflow, ...) on the EVM; expected cases are listed in [#KNOWN_DIVERGENT].
/// - A parameterized function runs with the values its leading `require` pins, recovered by
/// [PinnedArguments].
///
/// The `TestSuite.sol` half runs in the common `test` task; the `solc/*.sol` half is tagged
/// `solidityExamples` and runs in the CI-only `testSolidityExamples` task.
public class SolidityRuntimeExecutionTest {

    /// Examples whose proofs rely on KeY's unbounded mathematical integers and therefore panic
    /// under the EVM's checked arithmetic. Keyed `Contract.function`; a Panic(0x01) entry would
    /// hide an assert violation and needs written justification.
    private static final Set<String> KNOWN_DIVERGENT = Set.of();

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

    private record Fixture(SolidityOutline.Contract contract, EvmContractRunner runner,
            Path source) {
    }

    private static final Map<String, Fixture> FIXTURES = new ConcurrentHashMap<>();

    @ParameterizedTest(name = "{0}.{1}")
    @MethodSource("testSuiteExamples")
    void runsWithoutAssertFailure(String contract, String function) throws IOException {
        runCase(contract, function);
    }

    @Tag("solidityExamples")
    @ParameterizedTest(name = "{0}.{1}")
    @MethodSource("solcExamples")
    void solcExampleRunsWithoutAssertFailure(String contract, String function)
            throws IOException {
        runCase(contract, function);
    }

    private static void runCase(String contract, String function) throws IOException {
        Fixture fixture = fixture(contract);
        SolidityOutline.Function fn = fixture.contract().function(function).orElseThrow();
        boolean box = fn.documentation().contains(SolidityProblemSynthesizer.BOX_DIRECTIVE);

        List<BigInteger> args = List.of();
        if (!fn.parameters().isEmpty()) {
            Optional<List<BigInteger>> pinned =
                PinnedArguments.of(fixture.source(), contract, fn);
            Assumptions.assumeTrue(pinned.isPresent(), () -> contract + "." + function
                + ": parameters not pinned by leading requires — cannot synthesize arguments");
            args = pinned.get();
        }

        EvmContractRunner.CallResult result =
            fixture.runner().call(Abi.signatureOf(fn), args);
        switch (result.status()) {
            case SUCCESS -> {
            }
            case EXCEPTIONAL_HALT -> fail(contract + "." + function
                + ": exceptional halt " + result.haltReason()
                + (result.haltReason().contains("INSUFFICIENT_GAS")
                        ? " — raise the gas limit in EvmContractRunner"
                        : ""));
            case REVERT -> judgeRevert(contract + "." + function, result.revertData(), box);
        }
    }

    private static void judgeRevert(String example, Bytes revertData, boolean box) {
        int panicCode = panicCode(revertData);
        if (panicCode < 0) {
            if (!box) {
                fail(example + ": require reverted (" + revertData
                    + ") but the function is not box-tagged, so its proof claims it never"
                    + " reverts");
            }
            Assumptions.assumeTrue(false, example
                + ": assumption require reverted on fresh storage — runtime check is vacuous");
        }
        String panic = "Panic(0x%02x %s)".formatted(panicCode,
            PANIC_NAMES.getOrDefault(panicCode, "unknown"));
        if (KNOWN_DIVERGENT.contains(example)) {
            Assumptions.assumeTrue(false,
                example + ": " + panic + " — known unbounded-integer divergence");
        }
        if (panicCode == 0x01) {
            fail(example + ": " + panic
                + " — assert violated at runtime but the proof closes;"
                + " possible calculus soundness gap");
        }
        fail(example + ": " + panic + " (revert data " + revertData + ")"
            + " — checked-arithmetic divergence; if the unbounded-integer proof is"
            + " intended, add the example to KNOWN_DIVERGENT");
    }

    /// The Panic code carried by `revertData`, or `-1` if it is not a Panic payload.
    private static int panicCode(Bytes revertData) {
        if (revertData.size() != 36 || !revertData.slice(0, 4).equals(PANIC_SELECTOR)) {
            return -1;
        }
        return revertData.slice(4).toUnsignedBigInteger().intValueExact();
    }

    static Stream<Arguments> testSuiteExamples() throws IOException {
        return examplesOf(List.of(SolidityExampleTests.TEST_SUITE_CONTRACT));
    }

    static Stream<Arguments> solcExamples() throws IOException {
        return examplesOf(solcContracts());
    }

    private static Stream<Arguments> examplesOf(List<String> contracts) throws IOException {
        Stream.Builder<Arguments> args = Stream.builder();
        for (String contract : contracts) {
            SolidityProblemSynthesizer.provableFunctions(source(contract), contract)
                    .stream()
                    .sorted()
                    .forEach(function -> args.add(Arguments.of(contract, function)));
        }
        return args.build();
    }

    private static List<String> solcContracts() throws IOException {
        try (Stream<Path> files = Files.list(SolidityExampleTests.examplesDir("solc"))) {
            return files.map(p -> p.getFileName().toString())
                    .filter(name -> name.endsWith(".sol"))
                    .map(name -> name.substring(0, name.length() - ".sol".length()))
                    .sorted()
                    .toList();
        }
    }

    private static Path source(String contract) {
        return contract.equals(SolidityExampleTests.TEST_SUITE_CONTRACT)
                ? SolidityExampleTests.testSuite()
                : SolidityExampleTests.example("solc/" + contract + ".sol");
    }

    private static Fixture fixture(String contract) {
        return FIXTURES.computeIfAbsent(contract, name -> {
            try {
                Path source = source(name);
                assertDirectRuntimeDeploymentIsSound(source, name);
                SolidityOutline.Contract outline =
                    SolidityOutline.of(source).contract(name).orElseThrow();
                return new Fixture(outline, new EvmContractRunner(runtimeBin(source, name)),
                    source);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private static String runtimeBin(Path source, String contract) throws IOException {
        JsonNode contracts = new ObjectMapper()
                .readTree(SolcWrapper.getCombinedBinJson(source)).get("contracts");
        for (Map.Entry<String, JsonNode> entry : contracts.properties()) {
            if (entry.getKey().endsWith(":" + contract)) {
                return entry.getValue().get("bin-runtime").asString();
            }
        }
        throw new IllegalArgumentException("no contract " + contract + " compiled from "
            + source + "; candidates: " + contracts.propertyNames());
    }

    /// [EvmContractRunner] installs the runtime bytecode without executing the creation code,
    /// which is only equivalent while the contract has no constructor and no initialized state
    /// variable. All examples satisfy this today; this guard turns a future violation into an
    /// explicit demand for a real deployment step instead of a wrong all-zero initial storage.
    private static void assertDirectRuntimeDeploymentIsSound(Path source, String contract)
            throws IOException {
        JsonNode root = new ObjectMapper().readTree(SolcWrapper.getJsonSolidity(source));
        for (JsonNode contractNode : root.get("nodes").values()) {
            if (!contract.equals(text(contractNode, "name"))) {
                continue;
            }
            for (JsonNode member : contractNode.get("nodes").values()) {
                boolean constructor = "FunctionDefinition".equals(text(member, "nodeType"))
                        && "constructor".equals(text(member, "kind"));
                boolean initializedStateVariable =
                    "VariableDeclaration".equals(text(member, "nodeType"))
                            && member.has("value") && !member.get("value").isNull();
                assertTrue(!constructor && !initializedStateVariable, contract
                    + " has deployment logic (constructor or state variable initializer);"
                    + " the runtime harness needs a real deployment step for it");
            }
        }
    }

    private static String text(JsonNode node, String field) {
        return node != null && node.has(field) ? node.get(field).asString() : "";
    }
}
