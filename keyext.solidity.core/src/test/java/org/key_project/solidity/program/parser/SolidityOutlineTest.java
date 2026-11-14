/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.parser;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import org.key_project.solidity.program.parser.SolidityOutline.Span;
import org.key_project.solidity.testutil.ExampleDirs;
import org.key_project.solidity.testutil.SolidityExampleTests;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Checks the outline's source ranges and its account of what cannot be proved — the two things
/// the GUI's function picker renders.
public class SolidityOutlineTest {

    static List<String> tacletContracts() throws IOException {
        return SolidityExampleTests.solContracts(ExampleDirs.TACLETS);
    }

    private static java.nio.file.Path tacletFile(String contract) {
        return SolidityExampleTests.example(ExampleDirs.TACLETS + "/" + contract + ".sol");
    }

    private static byte[] bytesOf(java.nio.file.Path file) throws IOException {
        return Files.readAllBytes(file);
    }

    /// Every function of the taclet examples slices back to its own declaration. solc reports
    /// byte offsets, and the `taclets/` files have non-ASCII section markers in their comments,
    /// so slicing a file as a `String` drifts by one character per marker passed.
    @ParameterizedTest(name = "{0}")
    @MethodSource("tacletContracts")
    void everyFunctionSlicesBackToItsDeclaration(String name) throws IOException {
        var file = tacletFile(name);
        byte[] source = bytesOf(file);
        var contract = SolidityOutline.of(file).contract(name).orElseThrow();
        assertFalse(contract.functions().isEmpty());

        for (var function : contract.functions()) {
            String text = function.source().textIn(source).strip();
            assertTrue(text.startsWith("///") || text.startsWith("function"),
                function.name() + " slices to: " + text);
            assertTrue(text.contains("function " + function.name() + "("),
                function.name() + " slices to: " + text);
            assertTrue(text.endsWith("}"), function.name() + " slices to: " + text);
        }
    }

