package org.key_project.solidity.idea.lang

import com.intellij.psi.TokenType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.key_project.solidity.parser.KeYSolidityDLLexer

class KeyLexerTest {
    private fun lex(text: String, from: Int = 0, state: Int = 0): List<Triple<String, Int, Int>> {
        val lexer = KeyLexer()
        lexer.start(text, from, text.length, state)
        val tokens = mutableListOf<Triple<String, Int, Int>>()
        while (lexer.tokenType != null) {
            tokens += Triple(lexer.tokenType.toString(), lexer.tokenStart, lexer.tokenEnd)
            lexer.advance()
        }
        return tokens
    }

    private fun assertTiles(text: String) {
        val tokens = lex(text)
        var offset = 0
        for ((name, start, end) in tokens) {
            assertEquals(offset, start, "gap or overlap before $name in: $text")
            assertTrue(end > start, "zero-width token $name in: $text")
            offset = end
        }
        assertEquals(text.length, offset, "tokens stop short of the end of: $text")
    }

    @Test
    fun `tokens tile a taclet`() {
        assertTiles(
            """
            // a comment
            \rules {
                requireSimple {
                    \find (==> \<{ require(#e); ... }\> post)
                    \varcond (\hasSort(#e, boolean))
                    \replacewith (==> \<{ ... }\> post);
                };
            }
            """.trimIndent(),
        )
    }

    @Test
    fun `tokens tile skipped whitespace inside a modality body`() {
        assertTiles("\\problem { \\<{ uint256 x  =  1 ; }\\> true }")
    }

    @Test
    fun `tokens tile text after a proof section`() {
        assertTiles("\\problem { true }\n\\proof {\n(keyLog \"x\")\n}\n")
    }

    @Test
    fun `an unmatched character outside a modality is the grammar's own error token`() {
        val text = "\\problem { ¿ }"
        assertTiles(text)
        assertTrue(lex(text).any { it.first == "ERROR_CHAR" }, "expected ERROR_CHAR in ${lex(text)}")
    }

    @Test
    fun `the grammar's error token is coloured as a bad character`() {
        val vocabulary = KeYSolidityDLLexer.VOCABULARY
        val errorChar = (1..vocabulary.maxTokenType).first { vocabulary.getSymbolicName(it) == "ERROR_CHAR" }
        assertEquals(KeyColors.BAD_CHARACTER, KeyColors.of(KeyTokenTypes.forAntlrType(errorChar)))
    }

    @Test
    fun `an unmatched character inside a modality body is a bad character rather than a gap`() {
        val text = "\\problem { \\<{ ¿ }\\> true }"
        assertTiles(text)
        assertTrue(
            lex(text).any { it.first == KeyTokenTypes.BAD_CHARACTER.toString() },
            "expected a bad character in ${lex(text)}",
        )
    }

    @Test
    fun `whitespace is reported as whitespace`() {
        assertTrue(lex("\\problem { true }").any { it.first == TokenType.WHITE_SPACE.toString() })
    }

    @Test
    fun `a modality body lexes as Solidity`() {
        val names = lex("\\problem { \\<{ delete x; }\\> true }").map { it.first }
        assertTrue("DELETE" in names, "expected the Solidity DELETE token, got $names")
    }

    @Test
    fun `the state inside a modality body differs from the default state`() {
        val text = "\\problem { \\<{ delete x; }\\> true }"
        val lexer = KeyLexer()
        lexer.start(text, 0, text.length, 0)
        var insideState: Int? = null
        while (lexer.tokenType != null) {
            if (lexer.tokenType.toString() == "DELETE") insideState = lexer.state
            lexer.advance()
        }
        assertNotEquals(null, insideState, "the modality body was never reached")
        assertNotEquals(0, insideState, "the SOL mode was not encoded into the lexer state")
    }

    @Test
    fun `restarting from a state inside a modality body resumes in Solidity`() {
        val text = "\\problem { \\<{ delete x; }\\> true }"
        val lexer = KeyLexer()
        lexer.start(text, 0, text.length, 0)
        var restartOffset = -1
        var restartState = 0
        while (lexer.tokenType != null) {
            if (lexer.tokenType.toString() == "DELETE") {
                restartOffset = lexer.tokenStart
                restartState = lexer.state
                break
            }
            lexer.advance()
        }
        assertNotEquals(-1, restartOffset, "the modality body was never reached")
        assertEquals("DELETE", lex(text, restartOffset, restartState).first().first)
    }
}
