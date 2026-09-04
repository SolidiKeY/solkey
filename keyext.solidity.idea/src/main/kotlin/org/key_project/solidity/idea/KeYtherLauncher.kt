package org.key_project.solidity.idea

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.process.OSProcessHandler
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessListener
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.externalSystem.model.execution.ExternalSystemTaskExecutionSettings
import com.intellij.openapi.externalSystem.service.execution.ProgressExecutionMode
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import java.io.File
import java.nio.file.Files
import org.jetbrains.plugins.gradle.util.GradleConstants

/** Opens KeYther on one function of a `.sol` file. */
object KeYtherLauncher {

    private const val GUI_TASK = ":keyext.solidity.gui:solidityGui"
    private const val JAR_TASK = ":keyext.solidity.gui:shadowJar"
    private const val GUI_JAR = "keyext.solidity.gui/build/libs/keyext.solidity.gui-exe.jar"
    private const val NOTIFICATIONS = "SolKey"

    /**
     * The trees whose edits make the fat jar stale. The GUI is what runs; core is what it is
     * built on, and its resources carry the solc binary.
     */
    private val SOURCE_TREES = listOf("keyext.solidity.gui/src", "keyext.solidity.core/src")

    /**
     * Opens KeYther on `function`.
     *
     * Two routes, and the difference is not small. Running the fat jar is a plain `java -jar`:
     * the whole launch, taclet base included, is about as long as Gradle spends merely
     * *configuring* this repository's two dozen subprojects before it does any work. So the jar
     * is used whenever it is present and no source is newer than it, and Gradle is the fallback
     * — which also rebuilds the jar, so the click after an edit to SolKey is the only slow one.
     */
    fun launch(project: Project, file: VirtualFile, function: SolFunction) {
        // The staleness check walks two source trees; neither that nor starting a process belongs
        // on the EDT.
        ApplicationManager.getApplication().executeOnPooledThread {
            val root = findSolkeyRoot(file)
            if (root == null) {
                runConfiguredJar(project, file, function)
                return@executeOnPooledThread
            }
            val jar = File(root, GUI_JAR)
            if (jar.isFile && !isStale(root, jar)) {
                runJar(project, jar.absolutePath, file, function)
            } else {
                ApplicationManager.getApplication()
                    .invokeLater { runGradle(project, root, file, function) }
            }
        }
    }

    /** Whether anything KeYther is built from is newer than the jar built from it. */
    private fun isStale(root: File, jar: File): Boolean {
        val built = jar.lastModified()
        return SOURCE_TREES.any { relative ->
            val tree = File(root, relative)
            tree.isDirectory && runCatching {
                Files.walk(tree.toPath()).use { paths ->
                    paths.anyMatch { Files.isRegularFile(it) && it.toFile().lastModified() > built }
                }
            }.getOrDefault(true) // unreadable tree: assume stale and let Gradle sort it out
        }
    }

    /**
     * The solkey checkout the file belongs to, or null.
     *
     * The walk starts at the file, not at the project root, so a `.sol` opened from outside the
     * project tree still finds the build that can prove it.
     */
    private fun findSolkeyRoot(file: VirtualFile): File? {
        var dir = File(file.path).parentFile
        while (dir != null) {
            if (File(dir, "keyext.solidity.gui/build.gradle").isFile) {
                return dir
            }
            dir = dir.parentFile
        }
        return null
    }

