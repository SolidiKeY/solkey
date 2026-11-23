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
            e.presentation.isEnabledAndVisible = tool.isAvailableFor(target)
            return
        }
        val editorFile = e.getData(CommonDataKeys.VIRTUAL_FILE)
        val isKey = KeyProblemScanner.isKeyFile(editorFile?.extension)
        if (e.project == null ||
            !(editorFile?.extension.equals("sol", ignoreCase = true) || isKey)
        ) {
            e.presentation.isEnabledAndVisible = false
            return
        }
        if (isKey) {
            e.presentation.isEnabledAndVisible = tool != SolKeyTool.SOLC
            return
        }
        val atCaret = if (tool == SolKeyTool.SOLC) {
            e.getData(CommonDataKeys.EDITOR)?.let { targetAtCaret(it, editorFile!!) }
        } else {
            null
        }
        e.presentation.isEnabledAndVisible = tool != SolKeyTool.SOLC ||
            (atCaret != null && tool.isAvailableFor(atCaret))
    }

    override fun actionPerformed(e: AnActionEvent) {
        val inProject = project ?: e.project ?: return
        if (target != null && file != null) {
            SolKeyLauncher.launch(inProject, tool, file, target)
            return
        }
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val editorFile = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return
        val atCaret = targetAtCaret(editor, editorFile)
        if (atCaret == null && KeyProblemScanner.isKeyFile(editorFile.extension)) {
            Messages.showErrorDialog(inProject, "This file has no \\problem to prove.", "Nothing to Run")
            return
        }
        if (atCaret == null) {
            Messages.showErrorDialog(
                inProject,
                "The caret is outside every contract in this file. Put it inside one — or " +
                    "inside a public or external function, to run on that function alone — or click a ▶ in " +
                    "the gutter.",
                "Nothing to Run at the Caret",
            )
            return
        }
        SolKeyLauncher.launch(inProject, tool, editorFile, atCaret)
    }

    /**
     * The narrowest thing the caret sits in: a public or external function, or the enclosing contract.
     */
    private fun targetAtCaret(editor: Editor, file: VirtualFile): SolKeyTarget? {
        if (KeyProblemScanner.isKeyFile(file.extension)) {
            return KeyProblemScanner.find(editor.document.charsSequence)
                ?.let { SolKeyTarget.Problem(it, file.name) }
        }
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
