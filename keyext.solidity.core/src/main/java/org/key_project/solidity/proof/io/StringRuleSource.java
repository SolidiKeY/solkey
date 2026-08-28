/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.proof.io;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.util.Objects;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.jspecify.annotations.NonNull;

import static java.nio.charset.StandardCharsets.UTF_8;

/// A [RuleSource] whose content is held in memory rather than read from disk.
///
/// The `anchor` is a path that need not exist: it only has to sit in the directory relative
/// includes and `\programSource` entries should resolve against, and to identify this source
/// uniquely — [#getExternalForm] backs `KeYFile.equals`/`hashCode`, which decide whether the
/// problem initializer considers a file already parsed.
public class StringRuleSource extends RuleSource {
    private final @NonNull String content;
    private final @NonNull Path anchor;

    StringRuleSource(String content, Path anchor) {
        this.content = Objects.requireNonNull(content);
        this.anchor = Objects.requireNonNull(anchor);
    }

    @Override
    public @NonNull Path file() {
        return anchor;
    }

    @Override
    public URL url() throws IOException {
        return anchor.toUri().toURL();
    }

    @Override
    public boolean isDirectory() {
        return false;
    }

    @Override
    public int getNumberOfBytes() {
        return content.getBytes(UTF_8).length;
    }

    @Override
    public String getExternalForm() {
        try {
            return anchor.toUri().toURL().toExternalForm();
        } catch (final MalformedURLException exception) {
            throw new RuntimeException(exception);
        }
    }

    @Override
    public InputStream getNewStream() {
        return new ByteArrayInputStream(content.getBytes(UTF_8));
    }

    @Override
    public String toString() {
        return anchor.toString();
    }

    /// A fresh stream on every call: the parser seeks the one it is given, and closing a
    /// `KeYFile` forces a re-parse from the start.
    @Override
    public CharStream getCharStream() {
        return CharStreams.fromString(content, anchor.toString());
    }
}
