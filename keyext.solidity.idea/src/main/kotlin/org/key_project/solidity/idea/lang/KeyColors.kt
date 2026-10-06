package org.key_project.solidity.idea.lang

import com.intellij.openapi.editor.DefaultLanguageHighlighterColors as Default
import com.intellij.openapi.editor.HighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.editor.colors.TextAttributesKey.createTextAttributesKey
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType

/**
 * The colors of KeY syntax, and which token of the grammar wears which.
 *
 * Only the categories that are not keywords are listed by name; every other token of the grammar
 * is a keyword, so a taclet keyword added to `KeYLexer.g4` is highlighted without a change here.
 */
object KeyColors {
    val KEYWORD = createTextAttributesKey("KEY.KEYWORD", Default.KEYWORD)
    val VARCOND = createTextAttributesKey("KEY.VARCOND", Default.METADATA)
    val MODALITY = createTextAttributesKey("KEY.MODALITY", Default.PREDEFINED_SYMBOL)
    val SEQUENT_ARROW = createTextAttributesKey("KEY.SEQUENT_ARROW", Default.KEYWORD)
    val TYPE = createTextAttributesKey("KEY.TYPE", Default.CLASS_NAME)
    val SCHEMA_VARIABLE = createTextAttributesKey("KEY.SCHEMA_VARIABLE", Default.INSTANCE_FIELD)
    val IDENTIFIER = createTextAttributesKey("KEY.IDENTIFIER", Default.IDENTIFIER)
    val NUMBER = createTextAttributesKey("KEY.NUMBER", Default.NUMBER)
    val STRING = createTextAttributesKey("KEY.STRING", Default.STRING)
    val CONSTANT = createTextAttributesKey("KEY.CONSTANT", Default.CONSTANT)
    val LINE_COMMENT = createTextAttributesKey("KEY.LINE_COMMENT", Default.LINE_COMMENT)
    val BLOCK_COMMENT = createTextAttributesKey("KEY.BLOCK_COMMENT", Default.BLOCK_COMMENT)
    val DOC_COMMENT = createTextAttributesKey("KEY.DOC_COMMENT", Default.DOC_COMMENT)
    val OPERATOR = createTextAttributesKey("KEY.OPERATOR", Default.OPERATION_SIGN)
    val PARENTHESES = createTextAttributesKey("KEY.PARENTHESES", Default.PARENTHESES)
    val BRACES = createTextAttributesKey("KEY.BRACES", Default.BRACES)
    val BRACKETS = createTextAttributesKey("KEY.BRACKETS", Default.BRACKETS)
    val SEMICOLON = createTextAttributesKey("KEY.SEMICOLON", Default.SEMICOLON)
    val COMMA = createTextAttributesKey("KEY.COMMA", Default.COMMA)
    val DOT = createTextAttributesKey("KEY.DOT", Default.DOT)
    val BAD_CHARACTER = createTextAttributesKey("KEY.BAD_CHARACTER", HighlighterColors.BAD_CHARACTER)

