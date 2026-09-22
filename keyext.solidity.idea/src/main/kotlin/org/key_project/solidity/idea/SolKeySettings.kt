package org.key_project.solidity.idea

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

/**
 * Where the SolKey jars and the JVM to run them on are.
 *
 * Inside the repository the jars need no configuring: [SolKeyLauncher] finds the Gradle root
 * beside the `.sol` file and builds them. They are the fallback for a Solidity project of one's
 * own. The JDK is the exception — it is worth setting anywhere the IDE does not already run on a
 * 21, since that is the only JVM solc works under.
 */
@Service(Service.Level.APP)
@State(name = "SolKeySettings", storages = [Storage("solkey.xml")])
class SolKeySettings : PersistentStateComponent<SolKeySettings.State> {

    class State {
        /** Path to `keyext.solidity.gui-exe.jar`, empty when unset. */
        @JvmField var keytherJarPath: String = ""

        /** Path to `keyext.solidity.core-exe.jar`, empty when unset. */
        @JvmField var cliJarPath: String = ""

        /** Home of a JDK 21, empty when unset. */
        @JvmField var jdkPath: String = ""
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

    var cliJarPath: String
        get() = state.cliJarPath
        set(value) {
            state.cliJarPath = value
        }

    var jdkPath: String
        get() = state.jdkPath
        set(value) {
            state.jdkPath = value
        }

    /** The jar configured for [tool], empty when unset. */
    fun jarFor(tool: SolKeyTool): String =
        if (tool.usesCoreJar) cliJarPath else keytherJarPath

    companion object {
        fun getInstance(): SolKeySettings =
            ApplicationManager.getApplication().getService(SolKeySettings::class.java)
    }
}
