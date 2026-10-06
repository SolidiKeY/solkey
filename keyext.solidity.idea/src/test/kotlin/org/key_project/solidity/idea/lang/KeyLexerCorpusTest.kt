package org.key_project.solidity.idea.lang

import java.io.File
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory

/**
 * Lexes every `.key` file of the repository. The hand-written cases in [KeyLexerTest] are a few
 * lines each; these are the files the editor is actually pointed at.
 */
class KeyLexerCorpusTest {
    @TestFactory
    fun `every key file in the repository lexes and tiles`(): List<DynamicTest> {
        val files = corpus()
        assertTrue(files.size > 20, "expected the repository's .key files, found ${files.size}")
        return files.map { file ->
            DynamicTest.dynamicTest(file.name) { assertTilesAndRestarts(file.readText()) }
        }
    }

    private fun corpus(): List<File> {
        val root = File("..").canonicalFile
        return root.walkTopDown()
            .onEnter { it.name != "build" && it.name != ".git" && it.name != ".gradle" }
            .filter { it.isFile && it.extension == "key" }
            .sortedBy { it.path }
            .toList()
    }

    private fun assertTilesAndRestarts(text: String) {
        val lexer = KeyLexer()
        lexer.start(text, 0, text.length, 0)
        var offset = 0
        var tokens = 0
        val restarts = mutableListOf<Pair<Int, Int>>()
        while (lexer.tokenType != null) {
            assertEquals(offset, lexer.tokenStart, "gap or overlap after $tokens tokens")
            assertTrue(lexer.tokenEnd > lexer.tokenStart, "zero-width token at $offset")
            if (tokens % 500 == 0) restarts += lexer.tokenStart to lexer.state
            offset = lexer.tokenEnd
            tokens++
            lexer.advance()
        }
        assertEquals(text.length, offset, "tokens stop short of the end of the file")

        for ((from, state) in restarts) {
            val restarted = KeyLexer()
            restarted.start(text, from, text.length, state)
            var tail = from
            while (restarted.tokenType != null) {
                assertEquals(tail, restarted.tokenStart, "gap after restarting at $from")
                tail = restarted.tokenEnd
                restarted.advance()
            }
            assertEquals(text.length, tail, "restart at $from stops short of the end")
        }
    }
}
