/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.parser;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.jspecify.annotations.Nullable;

import static java.nio.charset.StandardCharsets.UTF_8;

public final class NativeSolcCompiler implements SolcCompiler {

    public static final String LOCATION_PROPERTY = "solkey.solc";

    public static final String LOCATION_ENVIRONMENT = "SOLKEY_SOLC";

    public static final String STAGED_DIRECTORY = "solc-native";

    private static final String PINNED_VERSION_RESOURCE = "/solc.version";

    private static final String VERSION_PREFIX = "Version: ";

    private static final long VERSION_TIMEOUT_SECONDS = 10;

    private final Path executable;

    private final String version;

    private final SolcOutputCache cache;

    private NativeSolcCompiler(Path executable, String version) {
        this.executable = executable;
        this.version = version;
        this.cache = SolcOutputCache.forCompiler(
            new ByteArrayInputStream(("native " + version).getBytes(UTF_8)));
    }

    public static Optional<NativeSolcCompiler> find() {
        String pinned = pinnedVersion();
        if (pinned == null) {
            return Optional.empty();
        }
        String configured = System.getProperty(LOCATION_PROPERTY);
        if (configured != null && "off".equalsIgnoreCase(configured.trim())) {
            return Optional.empty();
        }
        for (Path candidate : candidates(configured)) {
            String version = versionOf(candidate);
            if (version != null && version.startsWith(pinned + ".")) {
                return Optional.of(new NativeSolcCompiler(candidate, version));
            }
        }
        return Optional.empty();
    }

    public Path executable() {
        return executable;
    }

    @Override
    public String compile(String standardJsonInput) throws IOException {
        String cached = cache.output(standardJsonInput);
        if (cached != null) {
            return cached;
        }
        String output = run(standardJsonInput);
        cache.storeOutput(standardJsonInput, output);
        return output;
    }

    @Override
    public String version() {
        return version;
    }

    private String run(String standardJsonInput) throws IOException {
        Process process = new ProcessBuilder(executable.toString(), "--standard-json",
            "--no-import-callback").start();
        CompletableFuture<byte[]> errors =
            CompletableFuture.supplyAsync(() -> readAll(process.getErrorStream()));
        CompletableFuture<Void> input = CompletableFuture.runAsync(() -> {
            try (OutputStream stdin = process.getOutputStream()) {
                stdin.write(standardJsonInput.getBytes(UTF_8));
            } catch (IOException ignored) {
            }
        });
        try {
            byte[] output = process.getInputStream().readAllBytes();
            int exit = process.waitFor();
            input.get();
            if (exit != 0) {
                throw new IOException(executable + " --standard-json exited with " + exit + ": "
                    + new String(errors.get(), UTF_8).strip());
            }
            return new String(output, UTF_8);
        } catch (InterruptedException e) {
            process.destroyForcibly();
            Thread.currentThread().interrupt();
            throw new IOException("interrupted while " + executable + " was compiling", e);
        } catch (ExecutionException e) {
            throw new IOException(executable + " failed", e.getCause());
        }
    }

    private static byte[] readAll(InputStream stream) {
        try (stream) {
            return stream.readAllBytes();
        } catch (IOException e) {
            return new byte[0];
        }
    }

    private static @Nullable String pinnedVersion() {
        try (InputStream stream =
            NativeSolcCompiler.class.getResourceAsStream(PINNED_VERSION_RESOURCE)) {
            if (stream == null) {
                return null;
            }
            String pinned = new String(stream.readAllBytes(), UTF_8).strip();
            return pinned.isEmpty() ? null : pinned;
        } catch (IOException e) {
            return null;
        }
    }

    private static List<Path> candidates(@Nullable String configured) {
        List<Path> candidates = new ArrayList<>();
        if (configured != null && !configured.isBlank()) {
            candidates.add(Path.of(configured.trim()));
        }
        String environment = System.getenv(LOCATION_ENVIRONMENT);
        if (environment != null && !environment.isBlank()) {
            candidates.add(Path.of(environment.trim()));
        }
        Path staged = stagedBesideJar();
        if (staged != null) {
            candidates.add(staged);
        }
        String path = System.getenv("PATH");
        if (path != null) {
            for (String directory : path.split(File.pathSeparator)) {
                if (!directory.isBlank()) {
                    candidates.add(Path.of(directory).resolve(executableName()));
                }
            }
        }
        return candidates;
    }

    private static @Nullable Path stagedBesideJar() {
        try {
            var source = NativeSolcCompiler.class.getProtectionDomain().getCodeSource();
            if (source == null) {
                return null;
            }
            Path location = Path.of(source.getLocation().toURI());
            if (!Files.isRegularFile(location) || location.getParent() == null) {
                return null;
            }
            return location.getParent().resolve(STAGED_DIRECTORY).resolve(executableName());
        } catch (URISyntaxException | RuntimeException e) {
            return null;
        }
    }

    private static String executableName() {
        return System.getProperty("os.name", "").startsWith("Windows") ? "solc.exe" : "solc";
    }

    private static @Nullable String versionOf(Path candidate) {
        if (!Files.isRegularFile(candidate) || !Files.isExecutable(candidate)) {
            return null;
        }
        try {
            Process process = new ProcessBuilder(candidate.toString(), "--version")
                    .redirectErrorStream(true).start();
            process.getOutputStream().close();
            byte[] output = process.getInputStream().readAllBytes();
            if (!process.waitFor(VERSION_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                return null;
            }
            if (process.exitValue() != 0) {
                return null;
            }
            return new String(output, UTF_8).lines()
                    .filter(line -> line.startsWith(VERSION_PREFIX))
                    .map(line -> line.substring(VERSION_PREFIX.length()).strip())
                    .findFirst().orElse(null);
        } catch (IOException e) {
            return null;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }
}