    /**
     * Runs the Gradle task, so the GUI is rebuilt if it is stale and its output lands in the Run
     * tool window. This also uses the IDE's configured Gradle JVM rather than the JBR the IDE
     * itself runs on, which is what keeps the build on Java 21.
     */
    private fun runGradle(project: Project, root: File, file: VirtualFile, function: SolFunction) {
        val parameters = buildList {
            // Quoted: `scriptParameters` is one string, split with ParametersListUtil.parse, which
            // honours double quotes — so a path with spaces survives as one argument.
            add("-PsolFile=\"${file.path}\"")
            function.contract?.let { add("-Pcontract=$it") }
            add("-Pfunction=${function.name}")
        }
        val settings = ExternalSystemTaskExecutionSettings().apply {
            externalProjectPath = root.absolutePath
            // shadowJar first, so this slow launch leaves a fresh jar behind and the next click
            // takes the fast route.
            taskNames = listOf(JAR_TASK, GUI_TASK)
            scriptParameters = parameters.joinToString(" ")
            externalSystemIdString = GradleConstants.SYSTEM_ID.id
            executionName = "KeYther: ${qualified(function)}"
        }
        ExternalSystemUtil.runTask(
            settings,
            DefaultRunExecutor.EXECUTOR_ID,
            project,
            GradleConstants.SYSTEM_ID,
            null,
            ProgressExecutionMode.IN_BACKGROUND_ASYNC,
            false,
        )
    }

    /** Outside the solkey checkout there is no Gradle build to run, only the configured jar. */
    private fun runConfiguredJar(project: Project, file: VirtualFile, function: SolFunction) {
        val jar = SolKeySettings.getInstance().keytherJarPath
        if (jar.isBlank() || !File(jar).isFile) {
            notifyMissingJar(project, jar)
            return
        }
        runJar(project, jar, file, function)
    }

    /**
     * Starts KeYther straight from the fat jar. It reports a function it cannot prove in its own
     * dialog, so the process needs no console here — only a non-zero exit, which means the command
     * line itself was rejected, is worth surfacing.
     */
    private fun runJar(project: Project, jar: String, file: VirtualFile, function: SolFunction) {
        val command = GeneralCommandLine(javaExecutable(), "-jar", jar, file.path).apply {
            function.contract?.let { addParameters("--contract", it) }
            addParameters("--function", function.name)
            workDirectory = File(file.path).parentFile
        }
        val handler = OSProcessHandler(command)
        val output = StringBuilder()
        handler.addProcessListener(
            object : ProcessListener {
                override fun onTextAvailable(event: ProcessEvent, outputType: Key<*>) {
                    output.append(event.text)
                }

                override fun processTerminated(event: ProcessEvent) {
                    if (event.exitCode != 0) {
                        notify(
                            project,
                            "KeYther exited with ${event.exitCode}",
                            output.toString().trim().ifEmpty { "No output." },
                            NotificationType.ERROR,
                        )
                    }
                }
            },
        )
        handler.startNotify()
    }

    private fun notifyMissingJar(project: Project, configured: String) {
        val detail =
            if (configured.isBlank()) "No KeYther jar is configured."
            else "The configured KeYther jar does not exist: $configured"
        val notification = NotificationGroupManager.getInstance()
            .getNotificationGroup(NOTIFICATIONS)
            .createNotification(
                "Cannot open KeYther",
                "$detail This project is not a solkey checkout, so the jar has to be pointed at " +
                    "in Settings | Tools | SolKey. Build it with " +
                    "'./gradlew :keyext.solidity.gui:shadowJar'.",
                NotificationType.ERROR,
            )
        notification.addAction(
            object : AnAction("Open Settings") {
                override fun actionPerformed(e: AnActionEvent) {
                    ShowSettingsUtil.getInstance().showSettingsDialog(project, "SolKey")
                    notification.expire()
                }
            },
        )
        notification.notify(project)
    }

    private fun notify(project: Project, title: String, body: String, type: NotificationType) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup(NOTIFICATIONS)
            .createNotification(title, body, type)
            .notify(project)
    }

    private fun javaExecutable(): String {
        val home = System.getProperty("java.home")
        val candidate = File(File(home, "bin"), if (isWindows()) "java.exe" else "java")
        return if (candidate.isFile) candidate.absolutePath else "java"
    }

    private fun isWindows(): Boolean =
        System.getProperty("os.name").orEmpty().startsWith("Windows", ignoreCase = true)

    private fun qualified(function: SolFunction): String =
        function.contract?.let { "$it.${function.name}" } ?: function.name
}
