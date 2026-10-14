/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.proof.io;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jspecify.annotations.Nullable;

/// Renders a load failure the way a user can act on it.
///
/// [ProblemLoaderException] and the exceptions solc raises are wrappers: the innermost cause
/// carries the location-annotated parser/converter message, while the outer one is generic. Both
/// the CLI and the GUI unwrap the same way, so the same failure reads the same in both.
public final class LoadErrors {

    private LoadErrors() {}

    /// The innermost cause of a throwable, whose message is usually the most specific.
    public static Throwable rootCause(Throwable t) {
        Throwable cur = t;
        while (cur.getCause() != null && cur.getCause() != cur) {
            cur = cur.getCause();
        }
        return cur;
    }

    /// A message for [#rootCause], falling back to its type when it carries none — a bare `null`
    /// in an error dialog tells the user nothing.
    public static String describe(Throwable t) {
        Throwable root = rootCause(t);
        String message = root.getMessage();
        return message == null || message.isBlank() ? root.getClass().getSimpleName() : message;
    }

    private static final Pattern SOLC_SRC =
        Pattern.compile("\\s*\\(at src (\\d+):(\\d+):(\\d+)\\)");
    private static final Pattern SOLC_DIAGNOSTIC =
        Pattern.compile("(?m)^(\\w*Error): (.*)$[\\s\\S]*?--> (\\S+?):(\\d+):(\\d+):");
    private static final Pattern POSITION = Pattern.compile("\\s+at (\\S+?):(\\d+):(\\d+)\\b");

    public static String compact(Throwable t, @Nullable Path file) {
        String message = null;
        for (Throwable cur = t; cur != null; cur = cur.getCause() == cur ? null : cur.getCause()) {
            if (cur.getMessage() != null && !cur.getMessage().isBlank()) {
                message = cur.getMessage();
            }
        }
        if (message == null) {
            message = describe(t);
        }
        String location = file == null ? null : file.toString();
        for (Throwable cur = t; cur != null; cur = cur.getCause() == cur ? null : cur.getCause()) {
            String text = cur.getMessage();
            if (text == null) {
                continue;
            }
            Matcher diagnostic = SOLC_DIAGNOSTIC.matcher(text);
            if (diagnostic.find()) {
                return diagnostic.group(3) + ":" + diagnostic.group(4) + ":" + diagnostic.group(5)
                    + ": " + diagnostic.group(1) + ": " + diagnostic.group(2).strip();
            }
            Matcher src = SOLC_SRC.matcher(text);
            if (src.find() && file != null) {
                location = file + ":" + lineAndColumn(file, Integer.parseInt(src.group(1)));
                message = SOLC_SRC.matcher(message).replaceAll("");
                break;
            }
            Matcher position = POSITION.matcher(text);
            if (position.find()) {
                location = position.group(1) + ":" + position.group(2) + ":" + position.group(3);
                message = POSITION.matcher(message).replaceAll("");
                break;
            }
        }
        String line = category(rootCause(t)) + ": " + message.strip();
        return location == null ? line : location + ": " + line;
    }

    public static String category(Throwable t) {
        if (t.getClass().getPackageName().equals("java.lang")
                && !(t instanceof NullPointerException) && !(t instanceof ClassCastException)
                && !(t instanceof IndexOutOfBoundsException)
                && !(t instanceof StackOverflowError)) {
            return "error";
        }
        String name = t.getClass().getSimpleName().replaceAll("(Exception|Error)$", "");
        String words = name.replaceAll("([a-z0-9])([A-Z])", "$1 $2").toLowerCase();
        return words.isEmpty() ? "error" : words + " error";
    }

    private static String lineAndColumn(Path file, int byteOffset) {
        try {
            byte[] bytes = Files.readAllBytes(file);
            int line = 1;
            int lineStart = 0;
            for (int i = 0; i < Math.min(byteOffset, bytes.length); i++) {
                if (bytes[i] == '\n') {
                    line++;
                    lineStart = i + 1;
                }
            }
            return line + ":" + (byteOffset - lineStart + 1);
        } catch (IOException e) {
            return "byte " + byteOffset;
        }
    }
}
