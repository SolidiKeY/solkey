package org.key_project.solidity.idea

/**
 * A public function declaration found in the text of a `.sol` file. [contract] is null for a
 * file-level (free) function; [offset] is the offset of the `function` keyword and [endOffset] the
 * offset just past its body, so a caret can be resolved to the function it sits in.
 */
data class SolFunction(
    val contract: String?,
    val name: String,
    val offset: Int,
    val endOffset: Int,
) {
    operator fun contains(caret: Int): Boolean = caret in offset until endOffset
}

/**
 * Finds the functions worth offering a proof for, by scanning the text rather than the PSI.
 *
 * The IDE only has Solidity PSI when a third-party Solidity plugin is installed, so anything built
 * on `LineMarkerProvider` would work for some users and silently do nothing for the rest. This
 * scanner is the price of not caring.
 *
 * It only has to be approximate. The authority on what can be proved is `SolidityOutline`, which
 * reads solc's AST and reports the reason a function is unsupported; KeYther applies it on launch.
 * A false positive here costs one error dialog, a false negative one missing icon.
 *
 * Matching `SolidityOutline.functionsOf`, a declaration qualifies when it is a `function` (not a
 * constructor, modifier, `receive` or `fallback`), is `public`, and has a body.
 */
object SolFunctionScanner {

    private val IDENTIFIER_START = { c: Char -> c.isLetter() || c == '_' || c == '$' }
    private val IDENTIFIER_PART = { c: Char -> c.isLetterOrDigit() || c == '_' || c == '$' }

    private val CONTAINER_KEYWORDS = setOf("contract", "library", "interface")

    fun scan(text: CharSequence): List<SolFunction> {
        val found = mutableListOf<SolFunction>()
        // One frame per open brace; a frame carries the container name the brace belongs to.
        val containers = ArrayDeque<String?>()
        var pending: String? = null
        var i = 0

        while (i < text.length) {
            val c = text[i]
            when {
                c == '/' && i + 1 < text.length && text[i + 1] == '/' -> i = skipLineComment(text, i)
                c == '/' && i + 1 < text.length && text[i + 1] == '*' -> i = skipBlockComment(text, i)
                c == '"' || c == '\'' -> i = skipString(text, i)
                c == '{' -> {
                    containers.addLast(pending)
                    pending = null
                    i++
                }
                c == '}' -> {
                    containers.removeLastOrNull()
                    i++
                }
                c == ';' -> {
                    // An abstract declaration or an import: whatever was pending never opens a body.
                    pending = null
                    i++
                }
                IDENTIFIER_START(c) -> {
                    val end = identifierEnd(text, i)
                    val word = text.subSequence(i, end).toString()
                    when {
                        word in CONTAINER_KEYWORDS -> {
                            val name = readIdentifierAfter(text, end)
                            pending = name?.first
                            i = name?.second ?: end
                        }
                        word == "function" -> {
                            val function = readFunction(text, i, end, containers.lastOrNull { it != null })
                            if (function != null) {
                                found += function.first
                            }
                            i = function?.second ?: end
                        }
                        else -> i = end
                    }
                }
                else -> i++
            }
        }
        return found
    }

    /**
     * Reads one `function` declaration starting at [keywordStart], where [nameStart] is just past
     * the keyword. Returns the accepted declaration and where to resume, or null (with where to
     * resume) when the declaration is not one we offer a proof for.
     */
    private fun readFunction(
        text: CharSequence,
        keywordStart: Int,
        nameStart: Int,
        contract: String?,
    ): Pair<SolFunction, Int>? {
        val name = readIdentifierAfter(text, nameStart) ?: return null
        val afterParameters = skipBalancedParentheses(text, name.second) ?: return null
        // Everything from the parameter list to the body or the semicolon: the modifier run.
        val (modifiers, end, hasBody) = readModifierRun(text, afterParameters)
        if (!hasBody || !containsWord(modifiers, "public")) {
            return null
        }
        // Looked at, not consumed: the main loop still walks the body, so its braces keep the
        // container stack balanced.
        val bodyEnd = skipBalancedBraces(text, end) ?: text.length
        return SolFunction(contract, name.first, keywordStart, bodyEnd) to end
    }

    /**
     * The offset just past the brace-balanced block starting at [from], which must be a `{`. Null
     * when the block is never closed.
     */
    private fun skipBalancedBraces(text: CharSequence, from: Int): Int? {
        if (from >= text.length || text[from] != '{') {
            return null
        }
        var depth = 0
        var i = from
        while (i < text.length) {
            val c = text[i]
            when {
                c == '/' && i + 1 < text.length && text[i + 1] == '/' -> i = skipLineComment(text, i)
                c == '/' && i + 1 < text.length && text[i + 1] == '*' -> i = skipBlockComment(text, i)
                c == '"' || c == '\'' -> i = skipString(text, i)
                c == '{' -> {
                    depth++
                    i++
                }
                c == '}' -> {
                    depth--
                    i++
                    if (depth == 0) {
                        return i
                    }
                }
                else -> i++
            }
        }
        return null
    }

