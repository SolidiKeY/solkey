package org.key_project.solidity.idea

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VirtualFile

/**
 * Opens KeYther on one Solidity function.
 *
 * Two ways in, both landing in [KeYtherLauncher]: the gutter icon, which knows which function it
 * belongs to, and the menu/shortcut entry, which resolves the function from the caret.
 */
class ProveFunctionAction(
    private val project: Project? = null,
    private val file: VirtualFile? = null,
    private val function: SolFunction? = null,
) : AnAction("Prove Function in KeYther") {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        if (function != null) {
            e.presentation.isEnabledAndVisible = true
            return
        }
        val editorFile = e.getData(CommonDataKeys.VIRTUAL_FILE)
        e.presentation.isEnabledAndVisible =
            e.project != null && editorFile?.extension.equals("sol", ignoreCase = true)
    }

    override fun actionPerformed(e: AnActionEvent) {
        val target = project ?: e.project ?: return
        if (function != null && file != null) {
            KeYtherLauncher.launch(target, file, function)
            return
        }
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val editorFile = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return
        val atCaret = functionAtCaret(editor)
        if (atCaret == null) {
            Messages.showErrorDialog(
                target,
                "The caret is not inside a public Solidity function with a body. Only those have " +
                    "a proof obligation; put the caret in one, or click the ▶ in the gutter.",
                "No Function at the Caret",
            )
            return
        }
        KeYtherLauncher.launch(target, editorFile, atCaret)
    }

    private fun functionAtCaret(editor: Editor): SolFunction? {
        val caret = editor.caretModel.offset
        return SolFunctionScanner.scan(editor.document.charsSequence).firstOrNull { caret in it }
    }
}
