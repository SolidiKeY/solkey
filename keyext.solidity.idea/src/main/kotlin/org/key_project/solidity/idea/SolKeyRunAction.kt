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
 * Runs one SolKey tool on one function or contract.
 *
 * Two ways in, both landing in [SolKeyLauncher]: a gutter icon, which knows what it sits beside,
 * and the menu/shortcut entries, which resolve the target from the caret.
 */
open class SolKeyRunAction(
    private val tool: SolKeyTool,
    private val project: Project? = null,
    private val file: VirtualFile? = null,
    private val target: SolKeyTarget? = null,
) : AnAction(tool.actionText) {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        if (target != null) {
            e.presentation.isEnabledAndVisible = true
            return
        }
        val editorFile = e.getData(CommonDataKeys.VIRTUAL_FILE)
        e.presentation.isEnabledAndVisible =
            e.project != null && editorFile?.extension.equals("sol", ignoreCase = true)
    }

    override fun actionPerformed(e: AnActionEvent) {
        val inProject = project ?: e.project ?: return
        if (target != null && file != null) {
            SolKeyLauncher.launch(inProject, tool, file, target)
            return
        }
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val editorFile = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return
        val atCaret = targetAtCaret(editor)
        if (atCaret == null) {
            Messages.showErrorDialog(
                inProject,
                "The caret is outside every contract in this file. Put it inside one — or " +
                    "inside a public function, to run on that function alone — or click a ▶ in " +
                    "the gutter.",
                "Nothing to Run at the Caret",
            )
            return
        }
        SolKeyLauncher.launch(inProject, tool, editorFile, atCaret)
    }

    /**
     * The narrowest thing the caret sits in: a public function if it is inside one, otherwise the
     * contract around it. Smallest wins, so a `library` inside a `contract` is the one picked.
     */
    private fun targetAtCaret(editor: Editor): SolKeyTarget? {
        val caret = editor.caretModel.offset
        val outline = SolFunctionScanner.scanAll(editor.document.charsSequence)
        outline.functions.firstOrNull { caret in it }
            ?.let { return SolKeyTarget.Function(it) }
        return outline.contracts
            .filter { caret in it }
            .minByOrNull { it.endOffset - it.offset }
            ?.let { SolKeyTarget.Contract(it) }
    }
}

/** The three `plugin.xml` entries; each needs a constructor the platform can call. */
class ProveInKeytherAction : SolKeyRunAction(SolKeyTool.KEY_GUI)

class ProveHeadlessAction : SolKeyRunAction(SolKeyTool.KEY_HEADLESS)

class RunSolcAction : SolKeyRunAction(SolKeyTool.SOLC)
