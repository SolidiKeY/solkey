package org.key_project.solidity.idea

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

/**
 * Where KeYther is, for projects that are not the solkey checkout.
 *
 * Inside the repository nothing needs configuring: [KeYtherLauncher] finds the Gradle root beside
 * the `.sol` file and runs `:keyext.solidity.gui:solidityGui`. This is only the fallback for a
 * Solidity project of one's own.
 */
@Service(Service.Level.APP)
@State(name = "SolKeySettings", storages = [Storage("solkey.xml")])
class SolKeySettings : PersistentStateComponent<SolKeySettings.State> {

    class State {
        /** Path to `keyext.solidity.gui-exe.jar`, empty when unset. */
        @JvmField var keytherJarPath: String = ""
    }

    private var state = State()

    override fun getState(): State = state

    override fun loadState(state: State) {
        this.state = state
    }

    var keytherJarPath: String
        get() = state.keytherJarPath
        set(value) {
            state.keytherJarPath = value
        }

    companion object {
        fun getInstance(): SolKeySettings =
            ApplicationManager.getApplication().getService(SolKeySettings::class.java)
    }
}
