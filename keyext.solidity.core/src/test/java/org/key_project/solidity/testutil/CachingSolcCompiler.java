/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.testutil;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;

import org.key_project.solidity.program.parser.SolcCompiler;
import org.key_project.solidity.program.parser.WasmSolcCompiler;

public final class CachingSolcCompiler implements SolcCompiler {

    private final Path directory;

    private final byte[] compilerDigest;

    public CachingSolcCompiler(Path directory) throws IOException {
        this.directory = Files.createDirectories(directory);
        try (InputStream soljson = Objects.requireNonNull(
            WasmSolcCompiler.class.getResourceAsStream("/soljson.js"), "soljson.js")) {
            this.compilerDigest = sha256().digest(soljson.readAllBytes());
        }
    }

    @Override
    public String compile(String standardJsonInput) throws IOException {
        MessageDigest digest = sha256();
        digest.update(compilerDigest);
        digest.update(standardJsonInput.getBytes(StandardCharsets.UTF_8));
        Path entry = directory.resolve(HexFormat.of().formatHex(digest.digest()) + ".json");
        if (Files.exists(entry)) {
            return Files.readString(entry);
        }
        String output = WasmSolcCompiler.get().compile(standardJsonInput);
        Path partial = Files.createTempFile(directory, "entry", ".tmp");
        Files.writeString(partial, output);
        Files.move(partial, entry, StandardCopyOption.REPLACE_EXISTING,
            StandardCopyOption.ATOMIC_MOVE);
        return output;
    }

    @Override
    public String version() {
        return WasmSolcCompiler.get().version();
    }

    private static MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
