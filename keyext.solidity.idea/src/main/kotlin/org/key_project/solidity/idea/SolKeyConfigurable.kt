package org.key_project.solidity.idea

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.FormBuilder
import javax.swing.JComponent

/** Settings | Tools | SolKey — the jars to run outside the repository, and the JVM to run them on. */
class SolKeyConfigurable : Configurable {

    private val guiJarPath = TextFieldWithBrowseButton()
    private val cliJarPath = TextFieldWithBrowseButton()
    private val jdkPath = TextFieldWithBrowseButton()

    override fun getDisplayName(): String = "SolKey"

    override fun createComponent(): JComponent {
        guiJarPath.addBrowseFolderListener(
            null,
            FileChooserDescriptorFactory.createSingleFileDescriptor("jar")
                .withTitle("Select the KeYther Jar"),
        )
        cliJarPath.addBrowseFolderListener(
            null,
            FileChooserDescriptorFactory.createSingleFileDescriptor("jar")
                .withTitle("Select the Solidity CLI Jar"),
        )
        jdkPath.addBrowseFolderListener(
            null,
            FileChooserDescriptorFactory.createSingleFolderDescriptor()
                .withTitle("Select a JDK 21 Home"),
        )
        return FormBuilder.createFormBuilder()
            .addLabeledComponent("KeYther jar:", guiJarPath)
            .addLabeledComponent("Solidity CLI jar:", cliJarPath)
            .addComponentToRightColumn(
                JBLabel(
                    "<html>Only needed outside the solkey repository. Inside it, the plugin runs " +
                        "the Gradle build beside the file and needs neither.<br>" +
                        "Build them with <code>./gradlew :keyext.solidity.gui:shadowJar " +
                        ":keyext.solidity.core:shadowJar</code>; they land in each module's " +
                        "<code>build/libs/</code>.</html>",
                ),
            )
            .addLabeledComponent("JDK 21 home:", jdkPath)
            .addComponentToRightColumn(
                JBLabel(
                    "<html>solc runs as WebAssembly on a JVM, and the Truffle release that " +
                        "interprets it needs a JDK 21 — a 24 or newer fails on the first " +
                        "contract.<br>Leave empty to use <code>SOLKEY_JAVA_HOME</code>, " +
                        "<code>JAVA_HOME</code>, the project SDK or the IDE's own JVM, in that " +
                        "order.</html>",
                ),
            )
            .addComponentFillVertically(javax.swing.JPanel(), 0)
            .panel
    }

    override fun isModified(): Boolean {
        val settings = SolKeySettings.getInstance()
        return guiJarPath.text != settings.keytherJarPath ||
            cliJarPath.text != settings.cliJarPath ||
            jdkPath.text != settings.jdkPath
    }

    override fun apply() {
        val settings = SolKeySettings.getInstance()
        settings.keytherJarPath = guiJarPath.text.trim()
        settings.cliJarPath = cliJarPath.text.trim()
        settings.jdkPath = jdkPath.text.trim()
    }

    override fun reset() {
        val settings = SolKeySettings.getInstance()
        guiJarPath.text = settings.keytherJarPath
        cliJarPath.text = settings.cliJarPath
        jdkPath.text = settings.jdkPath
    }
}