    /**
     * The text between the parameter list and the body, plus where it ends and whether a body
     * followed. Nested parentheses (`returns (uint)`, a modifier taking arguments) are skipped
     * whole, so a `{` inside them cannot be mistaken for the body.
     */
    private fun readModifierRun(text: CharSequence, from: Int): Triple<String, Int, Boolean> {
        val run = StringBuilder()
        var i = from
        while (i < text.length) {
            val c = text[i]
            when {
                c == '/' && i + 1 < text.length && text[i + 1] == '/' -> i = skipLineComment(text, i)
                c == '/' && i + 1 < text.length && text[i + 1] == '*' -> i = skipBlockComment(text, i)
                c == '"' || c == '\'' -> i = skipString(text, i)
                c == '(' -> i = skipBalancedParentheses(text, i) ?: text.length
                // Stop *at* the terminator, never past it: the main loop owns the brace stack, and
                // swallowing the body's `{` here would make its `}` pop the contract's frame.
                c == '{' -> return Triple(run.toString(), i, true)
                c == ';' -> return Triple(run.toString(), i, false)
                c == '}' -> return Triple(run.toString(), i, false)
                else -> {
                    run.append(c)
                    i++
                }
            }
        }
        return Triple(run.toString(), text.length, false)
    }

    /** Whether [haystack] contains [word] as a whole word, so `publicThing` is not `public`. */
    private fun containsWord(haystack: String, word: String): Boolean {
        var from = 0
        while (true) {
            val at = haystack.indexOf(word, from)
            if (at < 0) {
                return false
            }
            val before = at == 0 || !IDENTIFIER_PART(haystack[at - 1])
            val afterAt = at + word.length
            val after = afterAt == haystack.length || !IDENTIFIER_PART(haystack[afterAt])
            if (before && after) {
                return true
            }
            from = at + 1
        }
    }

    /** The identifier following [from] (skipping whitespace and comments), and its end. */
    private fun readIdentifierAfter(text: CharSequence, from: Int): Pair<String, Int>? {
        val start = skipTrivia(text, from)
        if (start >= text.length || !IDENTIFIER_START(text[start])) {
            return null
        }
        val end = identifierEnd(text, start)
        return text.subSequence(start, end).toString() to end
    }

    private fun identifierEnd(text: CharSequence, from: Int): Int {
        var i = from
        while (i < text.length && IDENTIFIER_PART(text[i])) {
            i++
        }
        return i
    }

    /**
     * Skips the parenthesised group starting at the next `(` after [from], and returns the offset
     * just past its matching `)`. Null when there is no `(` there or it is never closed.
     */
    private fun skipBalancedParentheses(text: CharSequence, from: Int): Int? {
        val start = skipTrivia(text, from)
        if (start >= text.length || text[start] != '(') {
            return null
        }
        var depth = 0
        var i = start
        while (i < text.length) {
            val c = text[i]
            when {
                c == '/' && i + 1 < text.length && text[i + 1] == '/' -> i = skipLineComment(text, i)
                c == '/' && i + 1 < text.length && text[i + 1] == '*' -> i = skipBlockComment(text, i)
                c == '"' || c == '\'' -> i = skipString(text, i)
                c == '(' -> {
                    depth++
                    i++
                }
                c == ')' -> {
                    depth--
                    i++
                    if (depth == 0) {
                        return i
                    }
                }
                else -> i++
            }
        }
        return null
    }

    private fun skipTrivia(text: CharSequence, from: Int): Int {
        var i = from
        while (i < text.length) {
            val c = text[i]
            i = when {
                c.isWhitespace() -> i + 1
                c == '/' && i + 1 < text.length && text[i + 1] == '/' -> skipLineComment(text, i)
                c == '/' && i + 1 < text.length && text[i + 1] == '*' -> skipBlockComment(text, i)
                else -> return i
            }
        }
        return i
    }

    private fun skipLineComment(text: CharSequence, from: Int): Int {
        val end = text.indexOf('\n', from + 2)
        return if (end < 0) text.length else end + 1
    }

    private fun skipBlockComment(text: CharSequence, from: Int): Int {
        var i = from + 2
        while (i + 1 < text.length) {
            if (text[i] == '*' && text[i + 1] == '/') {
                return i + 2
            }
            i++
        }
        return text.length
    }

    private fun skipString(text: CharSequence, from: Int): Int {
        val quote = text[from]
        var i = from + 1
        while (i < text.length) {
            when (text[i]) {
                '\\' -> i += 2
                quote -> return i + 1
                '\n' -> return i + 1
                else -> i++
            }
        }
        return text.length
    }

    private fun CharSequence.indexOf(c: Char, from: Int): Int {
        var i = from
        while (i < length) {
            if (this[i] == c) {
                return i
            }
            i++
        }
        return -1
    }
}
