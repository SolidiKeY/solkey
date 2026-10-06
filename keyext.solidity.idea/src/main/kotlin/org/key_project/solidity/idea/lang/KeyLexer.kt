package org.key_project.solidity.idea.lang

import com.intellij.lexer.LexerBase
import com.intellij.psi.tree.IElementType
import org.antlr.v4.runtime.CharStream
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.Lexer
import org.antlr.v4.runtime.Token
import org.key_project.solidity.parser.KeYSolidityDLLexer

/**
 * The prover's own lexer, adjusted for an editor rather than a parse.
 *
 * `SOL_WS` is declared `-> skip`, so the prover's token stream does not cover every character of
 * the file; an IntelliJ lexer must. Not skipping it closes that hole, and [KeyLexer] fills the
 * ones a lexer error leaves. The inherited `nextToken` also stashes an end-of-file token the
 * moment it reads `\proof`, which would cut highlighting off at a saved proof.
 */
private class EditorKeyLexer(input: CharStream) : KeYSolidityDLLexer(input) {
    override fun skip() {}

    override fun nextToken(): Token {
        val token = super.nextToken()
        if (token.type == Token.EOF && _input.index() < _input.size()) return super.nextToken()
        return token
    }
}

/**
 * Interns the lexer's mode and mode stack as the `int` an IntelliJ lexer reports as its state, so
 * re-lexing can resume inside a modality body. [INITIAL] is the state of a lexer that has just
 * started, which IntelliJ requires to be zero.
 */
private object KeyLexerModes {
    const val INITIAL = 0

    private val modeStacks = mutableListOf(listOf(Lexer.DEFAULT_MODE))
    private val indices = mutableMapOf(modeStacks[0] to INITIAL)

    fun encode(lexer: KeYSolidityDLLexer): Int {
        val modes = ArrayList<Int>(lexer._modeStack.size() + 1)
        modes.add(lexer._mode)
        for (i in 0 until lexer._modeStack.size()) modes.add(lexer._modeStack.get(i))
        synchronized(this) {
            indices[modes]?.let { return it }
            modeStacks.add(modes)
            val index = modeStacks.size - 1
            indices[modes] = index
            return index
        }
    }

    fun restore(lexer: KeYSolidityDLLexer, state: Int) {
        val modes = synchronized(this) { modeStacks.getOrNull(state) } ?: return
        lexer._modeStack.clear()
        lexer._mode = modes[0]
        for (i in 1 until modes.size) lexer._modeStack.push(modes[i])
    }
}

/**
 * An IntelliJ lexer over [KeYSolidityDLLexer]. Characters no grammar rule matched are reported as
 * [KeyTokenTypes.BAD_CHARACTER] rather than dropped, so the tokens tile the document.
 */
class KeyLexer : LexerBase() {
    private var chars: CharSequence = ""
    private var charsStart = 0
    private var charsEnd = 0
    private var antlr: EditorKeyLexer? = null
    private var pending: Token? = null
    private var lexemeType: IElementType? = null
    private var lexemeStart = 0
    private var lexemeEnd = 0
    private var currentState = KeyLexerModes.INITIAL
    private var nextState = KeyLexerModes.INITIAL

    override fun start(buffer: CharSequence, startOffset: Int, endOffset: Int, initialState: Int) {
        chars = buffer
        charsStart = startOffset
        charsEnd = endOffset
        val text = buffer.subSequence(startOffset, endOffset).toString()
        antlr = EditorKeyLexer(CharStreams.fromString(text)).also {
            it.removeErrorListeners()
            KeyLexerModes.restore(it, initialState)
        }
        pending = null
        lexemeEnd = startOffset
        nextState = initialState
        advance()
    }

    override fun advance() {
        lexemeStart = lexemeEnd
        currentState = nextState
        val antlr = this.antlr
        if (antlr == null || lexemeStart >= charsEnd) {
            lexemeType = null
            return
        }
        val token = pending ?: antlr.nextToken()
        if (token.type == Token.EOF) {
            pending = null
            lexemeType = KeyTokenTypes.BAD_CHARACTER
            lexemeEnd = charsEnd
            return
        }
        val start = charsStart + token.startIndex
        if (start > lexemeStart) {
            pending = token
            lexemeType = KeyTokenTypes.BAD_CHARACTER
            lexemeEnd = start
            return
        }
        pending = null
        lexemeType = KeyTokenTypes.forAntlrType(token.type)
        lexemeEnd = charsStart + token.stopIndex + 1
        if (lexemeEnd <= lexemeStart) lexemeEnd = charsEnd
        nextState = KeyLexerModes.encode(antlr)
    }

    override fun getState(): Int = currentState

    override fun getTokenType(): IElementType? = lexemeType

    override fun getTokenStart(): Int = lexemeStart

    override fun getTokenEnd(): Int = lexemeEnd

    override fun getBufferSequence(): CharSequence = chars

    override fun getBufferEnd(): Int = charsEnd
}
