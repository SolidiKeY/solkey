/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.parser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Optional;

import org.key_project.solidity.program.parser.SolidityOutline.Span;
import org.key_project.solidity.testutil.SolidityExampleTests;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Checks the outline's source ranges and its account of what cannot be proved — the two things
/// the GUI's function picker renders.
public class SolidityOutlineTest {

    private static byte[] bytesOf(java.nio.file.Path file) throws IOException {
        return Files.readAllBytes(file);
    }

    /// Every function of the example suite slices back to its own declaration. solc reports byte
    /// offsets, and `TestSuite.sol` has non-ASCII section markers in its comments, so slicing the
    /// file as a `String` drifts by one character per marker passed.
    @Test
    void everyFunctionSlicesBackToItsDeclaration() throws IOException {
        var file = SolidityExampleTests.testSuite();
        byte[] source = bytesOf(file);
        var contract = SolidityOutline.of(file)
                .contract(SolidityExampleTests.TEST_SUITE_CONTRACT).orElseThrow();
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
        var file = SolidityExampleTests.testSuite();
        byte[] source = bytesOf(file);
        var function = SolidityOutline.of(file)
                .contract(SolidityExampleTests.TEST_SUITE_CONTRACT).orElseThrow()
                .function("additionStorageWrite").orElseThrow();

        String text = function.source().textIn(source);
        assertTrue(text.contains("@custom:key box"), text);
        assertTrue(text.contains("function additionStorageWrite("), text);
    }

    @Test
    void provableFunctionsReportNoReason() throws IOException {
        var contract = SolidityOutline.of(SolidityExampleTests.testSuite())
                .contract(SolidityExampleTests.TEST_SUITE_CONTRACT).orElseThrow();
        for (var function : contract.functions()) {
            assertEquals(function.unsupportedReason().isEmpty(), function.isProvable(),
                function.name() + " disagrees about being provable");
        }
    }

    @Test
    void aFunctionReturningAValueIsNotProvable() throws IOException {
        var function = SolidityOutline.of(SolidityExampleTests.example("functionBody/C.sol"))
                .contract("C").orElseThrow().function("expFctBdy").orElseThrow();

        assertFalse(function.isProvable());
        Optional<String> reason = function.unsupportedReason();
        assertTrue(reason.isPresent());
        assertTrue(reason.get().contains("returns a value"), reason.get());
    }

    @Test
    void aParameterWithoutAKeySortIsNotProvable() throws IOException {
        var contract = SolidityOutline.of(SolidityExampleTests.example("net/PiggyBankNet.sol"))
                .contract("PiggyBankNet").orElseThrow();

        String reason = contract.function("payTo").orElseThrow().unsupportedReason().orElseThrow();
        assertTrue(reason.contains("parameter a"), reason);
        assertTrue(reason.contains("address payable"), reason);
        assertTrue(contract.function("payOwner").orElseThrow().isProvable());
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
