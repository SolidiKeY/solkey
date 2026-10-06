package org.key_project.solidity.idea.lang

import com.intellij.icons.AllIcons
import com.intellij.lang.Language
import com.intellij.openapi.fileTypes.LanguageFileType
import javax.swing.Icon

/** The language of KeY problem, rule and taclet files. */
object KeyLanguage : Language("KeY")

/**
 * `.key` and `.proof` files. Registered for highlighting only: the parser produces a flat tree,
 * because nothing here needs a PSI structure.
 */
object KeyFileType : LanguageFileType(KeyLanguage) {
    override fun getName(): String = "KeY"

    override fun getDescription(): String = "KeY problem, rule and proof file"

    override fun getDefaultExtension(): String = "key"

    override fun getIcon(): Icon = AllIcons.FileTypes.Text
}
