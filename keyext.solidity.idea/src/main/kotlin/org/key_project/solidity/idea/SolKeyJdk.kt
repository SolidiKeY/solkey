package org.key_project.solidity.idea

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.CapturingProcessHandler
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * Finds the JVM SolKey has to run on.
 *
 * solc is a WebAssembly build interpreted by GraalWasm, and the Truffle release that runs it calls
 * a `sun.misc.Unsafe` method JDK 24 removed — so every tool here, KeYther included, dies on the
 * first contract under a newer JVM. The IDE's own JBR is 21 today and will not stay 21, hence this
 * search rather than `System.getProperty("java.home")` alone. It is the same order `run-key.sh`
 * walks, with the project SDK added because the IDE knows about it and a shell does not.
 */
object SolKeyJdk {

    private const val REQUIRED_VERSION = "21"

    private val jitSupport = ConcurrentHashMap<String, Boolean>()

    /** The `java` of the first JDK 21 found, or null when there is none. */
    fun javaExecutable(project: Project): String? =
        candidates(project).asSequence()
            .filter { it.isNotBlank() }
            .map { executableIn(it) }
            .firstOrNull { it != null && isVersion21(it) }

    private fun candidates(project: Project): List<String> = listOfNotNull(
        SolKeySettings.getInstance().jdkPath,
        System.getenv("SOLKEY_JAVA_HOME"),
        System.getenv("JAVA_HOME"),
        runCatching { ProjectRootManager.getInstance(project).projectSdk?.homePath }.getOrNull(),
        System.getProperty("java.home"),
    )

    private fun executableIn(home: String): String? {
        val name = if (isWindows()) "java.exe" else "java"
        val direct = File(File(home, "bin"), name)
        if (direct.isFile) {
            return direct.absolutePath
        }
        // A JRE path already pointing at bin/java, or a bare `java` on the PATH.
        val asExecutable = File(home)
        return if (asExecutable.isFile && asExecutable.name.startsWith("java")) {
            asExecutable.absolutePath
        } else {
            null
        }
    }

    /**
     * Whether `java` is a 21. The `release` file next to it answers without starting a JVM, which
     * matters because this runs on every click; asking the JVM itself is the fallback for a
     * layout that has no `release`.
     */
    private fun isVersion21(java: String): Boolean {
        val home = File(java).parentFile?.parentFile
        val release = home?.let { File(it, "release") }
        if (release != null && release.isFile) {
            val version = runCatching { release.readLines() }.getOrDefault(emptyList())
                .firstOrNull { it.startsWith("JAVA_VERSION=") }
                ?.substringAfter('=')?.trim('"', ' ')
            if (version != null) {
                return version == REQUIRED_VERSION || version.startsWith("$REQUIRED_VERSION.")
            }
        }
        val output = runCommand(java, "-XshowSettings:properties", "-version") ?: return false
        return output.lineSequence()
            .map { it.trim() }
            .any { it == "java.specification.version = $REQUIRED_VERSION" }
    }

    /**
     * The JVM arguments that let the Graal compiler compile solc instead of interpreting it —
     * about ten times the speed, and the jars are staged beside the fat jar by `shadowJar`. Empty
     * when they are not there or this JVM will not take them.
     */
    fun graalJitArguments(root: File, java: String): List<String> {
        val jars = File(root, "keyext.solidity.core/build/libs/graal-compiler")
            .listFiles { file -> file.name.endsWith(".jar") }
            ?.sorted()
            ?: return emptyList()
        if (jars.isEmpty()) {
            return emptyList()
        }
        val upgradePath = jars.joinToString(File.pathSeparator) { it.absolutePath }
        val arguments = listOf(
            "-XX:+UnlockExperimentalVMOptions",
            "-XX:+EnableJVMCI",
            "--upgrade-module-path=$upgradePath",
        )
        val supported = jitSupport.computeIfAbsent("$java|$upgradePath") {
            runCommand(java, *arguments.toTypedArray(), "-version") != null
        }
        return if (supported) arguments else emptyList()
    }

    /** The output of a short-lived probe, or null when it failed to run or exited non-zero. */
    private fun runCommand(vararg command: String): String? = runCatching {
        val output = CapturingProcessHandler(GeneralCommandLine(*command))
            .runProcess(PROBE_TIMEOUT_MS)
        if (output.exitCode == 0) output.stdout + output.stderr else null
    }.getOrNull()

    private const val PROBE_TIMEOUT_MS = 10_000

    private fun isWindows(): Boolean =
        System.getProperty("os.name").orEmpty().startsWith("Windows", ignoreCase = true)
}
