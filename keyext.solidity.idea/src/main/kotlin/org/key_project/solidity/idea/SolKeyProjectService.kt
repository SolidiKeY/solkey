package org.key_project.solidity.idea

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project

/**
 * Parent disposable for everything the plugin registers per project: the editor listener and the
 * per-editor alarms die with the project rather than with whatever happened to create them.
 */
@Service(Service.Level.PROJECT)
class SolKeyProjectService : Disposable {

    override fun dispose() {
        // Nothing of its own; children registered against it are disposed by the platform.
    }

    companion object {
        fun getInstance(project: Project): SolKeyProjectService = project.service()
    }
}
