/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.parser;

import org.key_project.solidity.util.parsing.BuildingException;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.tree.Trees;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.key_project.solidity.parser.ParserForTesting.*;

public class AntlrParserTest {

    @Test
    void testParseBool() {
        KeYSolidityDLParser parser = parse("true");

        KeYSolidityDLParser.PrimaryExpressionContext exp = parser.primaryExpression();
        assertEquals("([] true)", exp.toStringTree());
    }

    @Test
    void simpleBlock() {
        KeYSolidityDLParser parser = parse("{}");
        KeYSolidityDLParser.BlockContext block = parser.block();
        // use rule names (parser-aware) so the assertion is robust to rule-index shifts
        assertEquals("(normalBlock { })", block.normalBlock().toStringTree(parser));
    }

    @Test
    void schema() {
        KeYSolidityDLParser parser = parse("s#abc");
        KeYSolidityDLParser.SchemaVariableContext scm = parser.schemaVariable();
        String s = scm.toStringTree();
        assertEquals(0, parser.getNumberOfSyntaxErrors());
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "{ }",
        "{ int a; }",
        "{ bool a = b; }",
        "{ return true; }",
        "{ a = 5; b = 10; }",
        "{ bool a = true; return a; }",
        "{ int x; { int x; } }",
        "{ uint256 x; { uint256 y; } }",
        "{ if (true) { x = 1; } }",
        "{ if (a > b) x = 1; else x = 2; }",
        "{ for (uint i = 0; i < 10; i++) { } }",
        "{ while (x < 5) { x++; } }",
        "{ do { x--; } while (x > 0); }",
        "{ emit Transfer(msg.sender, to, val); }",
        "{ try externalContract.f() { } catch { } }",
        "{ unchecked { x = x - 1; } }",
        "{ revert(\"error\"); }",
        "{ (a, b) = (1, 2); }",
        "{ address payable x = payable(0x123); }",
        "{ s#schemaStm; }",
        "{ int a = s#schema; }"
    })
    void correctParsing(String input) {
        KeYSolidityDLParser parser = parse(input);
        KeYSolidityDLParser.BlockContext block = parser.block();
        String s = block.toStringTree();
        assertEquals(0, parser.getNumberOfSyntaxErrors());
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "{",
        "{ int a }",
        "{ assembly { let x := 0 } }",
    })
    void wrongParsing(String input) {
        KeYSolidityDLParser parser = parse(input);
        KeYSolidityDLParser.BlockContext block = parser.block();
        assertTrue(parser.getNumberOfSyntaxErrors() > 0);
    }

    @Test
    void modalityBodyIsASubtreeOfTheTerm() {
        var diamond = modality("\\<{ x = 1; }\\> true");
        assertInstanceOf(KeYSolidityDLParser.DiamondModalityContext.class, diamond);
        assertEquals("{x=1;}",
            diamond.getRuleContext(KeYSolidityDLParser.BlockContext.class, 0).getText());
        assertInstanceOf(KeYSolidityDLParser.BoxModalityContext.class,
            modality("\\[{ x = 1; }\\] true"));
    }

    @Test
    void schematicModalityKeepsItsOperatorName() {
        var named = assertInstanceOf(KeYSolidityDLParser.NamedModalityContext.class,
            modality("\\modality{#mod}{c# s#x; #c}\\endmodality true"));
        assertEquals("\\modality{#mod}", named.op.getText());
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "\\problem {\n \\<{ x = 1 y = 2; }\\>(x = 1) }",
        "\\problem {\n \\<{ x = 1; } y = 2; \\>(x = 1) }",
        "\\problem {\n (x = 1 -> x = 1 }"
    })
    void syntaxErrorsFailWithTheFilePosition(String problem) {
        var e = assertThrows(BuildingException.class,
            () -> ParsingFacade.parseFile(CharStreams.fromString(problem, "probe.key")));
        assertTrue(e.getMessage().contains("probe.key:2:"), e.getMessage());
    }

    private static KeYSolidityDLParser.ModalityContext modality(String term) {
        var ctx = ParsingFacade.parseExpression(CharStreams.fromString(term)).ctx;
        return (KeYSolidityDLParser.ModalityContext) Trees
                .findAllRuleNodes(ctx, KeYSolidityDLParser.RULE_modality).iterator().next();
    }
}
