package org.key_project.solidity.idea.lang

import com.intellij.lang.ASTNode
import com.intellij.lang.ParserDefinition
import com.intellij.lang.PsiBuilder
import com.intellij.lang.PsiParser
import com.intellij.lexer.Lexer
import com.intellij.openapi.project.Project
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.IFileElementType
import com.intellij.psi.tree.TokenSet
import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.extapi.psi.PsiFileBase

/**
 * A flat tree: every token becomes a child of the file node. Highlighting needs no structure, but
 * a [com.intellij.lang.Language]-backed file type needs a parser definition to have a PSI file at
 * all, and this is what gives the editor its comment and string token sets.
 */
class KeyParserDefinition : ParserDefinition {
    override fun createLexer(project: Project?): Lexer = KeyLexer()

    override fun createParser(project: Project?): PsiParser = KeyParser()

    override fun getFileNodeType(): IFileElementType = FILE

    override fun getWhitespaceTokens(): TokenSet = TokenSet.WHITE_SPACE

    override fun getCommentTokens(): TokenSet = KeyTokenTypes.COMMENTS

    override fun getStringLiteralElements(): TokenSet = KeyTokenTypes.STRINGS

    override fun createElement(node: ASTNode): PsiElement = ASTWrapperPsiElement(node)

    override fun createFile(viewProvider: FileViewProvider): PsiFile = KeyPsiFile(viewProvider)

    companion object {
        val FILE = IFileElementType(KeyLanguage)
    }
}

private class KeyParser : PsiParser {
    override fun parse(root: com.intellij.psi.tree.IElementType, builder: PsiBuilder): ASTNode {
        val file = builder.mark()
        while (!builder.eof()) builder.advanceLexer()
        file.done(root)
        return builder.treeBuilt
    }
}

class KeyPsiFile(viewProvider: FileViewProvider) : PsiFileBase(viewProvider, KeyLanguage) {
    override fun getFileType() = KeyFileType

    override fun toString() = "KeY file"
}
