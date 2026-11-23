package org.key_project.solidity.idea

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class KeyProblemScannerTest {

    @Test
    fun `finds the problem keyword`() {
        val text = "\\programVariables { int x; }\n\\problem {\n  true\n}\n"
        assertEquals(text.indexOf("\\problem"), KeyProblemScanner.find(text))
    }

    @Test
    fun `finds a proof obligation`() {
        val text = "\\proofObligation \"#Class : x\";"
        assertEquals(0, KeyProblemScanner.find(text))
    }

    @Test
    fun `skips comments and strings`() {
        val text = "// \\problem\n/* \\problem */\n\"\\problem\"\n\\problem { true }"
        assertEquals(text.lastIndexOf("\\problem"), KeyProblemScanner.find(text))
    }

    @Test
    fun `a longer keyword is not the problem keyword`() {
        assertNull(KeyProblemScanner.find("\\problems { }"))
    }

    @Test
    fun `a rules file has no problem`() {
        assertNull(KeyProblemScanner.find("\\rules { r { \\find(x) \\replacewith(x) }; }"))
    }
}
