/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.proof.io;

import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.zip.GZIPInputStream;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;

public class GZipFileRuleSource extends FileRuleSource {
    /// Instantiates a new file rule source.
    /// This is only instantiated from [#initRuleFile(Path,boolean)].
    ///
    /// @param ruleFile the file to read from.
    GZipFileRuleSource(Path ruleFile) {
        super(ruleFile);
    }

    @Override
    public InputStream getNewStream() {
        try {
            return new GZIPInputStream(ruleFile.toUri().toURL().openStream());
        } catch (IOException e) {
            throw new RuntimeException("Error while reading rules.", e);
        }
    }

    @Override
    public CharStream getCharStream() throws IOException {
        try (ReadableByteChannel channel = Channels.newChannel(getNewStream())) {
            return CharStreams.fromChannel(channel, StandardCharsets.UTF_8, 4096,
                CodingErrorAction.REPLACE, file().toString(), -1);
        }
    }
}
