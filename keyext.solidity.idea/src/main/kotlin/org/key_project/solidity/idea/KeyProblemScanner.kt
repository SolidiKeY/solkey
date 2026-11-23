package org.key_project.solidity.idea

/**
 * Finds the `\problem` (or `\proofObligation`) block of a `.key` file: the one thing a `.key` file
 * asks the prover to do, and so the one place its ▶ belongs.
 */
object KeyProblemScanner {

    private val KEYWORDS = listOf("\\problem", "\\proofObligation")

    /** The offset of the first problem keyword outside comments and strings, or null. */
    fun find(text: CharSequence): Int? {
        var i = 0
        while (i < text.length) {
            val c = text[i]
            when {
                c == '/' && i + 1 < text.length && text[i + 1] == '/' -> i = skipTo(text, i, "\n")
                c == '/' && i + 1 < text.length && text[i + 1] == '*' -> i = skipTo(text, i + 2, "*/")
                c == '"' -> i = skipString(text, i + 1)
                c == '\\' -> {
                    val keyword = KEYWORDS.firstOrNull { startsWithWord(text, i, it) }
                    if (keyword != null) {
                        return i
                    }
                    i++
                }
                else -> i++
            }
        }
        return null
    }

    private fun startsWithWord(text: CharSequence, at: Int, word: String): Boolean {
        if (at + word.length > text.length) {
            return false
        }
        for (k in word.indices) {
            if (text[at + k] != word[k]) {
                return false
            }
        }
        val next = at + word.length
        return next == text.length || !text[next].isLetterOrDigit()
    }

    private fun skipTo(text: CharSequence, from: Int, end: String): Int {
        var i = from
        while (i < text.length) {
            if (text.startsWith(end, i)) {
                return i + end.length
            }
            i++
        }
        return text.length
    }

    private fun skipString(text: CharSequence, from: Int): Int {
        var i = from
        while (i < text.length) {
            when (text[i]) {
                '\\' -> i += 2
                '"' -> return i + 1
                else -> i++
            }
        }
        return text.length
    }

    fun isKeyFile(extension: String?): Boolean = extension.equals("key", ignoreCase = true)
}