    private val byTokenName: Map<String, TextAttributesKey> = buildMap {
        putAll(
            """
            SAME_AS_TERM HAS_FIELD_SORT HAS_MEMORY_FIELD_SORT HAS_ELEMENT_SORT
            HAS_MEMORY_ELEMENT_SORT NEW_LOCAL_VARS STORE_TERM_IN STORE_EXPR_IN HAS_INVARIANT
            GET_INVARIANT GET_VARIANT IS_LABELED DIFFERENT NO_FREE_VAR_IN NO_FIXED_ARRAY_ELEMENT
            APPLY_UPDATE_ON_RIGID DEPENDINGON DISJOINTMODULONULL DROP_EFFECTLESS_ELEMENTARIES
            DROP_EFFECTLESS_STORES SIMPLIFY_IF_THEN_ELSE_UPDATE HASSORT ISINDUCTVAR ISOBSERVER
            ISSUBTYPE EQUAL_UNIQUE NEW NEW_TYPE_OF NEW_DEPENDING_ON HAS_ELEMENTARY_SORT NOT_
            NOTFREEIN SAME STRICT TYPEOF INSTANTIATE_GENERIC SAME_OBSERVER SAMEUPDATELEVEL
            INSEQUENTSTATE ANTECEDENTPOLARITY SUCCEDENTPOLARITY
            """.toKeys(VARCOND),
        )
        putAll(
            """
            MODALITY MODALITYD MODALITYB MODALITYBB MODALITYBB_END MODAILITYGENERIC1
            MODAILITYGENERIC2 MODAILITYGENERIC4 MODAILITYGENERIC6 DIAMOND_END BOX_END ENDMODALITY
            CTX_OPEN CTX_CLOSE
            """.toKeys(MODALITY),
        )
        putAll("SEQARROW".toKeys(SEQUENT_ARROW))
        putAll("ADDRESS BOOL STRING BYTE MAPPING Int Uint Byte Fixed Ufixed".toKeys(TYPE))
        putAll("Schema ExpandFunctionBody".toKeys(SCHEMA_VARIABLE))
        putAll("IDENT Identifier".toKeys(IDENTIFIER))
        putAll(
            """
            INT_LITERAL FLOAT_LITERAL DOUBLE_LITERAL REAL_LITERAL BIN_LITERAL HEX_LITERAL
            DecimalNumber HexNumber HexLiteralFragment VersionLiteral
            """.toKeys(NUMBER),
        )
        putAll(
            """
            STRING_LITERAL QUOTED_STRING_LITERAL CHAR_LITERAL MODALITYD_STRING MODALITYD_CHAR
            StringLiteralFragment
            """.toKeys(STRING),
        )
        putAll("TRUE FALSE BooleanLiteral NumberUnit".toKeys(CONSTANT))
        putAll("SL_COMMENT LINE_COMMENT".toKeys(LINE_COMMENT))
        putAll("ML_COMMENT COMMENT_END SOL_COMMENT".toKeys(BLOCK_COMMENT))
        putAll("DOC_COMMENT".toKeys(DOC_COMMENT))
        putAll("LPAREN RPAREN SOL_LPAREN SOL_RPAREN".toKeys(PARENTHESES))
        putAll("LBRACE RBRACE SOL_LBRACE SOL_RBRACE".toKeys(BRACES))
        putAll(
            """
            LBRACKET RBRACKET SOL_LBRACKET SOL_RBRACKET EMPTYBRACKETS OPENTYPEPARAMS
            CLOSETYPEPARAMS
            """.toKeys(BRACKETS),
        )
        putAll("SEMI SOL_SEMI".toKeys(SEMICOLON))
        putAll("COMMA SOL_COMMA".toKeys(COMMA))
        putAll("DOT DOTRANGE SOL_DOT".toKeys(DOT))
        putAll("ERROR_CHAR".toKeys(BAD_CHARACTER))
        putAll(
            """
            SLASH COLON DOUBLECOLON ASSIGN AT PARALLEL OR AND NOT IMP EQUALS NOT_EQUALS EXP TILDE
            PERCENT STAR MINUS PLUS GREATER GREATEREQUAL LESS LESSEQUAL LGUILLEMETS RGUILLEMETS
            EQV PRIMES UTF_PRECEDES UTF_IN UTF_EMPTY UTF_UNION UTF_INTERSECT UTF_SUBSET_EQ
            UTF_SUBSEQ UTF_SETMINUS ARROW INC DEC SOL_ASSIGN SOL_COLON SOL_PLUS SOL_MINUS SOL_NOT
            SOL_TILDE SOL_AT POW MUL DIV MOD SHL SHR LT GT LE GE EQ NE BITAND BITXOR BITOR SOL_AND
            SOL_OR QUESTION OR_ASSIGN XOR_ASSIGN AND_ASSIGN SHL_ASSIGN SHR_ASSIGN ADD_ASSIGN
            SUB_ASSIGN MUL_ASSIGN DIV_ASSIGN MOD_ASSIGN
            """.toKeys(OPERATOR),
        )
    }

    private fun String.toKeys(key: TextAttributesKey): Map<String, TextAttributesKey> =
        trim().split(Regex("\\s+")).associateWith { key }

    fun of(tokenType: IElementType): TextAttributesKey? = when {
        tokenType == TokenType.WHITE_SPACE -> null
        tokenType == KeyTokenTypes.BAD_CHARACTER -> BAD_CHARACTER
        else -> byTokenName[tokenType.toString()] ?: KEYWORD
    }
}
