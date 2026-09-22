package org.key_project.solidity.idea

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.FormBuilder
import javax.swing.JComponent

/** Settings | Tools | SolKey — the one thing worth configuring, and only outside the repository. */
class SolKeyConfigurable : Configurable {

    private val jarPath = TextFieldWithBrowseButton()

    override fun getDisplayName(): String = "SolKey"

    override fun createComponent(): JComponent {
        jarPath.addBrowseFolderListener(
            null,
            FileChooserDescriptorFactory.createSingleFileDescriptor("jar")
                .withTitle("Select the KeYther Jar"),
        )
        return FormBuilder.createFormBuilder()
            .addLabeledComponent("KeYther jar:", jarPath)
            .addComponentToRightColumn(
                JBLabel(
                    "<html>Only needed outside the solkey repository. Inside it, the plugin runs " +
                        "<code>:keyext.solidity.gui:solidityGui</code> and needs nothing here.<br>" +
                        "Build the jar with <code>./gradlew :keyext.solidity.gui:shadowJar</code>; " +
                        "it lands in " +
                        "<code>keyext.solidity.gui/build/libs/keyext.solidity.gui-exe.jar</code>." +
                        "</html>",
                ),
            )
            .addComponentFillVertically(javax.swing.JPanel(), 0)
            .panel
    }

    override fun isModified(): Boolean = jarPath.text != SolKeySettings.getInstance().keytherJarPath

    override fun apply() {
        SolKeySettings.getInstance().keytherJarPath = jarPath.text.trim()
    }

    override fun reset() {
        jarPath.text = SolKeySettings.getInstance().keytherJarPath
    }
}
