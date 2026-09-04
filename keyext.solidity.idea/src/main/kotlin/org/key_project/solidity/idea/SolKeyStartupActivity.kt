package org.key_project.solidity.idea

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity

/** Starts drawing gutter icons in `.sol` editors, including the ones already open. */
class SolKeyStartupActivity : ProjectActivity {

    override suspend fun execute(project: Project) {
        val parent = SolKeyProjectService.getInstance(project)
        val installer = SolGutterInstaller(project, parent)
        EditorFactory.getInstance().addEditorFactoryListener(installer, parent)
        // Editors restored with the project were created before the listener existed, so without
        // this sweep the file the user left open would be the one file with no icons.
        ApplicationManager.getApplication().invokeLater {
            if (project.isDisposed) {
                return@invokeLater
            }
            FileEditorManager.getInstance(project).allEditors
                .filterIsInstance<TextEditor>()
                .forEach { installer.install(it.editor) }
        }
    }
}
