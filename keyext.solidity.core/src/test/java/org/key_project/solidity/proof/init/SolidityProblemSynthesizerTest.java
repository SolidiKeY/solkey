/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.proof.init;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.key_project.prover.strategy.costbased.NumberRuleAppCost;
import org.key_project.prover.strategy.costbased.RuleAppCost;
import org.key_project.prover.strategy.costbased.TopRuleAppCost;
import org.key_project.solidity.CLI;
import org.key_project.solidity.control.GoalExplanation;
import org.key_project.solidity.control.ProofScript;
import org.key_project.solidity.control.ProofSession;
import org.key_project.solidity.control.SolidityVerifier;
import org.key_project.solidity.control.StepBudget;
import org.key_project.solidity.program.parser.SolidityOutline;
import org.key_project.solidity.program.parser.SoliditySources;
import org.key_project.solidity.proof.Goal;
import org.key_project.solidity.strategy.Strategy;
import org.key_project.solidity.testutil.ExampleDirs;
import org.key_project.solidity.testutil.SolidityExampleTests;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Checks what [SolidityProblemSynthesizer#resolve] refuses. A function the outline reports as
/// unprovable has to be rejected here with its reason: it would otherwise reach
/// [SolidityProblemSynthesizer#problemText], which renders a parameter without a `.key` sort as
/// `null a` and fails in the KeY parser with an unrelated message.
public class SolidityProblemSynthesizerTest {

    private static SolidityProblemSpec spec(String contract, String function) {
        return new SolidityProblemSpec(contract, function);
    }

    private static Path stringParameter(Path dir) throws IOException {
        Path file = dir.resolve("S.sol");
        Files.writeString(file, """
                // SPDX-License-Identifier: GPL-2.0-only
                pragma solidity ^0.8.0;
                contract S {
                    function label(string memory text) public {}
                }""");
        return file;
    }

    @Test
    void unsignedParameterFactsPreserveSignedInputs(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("ParameterFacts.sol");
        Files.writeString(file, """
                pragma solidity ^0.8.0;
                contract ParameterFacts {
                    function signedInput(int x) public pure {
                        assert(x >= 0);
                    }
                    /// @custom:key box
                    function unsignedInput(uint8 x) public pure {
                        assert(x >= 0);
                    }
                    function unsignedDiamond(uint x) public pure {
                        assert(x >= 0);
                    }
                }
                """);
        var signed = SolidityVerifier.verify(file, spec("ParameterFacts", "signedInput"),
            10000, -1, 3);
        assertEquals(SolidityVerifier.Status.OPEN, signed.status());
        for (String function : List.of("unsignedInput", "unsignedDiamond")) {
            var outcome =
                SolidityVerifier.verify(file, spec("ParameterFacts", function), 10000, -1, 3);
            assertEquals(SolidityVerifier.Status.PROVED, outcome.status(), function);
        }
    }

    @Test
    void discardedArithmeticStillRequiresNormalCompletion(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("Discard.sol");
        Files.writeString(file, """
                pragma solidity ^0.8.0;
                contract Discard {
                    function divide() public pure {
                        uint numerator = 1;
                        uint denominator = 0;
                        numerator / denominator;
                    }
                    function modulo() public pure {
                        uint numerator = 1;
                        uint denominator = 0;
                        numerator % denominator;
                    }
                    function compound() public pure {
                        uint value = 1;
                        uint denominator = 0;
                        uint result = (value /= denominator);
                        assert(result == value);
                    }
                }
                """);
        for (String function : List.of("divide", "modulo", "compound")) {
            var outcome = SolidityVerifier.verify(file, spec("Discard", function), 10000, -1, 3);
            assertEquals(SolidityVerifier.Status.OPEN, outcome.status(), function);
        }
    }

    @Test
    void aParameterWithoutAKeySortIsRefusedWithItsReason(@TempDir Path dir) throws IOException {
        Path file = stringParameter(dir);

        var e = assertThrows(IllegalArgumentException.class,
            () -> SolidityProblemSynthesizer.resolve(file, spec("S", "label")));

        assertTrue(e.getMessage().contains("S.label"), e.getMessage());
        assertTrue(e.getMessage().contains("cannot be proved"), e.getMessage());
        assertTrue(e.getMessage().contains("string"), e.getMessage());
    }

    @Test
    void anAddressParameterIsDeclaredAsAnInt() throws IOException {
        Path file = SolidityExampleTests.example(ExampleDirs.SOLIDIKEY_PLAIN + "/PiggyBankNet.sol");

        String text =
            SolidityProblemSynthesizer.problemText(file, spec("PiggyBankNet", "payToPlus"));

        assertTrue(text.contains("    int a;\n    int x;\n"), text);
        assertTrue(text.contains("payToPlus(a, x)@PiggyBankNet;"), text);
    }

    @Test
    void aFunctionReturningAnUnnamedValueIsRefusedWithItsReason(@TempDir Path dir)
            throws IOException {
        Path file = dir.resolve("R.sol");
        Files.writeString(file, """
                // SPDX-License-Identifier: GPL-2.0-only
                pragma solidity ^0.8.0;
                contract R {
                    function unnamed() public pure returns (uint) { return 1; }
                }""");

        var e = assertThrows(IllegalArgumentException.class,
            () -> SolidityProblemSynthesizer.resolve(file, spec("R", "unnamed")));

        assertTrue(e.getMessage().contains("cannot be proved"), e.getMessage());
        assertTrue(e.getMessage().contains("returns a value"), e.getMessage());
    }

    @Test
    void aConstructorStartsFromEmptyStorageAndAssumesNoInvariant() throws IOException {
        Path file = SolidityExampleTests.example(ExampleDirs.SOLIDIKEY + "/Counter.sol");

        String text = SolidityProblemSynthesizer.problemText(file, spec("Counter", "constructor"));

        assertTrue(text.contains("storage := mtSt"), text);
        assertTrue(text.contains("constructor(start)@Counter;"), text);
        assertFalse(text.contains("& CInv(storage, net) ->"), text);
    }

    @Test
    void severalNamedReturnsAreBoundByNameAndProved() throws IOException {
        Path file = Path.of("/nonexistent-solkey-dir/Ordered.sol");
        SoliditySources.register(file, """
                // SPDX-License-Identifier: GPL-2.0-only
                pragma solidity ^0.8.0;
                contract Ordered {
                    /// @custom:key ensures lo <= hi
                    /// @custom:key ensures lo == x || lo == y
                    function order(uint x, uint y) public pure returns (uint lo, uint hi) {
                        if (x < y) return (x, y);
                        return (y, x);
                    }
                    /// @custom:key ensures lo < hi
                    function strict(uint x, uint y) public pure returns (uint lo, uint hi) {
                        if (x < y) return (x, y);
                        return (y, x);
                    }
                }""");

        String text = SolidityProblemSynthesizer.problemText(file, spec("Ordered", "order"));

        assertTrue(text.contains("int result_lo;") && text.contains("int result_hi;"), text);
        assertTrue(text.contains("\\[{ (result_lo, result_hi) = order(x, y)@Ordered; }\\]"),
            text);
        assertTrue(text.contains("(result_lo <= result_hi)"), text);
        assertTrue(SolidityVerifier.verify(file, spec(null, "order"), 10000, -1, 3).closed());
        assertFalse(SolidityVerifier.verify(file, spec(null, "strict"), 10000, -1, 3).closed());
    }

    @Test
    void anUnspecifiedFunctionKeepsThePlainObligation() throws IOException {
        String function = "storagePushLengthPositive";
        Path file = SolidityExampleTests.tacletSource(function);
        String contract = SolidityExampleTests.tacletContract(function);

        String text = SolidityProblemSynthesizer.problemText(file, spec(contract, function));

        assertTrue(text.contains("\\find(lenInv(s))"), text);
        assertTrue(text.contains(
            "conforms(fieldType(%1$s$copyTargetDyn), selectSt<[Struct]>(s, %1$s$copyTargetDyn))"
                    .formatted(contract)),
            text);
        assertFalse(text.contains("conforms(fieldType(%s$copySource9)".formatted(contract)), text);
        assertTrue(text.endsWith("""
                \\problem {
                    lenInv(storage) ->
                    \\<{ %s()@%s; }\\>(true)
                }
                """.formatted(function, contract)), text);
    }

    @Test
    void aConstructorStartsFromTheEmptyStorage()
            throws IOException {
        Path file = SolidityExampleTests.example(ExampleDirs.SOLIDIKEY + "/FixedLengths.sol");

        String text =
            SolidityProblemSynthesizer.problemText(file, spec("FixedLengths", "constructor"));

        assertTrue(text.contains("{storage := mtSt\n     || net := mtSt}\n    "
            + "\\<{ constructor()@FixedLengths; }\\>(true)"), text);
        assertTrue(text.contains("conforms(fieldType(FixedLengths$dyn), selectSt<[Struct]>(s, "
            + "FixedLengths$dyn))"), text);
        assertFalse(text.contains("conforms(fieldType(FixedLengths$d1)"), text);
    }

    @Test
    void aSpecifiedFunctionGetsTheInvariantTacletAndTheLedgerObligation() throws IOException {
        Path file = SolidityExampleTests.example(ExampleDirs.SOLIDIKEY + "/Escrow.sol");

        String text = SolidityProblemSynthesizer.problemText(file, spec("Escrow", "placeInEscrow"));

        assertEquals(
            """
                    \\programSource "%s";

                    \\rules {
                        insertCInv {
                            \\schemaVar \\term Struct cinv$s, cinv$n;
                            \\find(CInv(cinv$s, cinv$n))
                            \\replacewith(
                                // sender != receiver :
                                !(find<[int]>(cinv$s, cons1(Escrow$sender)) = find<[int]>(cinv$s, cons1(Escrow$receiver)))
                                // amountInEscrow == net(sender) + net(receiver) :
                                & (find<[int]>(cinv$s, cons1(Escrow$amountInEscrow)) = (selectSt<[int]>(cinv$n, at(find<[int]>(cinv$s, cons1(Escrow$sender)))) + selectSt<[int]>(cinv$n, at(find<[int]>(cinv$s, cons1(Escrow$receiver))))))
                                // state != State.AwaitingDeposit || net(sender) == 0 :
                                & (!(find<[int]>(cinv$s, cons1(Escrow$state)) = 0) | (selectSt<[int]>(cinv$n, at(find<[int]>(cinv$s, cons1(Escrow$sender)))) = 0))
                                // state == State.DepositPlaced || amountInEscrow == 0 :
                                & ((find<[int]>(cinv$s, cons1(Escrow$state)) = 1) | (find<[int]>(cinv$s, cons1(Escrow$amountInEscrow)) = 0)))
                            \\heuristics(simplify)
                        };
                        insertLenInv {
                            \\schemaVar \\term Struct s;
                            \\find(lenInv(s))
                            \\replacewith(true)
                            \\heuristics(simplify)
                        };
                    }

                    \\problem {
                        // msg.value >= 0 :
                        geq(msgValue, 0)
                        // msg.sender != this :
                        & !(msgSender = self)
                        // msg.sender is an address :
                        & ((0 <= msgSender) & (msgSender <= 1461501637330902918203684832716283019655932542975))
                        // msg.sender == sender && state == State.AwaitingDeposit && msg.value > 0 :
                        & (((msgSender = find<[int]>(storage, cons1(Escrow$sender))) & (find<[int]>(storage, cons1(Escrow$state)) = 0)) & (msgValue > 0))
                        & CInv(storage, net) ->
                        {net := storeSt(net, at(msgSender), (selectSt<[int]>(net, at(msgSender)) + msgValue))
                         || selfBalance := (selfBalance + msgValue)}
                        \\[{ placeInEscrow()@Escrow; }\\]
                            (CInv(storage, net)
                             // net(sender) == msg.value && state == State.DepositPlaced :
                             & ((selectSt<[int]>(net, at(find<[int]>(storage, cons1(Escrow$sender)))) = msgValue) & (find<[int]>(storage, cons1(Escrow$state)) = 1)))
                    }
                    """
                    .formatted(file.toAbsolutePath()),
            text);
    }

    @Test
    void oldAndQuantifiersDeclareWhatTheyNeed() throws IOException {
        Path file = SolidityExampleTests.example(ExampleDirs.SOLIDIKEY + "/MultiAuction.sol");

        String text = SolidityProblemSynthesizer.problemText(file,
            spec("MultiAuction", "placeOrIncreaseBid"));

        assertTrue(text.contains("    Struct old;\n    Struct oldNet;\n"), text);
        assertTrue(text.contains("{old := storage\n     || oldNet := net\n     || net := storeSt("),
            text);
        assertFalse(text.contains("\\schemaVar \\variables"), text);
        assertTrue(text.contains("(\\exists int hb; (((0 <= hb) & (hb <= "), text);
        assertTrue(text.contains("\\varcond(\\noFreeVarIn(cinv$s), \\noFreeVarIn(cinv$n))"),
            text);
        assertTrue(text.contains("find<[int]>(old, cons2(MultiAuction$balances, at(msgSender)))"),
            text);
        assertTrue(text.contains("// msg.value >= 0 :\n    geq(msgValue, 0)"), text);

        String nonPayable = SolidityProblemSynthesizer.problemText(file,
            spec("MultiAuction", "withdraw"));
        assertTrue(nonPayable.contains("// msg.value == 0 :\n    (msgValue = 0)"), nonPayable);
    }

    @Test
    void aBrokenClauseNamesTheFunctionAndTheClause(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("B.sol");
        Files.writeString(file, """
                // SPDX-License-Identifier: GPL-2.0-only
                pragma solidity ^0.8.0;
                contract B {
                    uint n;
                    /// @custom:key ensures nope == 1
                    function f() public { n = 1; }
                }""");

        var e = assertThrows(IllegalArgumentException.class,
            () -> SolidityProblemSynthesizer.problemText(file, spec("B", "f")));

        assertTrue(e.getMessage().contains("B.f ensures"), e.getMessage());
        assertTrue(e.getMessage().contains("unknown identifier nope"), e.getMessage());
    }

    @Test
    void aClauseWhereNoObligationReadsItIsRejected(@TempDir Path dir) throws IOException {
        Map<String, String> misplaced = Map.of(
            "/// @custom:key ensures x == 0\ncontract M { uint x;\n"
                + "function f() public { x = 1; } }",
            "ensures is not allowed on contract M",
            "contract M { uint x;\n/// @custom:key invariant x == 0\n"
                + "function f() public { x = 1; } }",
            "invariant is not allowed on function f",
            "contract M { uint x;\n/// @custom:key ensures r == 0\n"
                + "function g() internal returns (uint r) { r = 1; }\n"
                + "function f() public { x = g(); } }",
            "ensures is not allowed on internal function g",
            "contract M { uint x;\n/// @custom:key requires x == 0\n"
                + "modifier m() { _; }\nfunction f() public m { x = 1; } }",
            "requires is not allowed on modifier m");
        for (var entry : misplaced.entrySet()) {
            Path file = dir.resolve("M.sol");
            Files.writeString(file, "// SPDX-License-Identifier: GPL-2.0-only\n"
                + "pragma solidity ^0.8.0;\n" + entry.getKey());
            var e = assertThrows(IllegalArgumentException.class,
                () -> SolidityProblemSynthesizer.problemText(file, spec("M", "f")));
            assertTrue(e.getMessage().contains(entry.getValue()), e.getMessage());
        }
    }

    @Test
    void anInvariantMayNameItsQuantifiedVariablesSOrN(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("S.sol");
        Files.writeString(file, """
                // SPDX-License-Identifier: GPL-2.0-only
                pragma solidity ^0.8.0;
                /// @custom:key invariant \\forall uint s; \\forall uint n; m[s] == m[n] || s != n
                contract S {
                    mapping(uint => uint) m;
                    /// @custom:key ensures r == m[k]
                    function get(uint k) public view returns (uint r) { r = m[k]; }
                }""");

        var result = SolidityVerifier.verify(file, spec("S", "get"), 10000, -1, 3);

        assertTrue(result.closed(), String.valueOf(result));
    }

    @Test
    void anUnknownFunctionStillListsTheCandidates() {
        Path file = SolidityExampleTests.tacletSource("testSimpleAssert");
        String contract = SolidityExampleTests.tacletContract("testSimpleAssert");

        var e = assertThrows(IllegalArgumentException.class,
            () -> SolidityProblemSynthesizer.resolve(file, spec(contract, "noSuchFunction")));

        assertTrue(e.getMessage().contains("has no public function noSuchFunction"),
            e.getMessage());
        assertTrue(e.getMessage().contains("testSimpleAssert"), e.getMessage());
    }

    @Test
    void aProvableFunctionResolvesToItself() throws IOException {
        Path file = SolidityExampleTests.tacletSource("testSimpleAssert");
        String contract = SolidityExampleTests.tacletContract("testSimpleAssert");

        assertEquals(spec(contract, "testSimpleAssert"),
            SolidityProblemSynthesizer.resolve(file, spec(contract, "testSimpleAssert")));
        assertEquals(spec(contract, "testSimpleAssert"),
            SolidityProblemSynthesizer.resolve(file, spec(null, "testSimpleAssert")));
    }

    /// The overload the GUI uses, so it does not fork solc a second time for a file it has already
    /// read, has to agree with the one that reads the file itself.
    @Test
    void theOutlineOverloadAgreesWithThePathOne(@TempDir Path dir) throws IOException {
        Path file = SolidityExampleTests.tacletSource("testSimpleAssert");
        SolidityOutline outline = SolidityOutline.of(file);
        SolidityProblemSpec requested =
            spec(SolidityExampleTests.tacletContract("testSimpleAssert"), "testSimpleAssert");

        assertEquals(SolidityProblemSynthesizer.resolve(file, requested),
            SolidityProblemSynthesizer.resolve(file, outline, requested));

        Path unsupported = stringParameter(dir);
        SolidityOutline unsupportedOutline = SolidityOutline.of(unsupported);
        var e = assertThrows(IllegalArgumentException.class, () -> SolidityProblemSynthesizer
                .resolve(unsupported, unsupportedOutline, spec("S", "label")));
        assertTrue(e.getMessage().contains("cannot be proved"), e.getMessage());
    }

    @Test
    void aSourceRegisteredInMemoryIsProvedWithoutAFile() throws IOException {
        Path file = Path.of("/nonexistent-solkey-dir/InMemory.sol");
        SoliditySources.register(file, """
                // SPDX-License-Identifier: GPL-2.0-only
                pragma solidity ^0.8.0;
                contract InMemory {
                    function holds() public pure {
                        uint8 x = 1;
                        assert(x == 1);
                    }
                    function fails() public pure {
                        uint8 x = 1;
                        assert(x == 2);
                    }
                }""");

        assertEquals(java.util.List.of("holds", "fails"), SolidityVerifier.functions(file, null));
        var holds = SolidityVerifier.verify(file, spec(null, "holds"), 10000, -1, 3);
        var fails = SolidityVerifier.verify(file, spec(null, "fails"), 10000, -1, 3);

        assertTrue(holds.closed(), String.valueOf(holds));
        assertFalse(fails.closed(), String.valueOf(fails));
        assertEquals(null, fails.error());
        assertFalse(fails.openGoals().isEmpty());
        assertEquals(null, holds.proof());

        var saved = SolidityVerifier.verify(file, spec(null, "holds"), 10000, -1, 3, true);
        assertTrue(saved.proof() != null && saved.proof().contains("\\proof"),
            String.valueOf(saved.proof()));
    }

    @Test
    void modifierContinuationsDoNotProveFalseAssertions(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("ModifierReturns.sol");
        Files.writeString(file, """
                pragma solidity ^0.8.0;
                contract ModifierReturns {
                    uint x;
                    modifier twice() { _; _; }
                    modifier outer() { _; x += 100; }
                    modifier inner() { _; x += 10; if (x == 11) return; x = 999; }
                    modifier repeated() {
                        for (uint i; i < 3; ++i) { _; x += 10; }
                    }
                    function body() internal pure twice returns (uint r) {
                        r += 1;
                        return r;
                    }
                    function framed() internal outer inner { x = 1; return; }
                    function looped() internal repeated returns (uint r) {
                        r += 1;
                        return r;
                    }
                    function twiceTrue() public pure { uint r = body(); assert(r == 2); }
                    function twiceFalse() public pure { uint r = body(); assert(r == 1); }
                    function frameTrue() public { framed(); assert(x == 111); }
                    function frameFalse() public { framed(); assert(x == 11); }
                    function loopTrue() public {
                        x = 0; uint r = looped(); assert(r == 3); assert(x == 30);
                    }
                    function loopFalse() public {
                        x = 0; uint r = looped(); assert(r == 1);
                    }
                }
                """);
        for (String prefix : List.of("twice", "frame", "loop")) {
            var holds = SolidityVerifier.verify(file, spec(null, prefix + "True"), 10000, -1, 3);
            var fails = SolidityVerifier.verify(file, spec(null, prefix + "False"), 10000, -1, 3);
            assertTrue(holds.closed(), String.valueOf(holds));
            assertEquals(SolidityVerifier.Status.OPEN, fails.status(), String.valueOf(fails));
            assertNull(fails.error());
        }
    }

    @Test
    void loopInvariantNamesResolveInTheScopeOfTheLoop(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("LoopScopes.sol");
        Files.writeString(file, """
                pragma solidity ^0.8.0;
                contract LoopScopes {
                    uint x;
                    modifier setsFirst(uint[] memory a) { a[0] = 9; _; }
                    function markFirst(uint[] memory a) internal pure setsFirst(a) {}
                    /// @custom:key box
                    function bodyLocalTrue(uint n) public pure {
                        uint t = 5;
                        uint i = 0;
                        /// @custom:key invariant i <= n && t == 5
                        while (i < n) { uint t = 7; t += 1; i++; }
                        assert(t == 5);
                    }
                    /// @custom:key box
                    function closedBlockTrue(uint n) public pure {
                        uint t = 5;
                        { uint t = 8; t += 1; }
                        /// @custom:key invariant i <= n && t == 5
                        for (uint i = 0; i < n; i++) {}
                        assert(t == 5);
                    }
                    /// @custom:key box
                    function stateVariableTrue(uint n) public {
                        require(n >= 1);
                        x = 5;
                        uint i = 0;
                        /// @custom:key invariant i <= n && x == 5
                        do { uint x = 7; x += 1; i++; } while (i < n);
                        assert(x == 5);
                    }
                    /// @custom:key box
                    function modifierWriteFalse(uint n) public pure {
                        require(n == 1);
                        uint[] memory arr = new uint[](1);
                        uint i = 0;
                        /// @custom:key invariant i <= n
                        while (i < n) { markFirst(arr); i++; }
                        assert(arr[0] == 0);
                    }
                }
                """);
        for (String function : List.of("bodyLocalTrue", "closedBlockTrue", "stateVariableTrue")) {
            var holds = SolidityVerifier.verify(file, spec(null, function), 20000, -1, 3);
            assertTrue(holds.closed(), String.valueOf(holds));
        }
        var fails = SolidityVerifier.verify(file, spec(null, "modifierWriteFalse"), 20000, -1, 3);
        assertEquals(SolidityVerifier.Status.OPEN, fails.status(), String.valueOf(fails));
        assertNull(fails.error());
    }

    private static Path inMemory(String name) {
        Path file = Path.of("/nonexistent-solkey-dir/" + name + ".sol");
        SoliditySources.register(file, """
                // SPDX-License-Identifier: GPL-2.0-only
                pragma solidity ^0.8.0;
                contract %s {
                    function holds() public pure {
                        uint8 x = 1;
                        assert(x == 1);
                    }
                    function fails() public pure {
                        uint8 x = 1;
                        assert(x == 2);
                    }
                }""".formatted(name));
        return file;
    }

    @Test
    void aMistypedTacletOptionIsRejectedWithTheKnownChoices() {
        Path file = inMemory("Typo");
        var outcome = SolidityVerifier.verify(file,
            new SolidityProblemSpec(null, "holds",
                java.util.List.of("transferSemantics:withCalback")),
            ProofSession.Limits.defaults(), 3, false);

        assertFalse(outcome.closed());
        assertTrue(String.valueOf(outcome.error()).contains("known choices"), outcome.error());
    }

    @Test
    void strategySettingsAreValidatedAndApplied() {
        Path file = inMemory("Strategy");
        var model = SolidityVerifier.verify(file, spec(null, "holds"),
            new ProofSession.Limits(10000, -1, java.util.Map.of(
                org.key_project.solidity.strategy.StrategyProperties.NON_LIN_ARITH_OPTIONS_KEY,
                org.key_project.solidity.strategy.StrategyProperties.NON_LIN_ARITH_COMPLETION)),
            3, false);
        var bad = SolidityVerifier.verify(file, spec(null, "holds"),
            new ProofSession.Limits(10000, -1,
                java.util.Map.of("SPLITTING_OPTIONS_KEY", "SOMETIMES")),
            3, false);

        assertTrue(model.closed(), String.valueOf(model));
        assertTrue(model.summary().nodes() > 1, String.valueOf(model.summary()));
        assertTrue(String.valueOf(bad.error()).contains("no strategy setting"), bad.error());
    }

    @Test
    void theGeneratedProblemIsShown() throws IOException {
        assertTrue(SolidityVerifier.problem(inMemory("Problem"), spec(null, "holds"))
                .contains("\\problem"));
    }

    @Test
    void aSourceSessionCanBeInspectedBeforeSearchAndResumed() throws Exception {
        try (var session = ProofSession.open(inMemory("Bounded"), spec(null, "holds"),
            new ProofSession.Limits(1, -1, Map.of()))) {
            int root = session.tree().getFirst().serial();
            assertEquals(1, session.summary().nodes());
            assertNull(session.lastSearch());
            assertNotNull(session.sequent(root));
            String sequent = session.node(root, true).sequent();
            int offset = sequent.indexOf("holds");
            var term = session.termAt(root, offset);
            assertNotNull(term, sequent);
            assertNotNull(term.op());
            assertNotNull(term.sort());
            assertNull(session.termAt(root, -1));
            var rules = session.ruleDiagnosticsAt(root, offset);
            assertFalse(rules.isEmpty(), sequent);
            assertTrue(rules.stream().anyMatch(r -> r.rule().missing().isEmpty()));

            var bounded = session.runAutoDetailed(null);
            assertEquals(ProofSession.StopReason.STEP_LIMIT, bounded.stopReason());
            assertEquals(1, bounded.appliedRules());
            assertSame(bounded, session.lastSearch());
            session.configure(ProofSession.Limits.defaults());
            assertEquals(ProofSession.StopReason.PROVED,
                session.runAutoDetailed(null).stopReason());
        }
    }

    @Test
    void rulesAreAppliedByNameAtATextPosition() throws Exception {
        assertEquals(new ProofSession.Limits(1, 500, Map.of("a", "b")),
            ProofSession.Limits.steps(1).withTimeoutMillis(500).withStrategy("a", "b"));
        try (var session =
            ProofSession.open(inMemory("ByName"), spec(null, "holds"),
                ProofSession.Limits.steps(1))) {
            int root = session.root();
            assertEquals(session.node(root, true).sequent(), session.node(root).sequent());
            assertNotNull(session.termAt(root, "holds"));
            assertFalse(session.ruleDiagnosticsAt(root, "holds").isEmpty());
            assertThrows(IllegalArgumentException.class, () -> session.offsetOf(root, "absent"));
            assertThrows(IllegalArgumentException.class,
                () -> session.applyRule(root, "holds", "noSuchRule"));

            var summary = session.applyRule(root, "holds", "functionBodyExpand");
            assertEquals(2, summary.nodes());
            assertEquals("functionBodyExpand", session.node(root).ruleName());
        }
    }

    @Test
    void aScriptAppliesRulesByNameAndRunsTheStrategy() throws Exception {
        try (var session = ProofSession.open(inMemory("Script"), spec(null, "holds"),
            ProofSession.Limits.defaults())) {
            var out = new ByteArrayOutputStream();
            new ProofScript(session, new PrintStream(out, true)).run("""
                    rules on "holds"; rule functionBodyExpand on "holds"
                    auto 5; auto""");
            String text = out.toString();
            assertTrue(text.contains("functionBodyExpand"), text);
            assertTrue(text.contains("applied functionBodyExpand"), text);
            assertTrue(text.contains("STEP_LIMIT, 5 rules applied"), text);
            assertTrue(session.summary().closed(), text);
            assertThrows(IllegalArgumentException.class,
                () -> new ProofScript(session, new PrintStream(out)).run("frobnicate"));
        }
    }

    @Test
    void whyNamesTheStatementNoRuleExecutes() throws Exception {
        Path file = Path.of("/nonexistent-solkey-dir/Stuck.sol");
        SoliditySources.register(file, """
                // SPDX-License-Identifier: GPL-2.0-only
                pragma solidity ^0.8.0;
                contract Stuck {
                    uint[] dyn;
                    function assignNew() public {
                        uint n = 10;
                        dyn = new uint[](n + 1);
                    }
                }""");
        try (var session = ProofSession.open(file, spec(null, "assignNew"),
            ProofSession.Limits.defaults())) {
            session.runAutoDetailed(null);
            var why = GoalExplanation.explain(session.proof(), 3);
            assertEquals(1, why.size());
            assertFalse(why.get(0).lastRules().isEmpty());
            var stuck = why.get(0).stuck().get(0);
            assertEquals(GoalExplanation.Kind.PROGRAM, stuck.kind());
            assertTrue(stuck.term().contains("new uint[](n + 1)"), stuck.term());
        }
    }

    @Test
    void theCliReportsJsonBaselinesAndOneLineErrors(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("Cli.sol");
        Files.writeString(file, """
                // SPDX-License-Identifier: GPL-2.0-only
                pragma solidity ^0.8.0;
                contract Cli {
                    function holds() public pure {
                        uint8 x = 1;
                        assert(x == 1);
                    }
                    function fails() public pure {
                        uint8 x = 1;
                        assert(x == 2);
                    }
                }""");
        String json = cli(1, file.toString(), "--json", "--why");
        List<String> lines = json.lines().toList();
        assertEquals(3, lines.size(), json);
        assertTrue(lines.get(0).contains("\"status\":\"PROVED\""), json);
        assertTrue(lines.get(1).contains("\"stopReason\":\"NO_APPLICABLE_RULE\""), json);
        assertTrue(lines.get(1).contains("\"why\""), json);
        assertTrue(lines.get(2).contains("\"closed\":1"), json);

        Path baseline = dir.resolve("baseline.txt");
        cli(0, file.toString(), "--quiet", "--baseline", baseline.toString());
        StepBudget recorded = StepBudget.load(baseline);
        StepBudget.Entry holds = recorded.entry("Cli", "holds");
        assertEquals("PROVED", holds.status());
        assertTrue(holds.steps() > 0);
        assertEquals("NO_APPLICABLE_RULE", recorded.entry(null, "Cli.fails").stopReason());
        assertEquals(holds.steps() + holds.steps() / 2 + StepBudget.FLOOR, holds.budget());
        assertTrue(cli(0, file.toString(), "--quiet", "--baseline", baseline.toString())
                .contains("baseline: 0 changed, 0 regressed"));
        assertTrue(cli(1, file.toString(), "--json", "--budget", baseline.toString())
                .contains("\"budget\":" + holds.budget()));
        Files.write(baseline, List.of("Cli.holds PROVED 3 STEP_LIMIT", "Cli.fails PROVED"));
        assertTrue(cli(1, file.toString(), "--quiet", "--baseline", baseline.toString())
                .contains("REGRESSED fails: PROVED -> OPEN"));
        assertTrue(cli(1, file.toString(), "-f", "holds", "--json", "--budget",
            baseline.toString()).contains("\"stopReason\":\"STEP_LIMIT\""));
        assertEquals(StepBudget.MIN_ESTIMATE, StepBudget.estimate(10));
        assertEquals(StepBudget.STEPS_PER_SOURCE_BYTE * 1000, StepBudget.estimate(1000));
        assertEquals(StepBudget.MAX_ESTIMATE, StepBudget.estimate(1_000_000));
        assertTrue(cli(1, file.toString(), "--json", "--budget").contains(
            "\"budget\":" + StepBudget.estimate(file, "Cli", "holds")));

        Path broken = dir.resolve("Broken.sol");
        Files.writeString(broken, "contract Broken { function f() public { uint x = ; } }");
        String error = cli(1, broken.toString());
        assertTrue(error.startsWith("Error: " + broken + ":1:"), error);
        assertTrue(error.contains("ParserError: "), error);
        assertEquals(2, error.lines().count(), error);
    }

    private static String cli(int expectedExit, String... args) {
        PrintStream out = System.out;
        PrintStream err = System.err;
        var buffer = new ByteArrayOutputStream();
        var stream = new PrintStream(buffer, true);
        System.setOut(stream);
        System.setErr(stream);
        try {
            assertEquals(expectedExit, CLI.execute(args), buffer::toString);
        } finally {
            System.setOut(out);
            System.setErr(err);
        }
        return buffer.toString();
    }

    @Test
    void timeoutAndExhaustedSearchAreDistinctFromAnError() throws Exception {
        try (var session = ProofSession.open(inMemory("Timeout"), spec(null, "fails"),
            new ProofSession.Limits(10000, 0, Map.of()))) {
            var timedOut = session.runAutoDetailed(null);
            assertEquals(ProofSession.StopReason.TIMEOUT, timedOut.stopReason());
            assertEquals(0, timedOut.appliedRules());
            assertNull(timedOut.exception());
            session.configure(ProofSession.Limits.defaults());
            var exhausted = session.runAutoDetailed(null);
            assertEquals(ProofSession.StopReason.NO_APPLICABLE_RULE, exhausted.stopReason());
            assertNull(exhausted.exception());
            assertFalse(session.summary().closed());
        }
    }

    @Test
    void closingOneSubtreeDoesNotReportTheWholeProofAsProved() throws Exception {
        Path file = Path.of("/nonexistent-solkey-dir/Branches.sol");
        SoliditySources.register(file, """
                // SPDX-License-Identifier: GPL-2.0-only
                pragma solidity ^0.8.0;
                contract Branches {
                    function split(bool condition) public pure {
                        if (condition) {
                            assert(condition);
                        } else {
                            assert(!condition);
                        }
                    }
                }""");
        try (var session = ProofSession.open(file, spec(null, "split"),
            new ProofSession.Limits(1, -1, Map.of()))) {
            for (int i = 0; i < 100 && session.summary().openGoals() == 1; i++) {
                session.runAutoDetailed(null);
            }
            assertTrue(session.summary().openGoals() > 1, session.summary().toString());
            int goal = session.tree().stream().filter(n -> n.state().equals("open"))
                    .findFirst().orElseThrow().serial();
            session.configure(ProofSession.Limits.defaults());
            assertEquals(ProofSession.StopReason.TARGET_CLOSED,
                session.runAutoDetailed(goal).stopReason());
            assertFalse(session.summary().closed());
            assertEquals(ProofSession.StopReason.PROVED,
                session.runAutoDetailed(null).stopReason());
        }
    }

    @Test
    void interruptionIsReportedWithoutBecomingAnExecutionError() throws Exception {
        try (var session = ProofSession.open(inMemory("Interrupted"), spec(null, "holds"),
            ProofSession.Limits.defaults())) {
            Thread.currentThread().interrupt();
            try {
                var result = session.runAutoDetailed(null);
                assertEquals(ProofSession.StopReason.INTERRUPTED, result.stopReason());
                assertNull(result.exception());
            } finally {
                Thread.interrupted();
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static void useDiagnosticStrategy(ProofSession session, RuleAppCost cost,
            boolean approved, RuntimeException failure) {
        var original = session.proof().getActiveStrategy();
        session.proof().setActiveStrategy((Strategy<Goal>) Proxy.newProxyInstance(
            Strategy.class.getClassLoader(), new Class<?>[] { Strategy.class },
            (proxy, method, args) -> {
                if (method.getName().equals("computeCost")) {
                    if (failure != null) {
                        throw failure;
                    }
                    return cost;
                }
                if (method.getName().equals("isApprovedApp")) {
                    return approved;
                }
                try {
                    return method.invoke(original, args);
                } catch (InvocationTargetException e) {
                    throw e.getCause();
                }
            }));
    }

    @Test
    void ruleDiagnosticsReportCostApprovalAndExceptionsWithoutApplying() throws Exception {
        try (var session = ProofSession.open(inMemory("Diagnostics"), spec(null, "holds"),
            ProofSession.Limits.defaults())) {
            int root = session.tree().getFirst().serial();
            int offset = session.node(root, true).sequent().indexOf("holds");
            useDiagnosticStrategy(session, TopRuleAppCost.INSTANCE, true, null);
            var costly = session.ruleDiagnosticsAt(root, offset);
            assertTrue(costly.stream().anyMatch(
                r -> r.disposition() == ProofSession.RuleDisposition.INFINITE_COST));
            useDiagnosticStrategy(session, NumberRuleAppCost.getZeroCost(), false, null);
            assertTrue(session.ruleDiagnosticsAt(root, offset).stream().anyMatch(
                r -> r.disposition() == ProofSession.RuleDisposition.NOT_APPROVED));
            useDiagnosticStrategy(session, NumberRuleAppCost.getZeroCost(), true, null);
            var accepted = session.ruleDiagnosticsAt(root, offset).stream()
                    .filter(r -> r.disposition() == ProofSession.RuleDisposition.ACCEPTED)
                    .findFirst().orElseThrow();
            var failure = new IllegalStateException("diagnostic failure");
            useDiagnosticStrategy(session, NumberRuleAppCost.getZeroCost(), true, failure);
            var errored = session.ruleDiagnosticsAt(root, offset).stream()
                    .filter(r -> r.disposition() == ProofSession.RuleDisposition.ERROR)
                    .findFirst().orElseThrow();
            assertSame(failure, errored.exception());
            assertEquals(1, session.summary().nodes());
            useDiagnosticStrategy(session, NumberRuleAppCost.getZeroCost(), true, null);
            assertTrue(session.apply(root, accepted.rule().index(), Map.of()).nodes() > 1);
        }
    }

    @Test
    void searchErrorsKeepTheOriginalExceptionAndInvalidateCachedRules() throws Exception {
        try (var session = ProofSession.open(inMemory("SearchError"), spec(null, "holds"),
            ProofSession.Limits.defaults())) {
            int root = session.tree().getFirst().serial();
            session.rulesAt(root, session.node(root, true).sequent().indexOf("holds"));
            var failure = new IllegalStateException("search failure");
            useDiagnosticStrategy(session, NumberRuleAppCost.getZeroCost(), true, failure);
            var result = session.runAutoDetailed(null);
            assertEquals(ProofSession.StopReason.ERROR, result.stopReason());
            assertSame(failure, result.exception());
            assertSame(result, session.lastSearch());
            var outcome = SolidityVerifier.outcome(session, "holds", 3, false, 0);
            assertEquals(SolidityVerifier.Status.ERROR, outcome.status());
            assertTrue(outcome.error().contains("search failure"));
            assertThrows(IllegalArgumentException.class,
                () -> session.apply(root, 0, Map.of()));
        }
    }

    @Test
    void detailedVerificationDistinguishesOpenProofsAndLoadFailures() throws Exception {
        Path file = inMemory("Detailed");
        var options = SolidityVerifier.VerificationOptions.defaults();
        var open = SolidityVerifier.verifyDetailed(file, spec(null, "fails"), options);
        assertEquals(SolidityVerifier.Status.OPEN, open.status());
        assertEquals(ProofSession.StopReason.NO_APPLICABLE_RULE, open.search().stopReason());
        assertNull(open.exception());
        var proved = SolidityVerifier.verifyOrThrow(file, spec(null, "holds"), options);
        assertEquals(SolidityVerifier.Status.PROVED, proved.status());
        var bad = new SolidityProblemSpec(null, "holds", java.util.List.of("invalid"));
        var failed = SolidityVerifier.verifyDetailed(file, bad, options);
        assertEquals(SolidityVerifier.Status.ERROR, failed.status());
        assertTrue(failed.exception() instanceof IllegalArgumentException);
        assertNull(failed.search());
        assertThrows(IllegalArgumentException.class,
            () -> SolidityVerifier.verifyOrThrow(file, bad, options));
    }

    @Test
    void closingASessionIsIdempotentAndRejectsFurtherProofOperations() throws Exception {
        var session = ProofSession.open(inMemory("Close"), spec(null, "holds"),
            ProofSession.Limits.defaults());
        var proof = session.proof();
        assertFalse(session.isClosed());
        session.close();
        session.close();
        assertTrue(session.isClosed());
        assertTrue(proof.isDisposed());
        assertThrows(IllegalStateException.class, session::summary);
        assertThrows(IllegalStateException.class, session::tree);
        assertThrows(IllegalStateException.class, () -> session.runAutoDetailed(null));
        assertThrows(IllegalStateException.class, session::save);
    }

    @Test
    void aSessionIsWalkedPrunedSteppedAndReplayed(@TempDir Path dir) throws Exception {
        Path file = inMemory("Session");
        ProofSession session =
            ProofSession.start(file, spec(null, "fails"), ProofSession.Limits.defaults());
        var tree = session.tree();
        var open = tree.stream().filter(e -> e.state().equals("open")).findFirst().orElseThrow();
        assertEquals(-1, tree.getFirst().parent());
        assertTrue(session.node(open.serial(), true).openGoal());

        var root = tree.getFirst().serial();
        var pruned = session.prune(root);
        assertEquals(1, pruned.nodes());
        String sequent = session.node(root, true).sequent();
        var rules = session.rulesAt(root, sequent.indexOf("fails"));
        var complete = rules.rules().stream()
                .filter(r -> !r.sequentWide() && r.missing().isEmpty()).findFirst()
                .orElseThrow(() -> new AssertionError(sequent + "\n" + rules));
        assertTrue(session.apply(root, complete.index(), java.util.Map.of()).nodes() > 1);
        var stepped = session.tree().stream().filter(e -> e.state().equals("open")).findFirst()
                .orElseThrow();
        assertFalse(session.runAuto(stepped.serial()).closed());

        ProofSession holds =
            ProofSession.start(file, spec(null, "holds"), ProofSession.Limits.defaults());
        var closedRoot = holds.tree().getFirst().serial();
        var refused = assertThrows(IllegalArgumentException.class, () -> holds.prune(closedRoot));
        assertTrue(refused.getMessage().contains("closed branch"), refused.getMessage());
        var goal = session.tree().stream().filter(e -> e.state().equals("open")).findFirst()
                .orElseThrow();
        assertThrows(IllegalArgumentException.class, () -> session.prune(goal.serial()));
        assertTrue(holds.summary().closed());
        Path sol = dir.resolve("Session.sol");
        Files.writeString(sol, SoliditySources.read(file));
        Path proof = dir.resolve("Session.holds.proof");
        Files.writeString(proof, holds.save().replace("\"" + file + "\"", "\"Session.sol\""));
        ProofSession replayed = ProofSession.load(proof, ProofSession.Limits.defaults(), false);
        assertTrue(replayed.summary().closed());
        assertTrue(replayed.replayErrors().isEmpty(), String.valueOf(replayed.replayErrors()));
    }

    static Stream<Arguments> specifiedContractFunctions() throws IOException {
        return SolidityExampleTests.contractFunctions(ExampleDirs.SOLIDIKEY);
    }

    @ParameterizedTest(name = "{0}.{1}")
    @MethodSource("specifiedContractFunctions")
    void everyGeneratedProblemLoads(String contract, String function) {
        Path file = SolidityExampleTests.example(ExampleDirs.SOLIDIKEY + "/" + contract + ".sol");

        var outcome = SolidityVerifier.verify(file, spec(contract, function), 0, -1, 0);

        assertNull(outcome.error(), outcome.error());
    }

    @Test
    void parametersNamedLikeBuiltInsGetTheirOwnVariables(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("Clash.sol");
        Files.writeString(file, """
                // SPDX-License-Identifier: GPL-2.0-only
                pragma solidity ^0.8.0;
                contract Clash {
                    uint s;
                    /// @custom:key box
                    function valueTrue(uint msgValue) public payable {
                        require(msgValue == 5);
                        assert(msgValue == 5);
                    }
                    /// @custom:key box
                    function valueFalse(uint msgValue) public payable {
                        require(msgValue == 5);
                        assert(msg.value == 5);
                    }
                    /// @custom:key box
                    function senderFalse(address msgSender) public view {
                        require(msgSender == address(0));
                        assert(msg.sender == address(0));
                    }
                    /// @custom:key ensures r == result + 1
                    function resultTrue(uint result) public pure returns (uint r) {
                        r = result + 1;
                    }
                    /// @custom:key ensures r == result
                    function resultFalse(uint result) public pure returns (uint r) {
                        r = result + 1;
                    }
                    /// @custom:key ensures s == \\old(s) + old
                    function oldTrue(uint old) public { s += old; }
                    /// @custom:key ensures self == address(this)
                    function selfFalse(address self) public { }
                    /// @custom:key ensures msgValue == 0
                    function valueEnsuresFalse(uint msgValue) public { }
                }""");

        String text = SolidityProblemSynthesizer.problemText(file, spec("Clash", "valueTrue"));
        assertTrue(text.contains("valueTrue(msgValue$0)@Clash"), text);
        assertTrue(text.contains("int msgValue$0;"), text);
        assertTrue(SolidityProblemSynthesizer.problemText(file, spec("Clash", "resultTrue"))
                .contains("result = resultTrue(result$0)@Clash"));
        for (String function : List.of("valueTrue", "resultTrue", "oldTrue")) {
            assertEquals(SolidityVerifier.Status.PROVED,
                SolidityVerifier.verify(file, spec("Clash", function), 10000, -1, 3).status(),
                function);
        }
        for (String function : List.of("valueFalse", "senderFalse", "resultFalse", "selfFalse",
            "valueEnsuresFalse")) {
            assertEquals(SolidityVerifier.Status.OPEN,
                SolidityVerifier.verify(file, spec("Clash", function), 10000, -1, 3).status(),
                function);
        }
    }

    @Test
    void everyOverloadIsItsOwnObligation(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("Overloads.sol");
        Files.writeString(file, """
                // SPDX-License-Identifier: GPL-2.0-only
                pragma solidity ^0.8.0;
                contract Overloads {
                    /// @custom:key box
                    function f(uint a, uint b) public pure {
                        require(a == 1 && b == 2);
                        assert(a == 2);
                    }
                    /// @custom:key box
                    function f(uint a) public pure {
                        require(a == 1);
                        assert(a == 1);
                    }
                    function g(address a) public pure { }
                }""");

        assertEquals(List.of("f(uint256,uint256)", "f(uint256)", "g"),
            SolidityVerifier.functions(file, null));
        assertEquals("f(uint256,uint256)", SolidityProblemSynthesizer
                .resolve(file, spec(null, "f(uint256, uint256)")).function());
        assertEquals("g", SolidityProblemSynthesizer.resolve(file, spec(null, "g")).function());
        assertEquals("g",
            SolidityProblemSynthesizer.resolve(file, spec(null, "g(address)")).function());
        var ambiguous = assertThrows(IllegalArgumentException.class,
            () -> SolidityProblemSynthesizer.resolve(file, spec(null, "f")));
        assertTrue(ambiguous.getMessage().contains("overloads f"), ambiguous.getMessage());
        assertEquals(SolidityVerifier.Status.PROVED,
            SolidityVerifier.verify(file, spec(null, "f(uint256)"), 10000, -1, 3).status());
        assertEquals(SolidityVerifier.Status.OPEN,
            SolidityVerifier.verify(file, spec(null, "f(uint256,uint256)"), 10000, -1, 3)
                    .status());
    }

    @Test
    void initializersAndFallbackFunctionsKeepTheInvariant(@TempDir Path dir)
            throws IOException {
        Path file = dir.resolve("Entry.sol");
        Files.writeString(file, """
                // SPDX-License-Identifier: GPL-2.0-only
                pragma solidity ^0.8.0;
                /// @custom:key invariant x == 5
                contract Holds {
                    uint x = 5;
                    receive() external payable { x = 5; }
                    fallback() external { assert(x == 5); }
                    function t() public view { assert(x == 5); }
                }
                /// @custom:key invariant y == 5
                contract Breaks {
                    uint y;
                    receive() external payable { y = 1; }
                    fallback() external { assert(y == 4); }
                }""");

        assertEquals(List.of("constructor", "receive", "fallback", "t"),
            SolidityVerifier.functions(file, "Holds"));
        for (String function : List.of("constructor", "receive", "fallback", "t")) {
            assertEquals(SolidityVerifier.Status.PROVED,
                SolidityVerifier.verify(file, spec("Holds", function), 10000, -1, 3).status(),
                function);
        }
        for (String function : List.of("constructor", "receive", "fallback")) {
            assertEquals(SolidityVerifier.Status.OPEN,
                SolidityVerifier.verify(file, spec("Breaks", function), 10000, -1, 3).status(),
                function);
        }
    }
}
