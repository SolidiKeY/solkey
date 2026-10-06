package org.key_project.solidity.idea.lang

import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighter
import com.intellij.openapi.options.colors.AttributesDescriptor
import com.intellij.openapi.options.colors.ColorDescriptor
import com.intellij.openapi.options.colors.ColorSettingsPage
import javax.swing.Icon

/** Settings → Editor → Color Scheme → KeY. */
class KeyColorSettingsPage : ColorSettingsPage {
    override fun getDisplayName(): String = "KeY"

    override fun getIcon(): Icon? = KeyFileType.icon

    override fun getHighlighter(): SyntaxHighlighter = KeySyntaxHighlighter()

    override fun getAttributeDescriptors(): Array<AttributesDescriptor> = DESCRIPTORS

    override fun getColorDescriptors(): Array<ColorDescriptor> = ColorDescriptor.EMPTY_ARRAY

    override fun getAdditionalHighlightingTagToDescriptorMap(): Map<String, TextAttributesKey>? = null

    override fun getDemoText(): String = DEMO_TEXT

    private companion object {
        val DESCRIPTORS = arrayOf(
            AttributesDescriptor("Keyword", KeyColors.KEYWORD),
            AttributesDescriptor("Variable condition", KeyColors.VARCOND),
            AttributesDescriptor("Modality delimiter", KeyColors.MODALITY),
            AttributesDescriptor("Sequent arrow", KeyColors.SEQUENT_ARROW),
            AttributesDescriptor("Type//Sort", KeyColors.TYPE),
            AttributesDescriptor("Schema variable", KeyColors.SCHEMA_VARIABLE),
            AttributesDescriptor("Identifier", KeyColors.IDENTIFIER),
            AttributesDescriptor("Number", KeyColors.NUMBER),
            AttributesDescriptor("String", KeyColors.STRING),
            AttributesDescriptor("Constant", KeyColors.CONSTANT),
            AttributesDescriptor("Comments//Line comment", KeyColors.LINE_COMMENT),
            AttributesDescriptor("Comments//Block comment", KeyColors.BLOCK_COMMENT),
            AttributesDescriptor("Comments//Documentation comment", KeyColors.DOC_COMMENT),
            AttributesDescriptor("Braces and operators//Operator", KeyColors.OPERATOR),
            AttributesDescriptor("Braces and operators//Parentheses", KeyColors.PARENTHESES),
            AttributesDescriptor("Braces and operators//Braces", KeyColors.BRACES),
            AttributesDescriptor("Braces and operators//Brackets", KeyColors.BRACKETS),
            AttributesDescriptor("Braces and operators//Semicolon", KeyColors.SEMICOLON),
            AttributesDescriptor("Braces and operators//Comma", KeyColors.COMMA),
            AttributesDescriptor("Braces and operators//Dot", KeyColors.DOT),
            AttributesDescriptor("Bad character", KeyColors.BAD_CHARACTER),
        )

        val DEMO_TEXT = """
            // A taclet over a Solidity statement.
            \sorts {
                Field;
            }

            \schemaVariables {
                \program Variable #v;
                \program SolExpression #e;
                \formula post;
            }

            \rules(transferSemantics:withCallback) {
                requireSimple {
                    \find (==> \<{ require(#e); ... }\> post)
                    \varcond (\hasSort(#e, boolean), \notFreeIn(#v, post))
                    \replacewith (==> value(#e) = TRUE -> \<{ ... }\> post);
                    \heuristics (simplify_prog)
                };
            }

            \problem {
                balance(msg.sender) >= 100 ->
                    \<{ uint256 x = balance(msg.sender); x = x - 100; }\> (x >= 0 & true)
            }
        """.trimIndent()
    }
}
