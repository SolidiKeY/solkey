package org.key_project.solidity.idea.lang

import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet
import org.antlr.v4.runtime.Token
import org.key_project.solidity.parser.KeYSolidityDLLexer

/**
 * One [IElementType] per ANTLR token type of [KeYSolidityDLLexer], named after the token's
 * symbolic name. The set is derived from the grammar's vocabulary, so a token added to
 * `KeYLexer.g4` or `SolidityLexer.g4` appears here without any change to this file.
 */
object KeyTokenTypes {
    private val vocabulary = KeYSolidityDLLexer.VOCABULARY

    private val whitespaceNames = setOf("WS", "SOL_WS")

    val BAD_CHARACTER = IElementType("KEY_BAD_CHARACTER", KeyLanguage)

    private val byAntlrType: Array<IElementType> =
        Array(vocabulary.maxTokenType + 1) { type ->
            val name = vocabulary.getSymbolicName(type)
            when {
                name == null -> IElementType("KEY_TOKEN_$type", KeyLanguage)
                name in whitespaceNames -> TokenType.WHITE_SPACE
                else -> IElementType(name, KeyLanguage)
            }
        }

    fun forAntlrType(type: Int): IElementType =
        if (type == Token.EOF || type !in byAntlrType.indices) BAD_CHARACTER else byAntlrType[type]

    private fun namedSet(vararg names: String): TokenSet {
        val wanted = names.toSet()
        return TokenSet.create(*byAntlrType.filter { it.toString() in wanted }.toTypedArray())
    }

    val COMMENTS = namedSet(
        "SL_COMMENT",
        "LINE_COMMENT",
        "ML_COMMENT",
        "COMMENT_END",
        "SOL_COMMENT",
        "DOC_COMMENT",
    )

    val STRINGS = namedSet(
        "STRING_LITERAL",
        "QUOTED_STRING_LITERAL",
        "CHAR_LITERAL",
        "MODALITYD_STRING",
        "MODALITYD_CHAR",
        "StringLiteralFragment",
    )
}
