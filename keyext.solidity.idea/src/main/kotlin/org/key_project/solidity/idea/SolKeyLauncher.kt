package org.key_project.solidity.idea

import com.intellij.execution.RunContentExecutor
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.process.OSProcessHandler
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessListener
import com.intellij.notification.Notification
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.externalSystem.model.execution.ExternalSystemTaskExecutionSettings
import com.intellij.openapi.externalSystem.service.execution.ProgressExecutionMode
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import java.io.File
import java.nio.file.Files
import org.jetbrains.plugins.gradle.util.GradleConstants

/** Runs one [SolKeyTool] on one [SolKeyTarget] of a `.sol` file. */
object SolKeyLauncher {

    private const val NOTIFICATIONS = "SolKey"

    /**
     * Runs [tool] on [target].
     *
     * Two routes, and the difference is not small. Running the fat jar is a plain `java -jar`:
     * the whole launch, taclet base included, is about as long as Gradle spends merely
     * *configuring* this repository's two dozen subprojects before it does any work. So the jar
     * is used whenever it is present and no source is newer than it, and Gradle is the fallback
     * — which also rebuilds the jar, so the click after an edit to SolKey is the only slow one.
     */
    fun launch(project: Project, tool: SolKeyTool, file: VirtualFile, target: SolKeyTarget) {
        // Every tool here is a separate process that reads the file from disk. Without this, an
        // edit still sitting in the editor is invisible to it, and the run silently reports on
        // the previous version of the contract — which looks exactly like a tool that does not
        // work. Run configurations save for the same reason.
        ApplicationManager.getApplication().invokeAndWait {
            FileDocumentManager.getInstance().saveAllDocuments()
        }
        // The staleness check walks two source trees, and finding a JDK may start a probe;
        // neither belongs on the EDT, nor does starting the process itself.
        ApplicationManager.getApplication().executeOnPooledThread {
            val java = SolKeyJdk.javaExecutable(project)
            if (java == null) {
                notifyMissingJdk(project)
                return@executeOnPooledThread
            }
            val root = findSolkeyRoot(file)
            if (root == null) {
                runConfiguredJar(project, tool, java, file, target)
                return@executeOnPooledThread
            }
            val jar = File(root, tool.jar)
            if (jar.isFile && !isStale(root, tool, jar)) {
                val jvmArguments = SolKeyJdk.graalJitArguments(root, java)
                runJar(project, tool, java, jvmArguments, jar.absolutePath, file, target)
            } else {
                ApplicationManager.getApplication()
                    .invokeLater { runGradle(project, tool, root, file, target) }
            }
        }
    }

    /** Whether anything the tool is built from is newer than the jar built from it. */
    private fun isStale(root: File, tool: SolKeyTool, jar: File): Boolean {
        // `shadowJar` is what stages the Graal compiler beside the jar, so a jar without it was
        // built before that step existed — or half-cleaned since.
        if (tool.usesCoreJar &&
            !File(root, "keyext.solidity.core/build/libs/graal-compiler").isDirectory
        ) {
            return true
        }
        val built = jar.lastModified()
        return tool.sourceTrees.any { relative ->
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
     * Runs the Gradle task, so the tool is rebuilt if it is stale and its output lands in the Run
     * tool window. This also uses the IDE's configured Gradle JVM rather than the JBR the IDE
     * itself runs on, which is what keeps the build on Java 21.
     */
    private fun runGradle(
        project: Project,
        tool: SolKeyTool,
        root: File,
        file: VirtualFile,
        target: SolKeyTarget,
    ) {
        val settings = ExternalSystemTaskExecutionSettings().apply {
            externalProjectPath = root.absolutePath
            // shadowJar first, so this slow launch leaves a fresh jar behind and the next click
            // takes the fast route.
            taskNames = listOf(tool.jarTask, tool.gradleTask)
            scriptParameters =
                SolKeyCommand.gradleParameters(tool, target, file.path).joinToString(" ")
            externalSystemIdString = GradleConstants.SYSTEM_ID.id
            executionName = "${tool.displayName}: ${target.label}"
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
    private fun runConfiguredJar(
        project: Project,
        tool: SolKeyTool,
        java: String,
        file: VirtualFile,
        target: SolKeyTarget,
    ) {
        val jar = SolKeySettings.getInstance().jarFor(tool)
        if (jar.isBlank() || !File(jar).isFile) {
            notifyMissingJar(project, tool, jar)
            return
        }
        // Without the checkout there is nowhere to find the staged Graal compiler, so solc is
        // interpreted here. It is slower, not broken.
        runJar(project, tool, java, emptyList(), jar, file, target)
    }

    /**
     * Starts the tool straight from the fat jar.
     *
     * KeYther reports what it cannot prove in its own dialog, so it needs no console — only a
     * non-zero exit, which means the command line itself was rejected, is worth surfacing. The
     * two headless tools have nowhere else to speak, and get one.
     */
    private fun runJar(
        project: Project,
        tool: SolKeyTool,
        java: String,
        jvmArguments: List<String>,
        jar: String,
        file: VirtualFile,
        target: SolKeyTarget,
    ) {
        val command = GeneralCommandLine(java).apply {
            addParameters(jvmArguments)
            addParameters("-jar", jar)
            addParameters(SolKeyCommand.arguments(tool, target, file.path))
            workDirectory = File(file.path).parentFile
        }
        if (tool.showsConsole) {
            ApplicationManager.getApplication().invokeLater {
                // RunContentExecutor owns startNotify(), and has to be built on the EDT.
                RunContentExecutor(project, OSProcessHandler(command))
                    .withTitle("${tool.displayName}: ${target.label}")
                    .withActivateToolWindow(true)
                    .run()
            }
            return
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
                            "${tool.displayName} exited with ${event.exitCode}",
                            output.toString().trim().ifEmpty { "No output." },
                            NotificationType.ERROR,
                        )
                    }
                }
            },
        )
        handler.startNotify()
    }

    private fun notifyMissingJar(project: Project, tool: SolKeyTool, configured: String) {
        val name = File(tool.jar).name
        val detail =
            if (configured.isBlank()) "No $name is configured."
            else "The configured $name does not exist: $configured"
        withSettingsAction(
            project,
            NotificationGroupManager.getInstance()
                .getNotificationGroup(NOTIFICATIONS)
                .createNotification(
                    "Cannot run ${tool.displayName}",
                    "$detail This project is not a solkey checkout, so the jar has to be pointed " +
                        "at in Settings | Tools | SolKey. Build it with " +
                        "'./gradlew ${tool.jarTask}'.",
                    NotificationType.ERROR,
                ),
        )
    }

    private fun notifyMissingJdk(project: Project) {
        withSettingsAction(
            project,
            NotificationGroupManager.getInstance()
                .getNotificationGroup(NOTIFICATIONS)
                .createNotification(
                    "No JDK 21 found",
                    "SolKey runs solc as WebAssembly, and the Truffle release that interprets it " +
                        "needs a JDK 21 — a 24 or newer fails on the first contract. Point " +
                        "Settings | Tools | SolKey at one, or set SOLKEY_JAVA_HOME.",
                    NotificationType.ERROR,
                ),
        )
    }

    private fun withSettingsAction(project: Project, notification: Notification) {
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
}
