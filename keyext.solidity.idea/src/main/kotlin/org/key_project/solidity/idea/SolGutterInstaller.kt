package org.key_project.solidity.idea

import com.intellij.icons.AllIcons
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorKind
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.editor.event.EditorFactoryEvent
import com.intellij.openapi.editor.event.EditorFactoryListener
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.Alarm
import javax.swing.Icon

/**
 * Puts a ▶ in the gutter beside every public function of an open `.sol` file.
 *
 * The icons live in the editor's own markup model rather than coming from a `LineMarkerProvider`,
 * because that extension point is registered per language and the IDE only knows Solidity when a
 * third-party plugin supplies it. See [SolFunctionScanner].
 */
class SolGutterInstaller(
    private val project: Project,
    private val parent: Disposable,
) : EditorFactoryListener {

    override fun editorCreated(event: EditorFactoryEvent) {
        install(event.editor)
    }

    override fun editorReleased(event: EditorFactoryEvent) {
        clear(event.editor)
        event.editor.putUserData(ALARM, null)
    }

    /** Draws the icons for [editor] and keeps them current as the file is edited. */
    fun install(editor: Editor) {
        if (fileOf(editor) == null) {
            return
        }
        refresh(editor)
        editor.document.addDocumentListener(
            object : DocumentListener {
                override fun documentChanged(event: DocumentEvent) {
                    // Coalesced: a rescan per keystroke would run on the EDT.
                    val alarm = editor.getUserData(ALARM) ?: return
                    alarm.cancelAllRequests()
                    alarm.addRequest({ if (!editor.isDisposed) refresh(editor) }, RESCAN_DELAY_MS)
                }
            },
            newAlarmFor(editor),
        )
    }

    private fun newAlarmFor(editor: Editor): Disposable {
        val alarm = Alarm(Alarm.ThreadToUse.SWING_THREAD, parent)
        editor.putUserData(ALARM, alarm)
        return alarm
    }

    private fun refresh(editor: Editor) {
        val file = fileOf(editor) ?: return
        clear(editor)
        val text = editor.document.charsSequence
        if (text.length > MAX_SCANNED_CHARS) {
            return
        }
        val added = mutableListOf<RangeHighlighter>()
        for (function in SolFunctionScanner.scan(text)) {
            if (function.offset >= editor.document.textLength) {
                continue
            }
            val line = editor.document.getLineNumber(function.offset)
            val highlighter =
                editor.markupModel.addLineHighlighter(null, line, HighlighterLayer.ADDITIONAL_SYNTAX)
            highlighter.gutterIconRenderer = ProveGutterIconRenderer(project, file, function)
            added += highlighter
        }
        editor.putUserData(HIGHLIGHTERS, added)
    }

    private fun clear(editor: Editor) {
        editor.getUserData(HIGHLIGHTERS)?.forEach { editor.markupModel.removeHighlighter(it) }
        editor.putUserData(HIGHLIGHTERS, null)
    }

    /** The `.sol` file [editor] shows, or null when the icons do not belong there. */
    private fun fileOf(editor: Editor): VirtualFile? {
        if (editor.project != project || editor.editorKind != EditorKind.MAIN_EDITOR) {
            return null
        }
        val file = FileDocumentManager.getInstance().getFile(editor.document) ?: return null
        return file.takeIf { it.extension.equals("sol", ignoreCase = true) }
    }

    private companion object {
        const val RESCAN_DELAY_MS = 300
        const val MAX_SCANNED_CHARS = 1_000_000
        val HIGHLIGHTERS = Key.create<MutableList<RangeHighlighter>>("solkey.gutter.highlighters")
        val ALARM = Key.create<Alarm>("solkey.gutter.alarm")
    }
}

/**
 * The clickable ▶. [equals] and [hashCode] are abstract on [com.intellij.openapi.editor.markup.GutterIconRenderer]
 * and carry real weight: without them the platform cannot tell one refresh's icons from the next.
 */
private class ProveGutterIconRenderer(
    private val project: Project,
    private val file: VirtualFile,
    private val function: SolFunction,
) : com.intellij.openapi.editor.markup.GutterIconRenderer() {

    override fun getIcon(): Icon = AllIcons.Actions.Execute

    override fun getTooltipText(): String {
        val qualified = function.contract?.let { "$it.${function.name}" } ?: function.name
        return "Prove $qualified in KeYther"
    }

    override fun getAlignment(): Alignment = Alignment.LEFT

    /**
     * What gives the icon the pointing-hand cursor. It defaults to false, and without it the ▶
     * still runs on click but looks inert — there is no affordance saying it can be pressed.
     */
    override fun isNavigateAction(): Boolean = true

    override fun getClickAction(): AnAction = ProveFunctionAction(project, file, function)

    override fun equals(other: Any?): Boolean =
        other is ProveGutterIconRenderer && other.file == file && other.function == function

    override fun hashCode(): Int = 31 * file.hashCode() + function.hashCode()
}
