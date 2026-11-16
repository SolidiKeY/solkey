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

import org.key_project.solidity.program.parser.SolidityOutline;
import org.key_project.solidity.proof.init.SolidityProblemSynthesizer;
import org.key_project.solidity.testutil.ExampleDirs;
import org.key_project.solidity.testutil.SolidityExampleTests;

import org.apache.tuweni.bytes.Bytes;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class SolidityRuntimeExecutionTest {

    private static final Set<String> KNOWN_DIVERGENT = Set.of();

    private static final Set<Integer> PANICS_MODELED_AS_REVERT = Set.of(0x12, 0x31, 0x32);

    private record Fixture(SolidityOutline.Contract contract, EvmContractRunner runner,
            Path source) {
    }

    private static final Map<String, Fixture> FIXTURES = new ConcurrentHashMap<>();

    @ParameterizedTest(name = "{0}.{1}")
    @MethodSource("testSuiteExamples")
    void runsWithoutAssertFailure(String contract, String function) throws IOException {
        runCase(contract, function);
    }

    @ParameterizedTest(name = "{0}.{1}")
    @MethodSource("noCleanTailExamples")
    void noCleanTailExampleRunsWithoutAssertFailure(String contract, String function)
            throws IOException {
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
            fixture.runner().call(SolidityRuntimeCheck.calldata(fn, args));
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
        int panicCode = SolidityRuntimeCheck.panicCode(revertData);
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
            SolidityRuntimeCheck.PANIC_NAMES.getOrDefault(panicCode, "unknown"));
        if (KNOWN_DIVERGENT.contains(example)) {
            Assumptions.assumeTrue(false,
                example + ": " + panic + " — known unbounded-integer divergence");
        }
        if (box && PANICS_MODELED_AS_REVERT.contains(panicCode)) {
            Assumptions.assumeTrue(false, example + ": " + panic
                + " on fresh storage — the box proof closes on the revert branch, so the"
                + " runtime check is vacuous");
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

    static Stream<Arguments> testSuiteExamples() throws IOException {
        return SolidityExampleTests.contractFunctions(ExampleDirs.TACLETS)
                .filter(arguments -> !arguments.get()[1].toString().startsWith("unprovable"));
    }

    static Stream<Arguments> solcExamples() throws IOException {
        return SolidityExampleTests.contractFunctions(ExampleDirs.SEMANTICS);
    }

    static Stream<Arguments> noCleanTailExamples() throws IOException {
        return SolidityExampleTests.contractFunctions(ExampleDirs.NO_CLEAN_TAIL);
    }

    private static Path source(String contract) {
        for (String directory : List.of(ExampleDirs.TACLETS, ExampleDirs.NO_CLEAN_TAIL)) {
            Path source = SolidityExampleTests.examplesDir(directory).resolve(contract + ".sol");
            if (Files.exists(source)) {
                return source;
            }
        }
        return SolidityExampleTests.example(ExampleDirs.SEMANTICS + "/" + contract + ".sol");
    }

    private static Fixture fixture(String contract) {
        return FIXTURES.computeIfAbsent(contract, name -> {
            try {
                Path source = source(name);
                String deploymentLogic = SolidityRuntimeCheck.deploymentLogicOf(source, name);
                assertTrue(deploymentLogic == null, () -> name + " " + deploymentLogic
                    + "; the runtime harness needs a real deployment step for it");
                SolidityOutline.Contract outline =
                    SolidityOutline.of(source).contract(name).orElseThrow();
                return new Fixture(outline,
                    new EvmContractRunner(SolidityRuntimeCheck.runtimeBytecode(source, name)),
                    source);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }
}
