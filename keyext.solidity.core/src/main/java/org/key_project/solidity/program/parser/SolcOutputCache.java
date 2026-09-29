/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.parser;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.stream.Stream;

import org.jspecify.annotations.Nullable;

import static java.nio.charset.StandardCharsets.UTF_8;

final class SolcOutputCache {

    static final String LOCATION_PROPERTY = "solkey.solcCache";

    private static final String OUTPUT_SUFFIX = ".json";

    private static final String VERSION_FILE = "version";

    private static final int MAX_ENTRIES = 256;

    private final @Nullable Path directory;

    private SolcOutputCache(@Nullable Path directory) {
        this.directory = directory;
    }

    static SolcOutputCache disabled() {
        return new SolcOutputCache(null);
    }

    static SolcOutputCache forCompiler(InputStream compiler) {
        Path root = root();
        if (root == null) {
            return disabled();
        }
        try {
            MessageDigest digest = sha256();
            byte[] buffer = new byte[1 << 16];
            for (int read = compiler.read(buffer); read >= 0; read = compiler.read(buffer)) {
                digest.update(buffer, 0, read);
            }
            return new SolcOutputCache(
                root.resolve(HexFormat.of().formatHex(digest.digest()).substring(0, 16)));
        } catch (IOException e) {
            return disabled();
        }
    }

    private static @Nullable Path root() {
        String configured = System.getProperty(LOCATION_PROPERTY);
        if (configured != null && !configured.isBlank()) {
            return "off".equalsIgnoreCase(configured.trim()) ? null : Path.of(configured.trim());
        }
        String xdg = System.getenv("XDG_CACHE_HOME");
        Path base = xdg != null && !xdg.isBlank() ? Path.of(xdg)
                : Path.of(System.getProperty("user.home"), ".cache");
        return base.resolve("solkey").resolve("solc");
    }

    @Nullable
    String output(String input) {
        return read(outputFile(input), true);
    }

    void storeOutput(String input, String output) {
        Path file = outputFile(input);
        if (file != null && write(file, output)) {
            prune();
        }
    }

    @Nullable
    String version() {
        return read(file(VERSION_FILE), false);
    }

    void storeVersion(String version) {
        Path file = file(VERSION_FILE);
        if (file != null) {
            write(file, version);
        }
    }

    private @Nullable Path outputFile(String input) {
        return file(HexFormat.of().formatHex(sha256().digest(input.getBytes(UTF_8)))
                + OUTPUT_SUFFIX);
    }

    private @Nullable Path file(String name) {
        return directory == null ? null : directory.resolve(name);
    }

    private static @Nullable String read(@Nullable Path file, boolean touch) {
        if (file == null || !Files.isRegularFile(file)) {
            return null;
        }
        try {
            String content = Files.readString(file, UTF_8);
            if (touch) {
                Files.setLastModifiedTime(file, FileTime.fromMillis(System.currentTimeMillis()));
            }
            return content;
        } catch (IOException e) {
            return null;
        }
    }

    private static boolean write(Path file, String content) {
        try {
            Files.createDirectories(file.getParent());
            Path temporary =
                Files.createTempFile(file.getParent(), file.getFileName() + ".", ".tmp");
            try {
                Files.writeString(temporary, content, UTF_8);
                try {
                    Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
                } catch (AtomicMoveNotSupportedException e) {
                    Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
                }
            } finally {
                Files.deleteIfExists(temporary);
            }
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private void prune() {
        if (directory == null) {
            return;
        }
        try (Stream<Path> files = Files.list(directory)) {
            List<Path> outputs = files
                    .filter(path -> path.getFileName().toString().endsWith(OUTPUT_SUFFIX))
                    .sorted(Comparator.comparingLong(SolcOutputCache::lastModified).reversed())
                    .toList();
            for (Path stale : outputs.subList(Math.min(MAX_ENTRIES, outputs.size()),
                outputs.size())) {
                Files.deleteIfExists(stale);
            }
        } catch (IOException ignored) {
        }
    }

    private static long lastModified(Path path) {
        try {
            return Files.getLastModifiedTime(path).toMillis();
        } catch (IOException e) {
            return 0;
        }
    }

    private static MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