    /// The span covers the natspec too: `@custom:key box` decides whether the obligation uses the
    /// box or the diamond modality, so it has to be visible next to the code it applies to.
    @Test
    void spanCoversTheNatspecDirective() throws IOException {
        var file = SolidityExampleTests.tacletSource("additionStorageWrite");
        byte[] source = bytesOf(file);
        var function = SolidityOutline.of(file)
                .contract(SolidityExampleTests.tacletContract("additionStorageWrite"))
                .orElseThrow()
                .function("additionStorageWrite").orElseThrow();

        String text = function.source().textIn(source);
        assertTrue(text.contains("@custom:key box"), text);
        assertTrue(text.contains("function additionStorageWrite("), text);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("tacletContracts")
    void provableFunctionsReportNoReason(String name) throws IOException {
        var contract = SolidityOutline.of(tacletFile(name)).contract(name).orElseThrow();
        for (var function : contract.functions()) {
            assertEquals(function.unsupportedReason().isEmpty(), function.isProvable(),
                function.name() + " disagrees about being provable");
        }
    }

    @Test
    void aNamedReturnIsProvableAnUnnamedOneIsNot(@TempDir Path dir) throws IOException {
        Path sol = dir.resolve("R.sol");
        Files.writeString(sol, """
                // SPDX-License-Identifier: GPL-2.0-only
                pragma solidity ^0.8.0;
                contract R {
                    function named(uint x) public returns (uint r) { r = x; }
                    function unnamed() public pure returns (uint) { return 1; }
                    function two() public pure returns (uint a, uint b) { a = 1; b = 2; }
                    function pair() public pure returns (uint, uint) { return (1, 2); }
                    /// @custom:key skip
                    function skipped() public {}
                }""");
        var contract = SolidityOutline.of(sol).contract("R").orElseThrow();

        var named = contract.function("named").orElseThrow();
        assertTrue(named.isProvable(), named.unsupportedReason().orElse(""));
        assertEquals("r", named.returns().get(0).name());

        Optional<String> reason = contract.function("unnamed").orElseThrow().unsupportedReason();
        assertTrue(reason.isPresent());
        assertTrue(reason.get().contains("returns a value"), reason.get());
        assertTrue(contract.function("two").orElseThrow().isProvable());
        assertTrue(contract.function("pair").orElseThrow().unsupportedReason().orElseThrow()
                .contains("returns a value"));
        assertTrue(contract.function("skipped").orElseThrow().unsupportedReason().orElseThrow()
                .contains("skip"));
        assertEquals(List.of("named", "two"),
            contract.provableFunctions().stream().map(SolidityOutline.Function::name).toList());
    }

    @Test
    void fileLevelTypesAndUnprovedDocumentationAreRecorded(@TempDir Path dir)
            throws IOException {
        Path sol = dir.resolve("T.sol");
        Files.writeString(sol, """
                // SPDX-License-Identifier: GPL-2.0-only
                pragma solidity ^0.8.0;
                enum Phase { A, B }
                struct P { uint a; }
                contract T {
                    enum Phase { X, Y, Z }
                    P p;
                    /// @custom:key ensures r == 1
                    function g() internal pure returns (uint r) { r = 1; }
                    /// @custom:key requires true
                    modifier m() { _; }
                    function f() public m { p.a = g(); }
                }""");
        var contract = SolidityOutline.of(sol).contract("T").orElseThrow();

        assertEquals(List.of("X", "Y", "Z"), contract.enums().get("Phase"));
        assertEquals(List.of("a"),
            contract.structs().get("P").stream().map(SolidityOutline.Variable::name).toList());
        assertEquals(List.of("internal function g", "modifier m"),
            List.copyOf(contract.otherDocumentation().keySet()));
    }

    @Test
    void theOutlineRecordsWhatSpecificationsNeed() throws IOException {
        var contract =
            SolidityOutline.of(SolidityExampleTests.example(ExampleDirs.SOLIDIKEY + "/Escrow.sol"))
                    .contract("Escrow").orElseThrow();

        assertTrue(contract.documentation().contains("@custom:key invariant sender != receiver"),
            contract.documentation());
        assertEquals(List.of("AwaitingDeposit", "DepositPlaced", "Withdrawn"),
            contract.enums().get("State"));
        assertEquals("enum Escrow.State", contract.stateVariables().stream()
                .filter(v -> v.name().equals("state")).findFirst().orElseThrow().type());
        var deposit = contract.function("placeInEscrow").orElseThrow();
        assertTrue(deposit.payable());
        assertFalse(contract.function("releaseEscrow").orElseThrow().payable());
        assertEquals(List.of("net(sender) == msg.value && state == State.DepositPlaced"),
            deposit.natspec().ensures());
    }

    @Test
    void constantsCarryTheirCompileTimeValue(@TempDir Path dir) throws IOException {
        Path sol = dir.resolve("K.sol");
        Files.writeString(sol, """
                // SPDX-License-Identifier: GPL-2.0-only
                pragma solidity ^0.8.0;
                uint constant FILE = LATER + 1;
                uint constant LATER = 2;
                contract K {
                    uint constant RATIONAL = 7 / 2 * 2;
                    uint constant CAST = uint(5);
                    uint constant MAXU = type(uint).max;
                    int8 constant MIN8 = type(int8).min;
                    address constant OWNER = address(0x1234);
                    bool constant FLAG = !false;
                    uint constant BIG = 10 ** 35;
                    uint constant DERIVED = FILE * CAST - 1;
                    uint constant QUOTIENT = CAST / 2;
                    uint constant OVERFLOW = MAXU + CAST;
                    uint plain = 3;
                }""");
        var values = new HashMap<String, String>();
        for (var variable : SolidityOutline.of(sol).contract("K").orElseThrow()
                .stateVariables()) {
            values.put(variable.name(), variable.constantValue());
        }

        assertEquals("7", values.get("RATIONAL"));
        assertEquals("5", values.get("CAST"));
        assertEquals(BigInteger.TWO.pow(256).subtract(BigInteger.ONE).toString(),
            values.get("MAXU"));
        assertEquals("-128", values.get("MIN8"));
        assertEquals("4660", values.get("OWNER"));
        assertEquals("true", values.get("FLAG"));
        assertEquals(BigInteger.TEN.pow(35).toString(), values.get("BIG"));
        assertEquals("14", values.get("DERIVED"));
        assertEquals("2", values.get("QUOTIENT"));
        assertNull(values.get("OVERFLOW"));
        assertNull(values.get("plain"));
    }

    @Test
    void aParameterWithoutAKeySortIsNotProvable(@TempDir Path dir) throws IOException {
        Path sol = dir.resolve("P.sol");
        Files.writeString(sol, """
                // SPDX-License-Identifier: GPL-2.0-only
                pragma solidity ^0.8.0;
                contract P {
                    function label(string memory text) public {}
                    function pay(address payable to, uint amount) external {}
                    function owner(address who) internal {}
                }""");
        var contract = SolidityOutline.of(sol).contract("P").orElseThrow();

        String reason = contract.function("label").orElseThrow().unsupportedReason().orElseThrow();
        assertTrue(reason.contains("parameter text"), reason);
        assertTrue(reason.contains("string"), reason);
        assertEquals(List.of("pay"), contract.provableFunctions().stream()
                .map(SolidityOutline.Function::name).toList());
    }

    @Test
    void malformedSpansYieldNoText() {
        byte[] source = "contract C {}".getBytes(StandardCharsets.UTF_8);
        assertEquals(new Span(12, 34), Span.parse("12:34:0"));
        assertEquals(Span.NONE, Span.parse(""));
        assertEquals(Span.NONE, Span.parse("garbage"));
        assertEquals(Span.NONE, Span.parse("a:b:c"));
        assertEquals(Span.NONE, Span.parse("12"));
        assertEquals(new Span(1, 2), Span.parse("1:2"));
        assertEquals("", Span.NONE.textIn(source));
        assertEquals("", new Span(9999, 4).textIn(source));
        assertEquals("contract", new Span(0, 8).textIn(source));
        assertEquals("C {}", new Span(9, 400).textIn(source));
    }

    /// Line numbers are counted over bytes, like the offsets they are derived from.
    @Test
    void linesAreCountedOverBytes() {
        byte[] source = "// ──\n// x\nfunction f() {}\n".getBytes(StandardCharsets.UTF_8);
        int charOffset = new String(source, StandardCharsets.UTF_8).indexOf("function");
        int byteOffset = source.length - 16;
        assertNotEquals(charOffset, byteOffset,
            "the marker has to be multi-byte for this to be a real check");
        assertEquals(3, new Span(byteOffset, 15).lineIn(source));
        assertEquals("function f() {}", new Span(byteOffset, 15).textIn(source));
    }
}
