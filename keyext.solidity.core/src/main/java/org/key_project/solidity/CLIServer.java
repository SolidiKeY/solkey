/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.key_project.solidity.proof.init.ProblemInitializer;
import org.key_project.solidity.proof.io.RuleSourceFactory;

public final class CLIServer {

    public static final int EXIT_STALE = 3;

    private static final long IDLE_MILLIS = 30 * 60 * 1000;
    private static final long POLL_MILLIS = 5;
    private static final long HEARTBEAT_MILLIS = 1000;

    private final Path dir;
    private final Path jar;
    private final long jarModified;
    private final Path rules;
    private long rulesModified;
    private volatile long lastRequest = System.currentTimeMillis();

    private CLIServer(Path dir) throws IOException {
        this.dir = dir;
        this.jar = Path.of(CLIServer.class.getProtectionDomain().getCodeSource().getLocation()
                .getPath());
        this.jarModified = modified(jar);
        String rulesDir = System.getProperty(RuleSourceFactory.STD_TACLET_DIR_PROP_KEY);
        this.rules = rulesDir == null ? null : Path.of(rulesDir);
        this.rulesModified = rulesModified();
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.err.println("usage: CLIServer SPOOL_DIRECTORY");
            System.exit(2);
        }
        System.exit(new CLIServer(Path.of(args[0])).serve());
    }

    private int serve() throws Exception {
        Files.createDirectories(dir);
        Path alive = dir.resolve("alive");
        PrintStream originalOut = System.out;
        PrintStream originalErr = System.err;
        Thread heartbeat = new Thread(() -> beat(alive), "solkey-server-heartbeat");
        heartbeat.setDaemon(true);
        heartbeat.start();
        originalOut.println("solkey server ready in " + dir);
        try {
            while (System.currentTimeMillis() - lastRequest < IDLE_MILLIS) {
                Path request = nextRequest();
                if (request == null) {
                    Thread.sleep(POLL_MILLIS);
                    continue;
                }
                String id = request.getFileName().toString().replace(".run", "");
                List<String> args = readArgs(request);
                Files.deleteIfExists(request);
                if (modified(jar) != jarModified) {
                    finish(id, "stale");
                    originalOut.println("solkey server: the jar was rebuilt, restarting");
                    return EXIT_STALE;
                }
                if (args.equals(List.of("--stop"))) {
                    Files.writeString(dir.resolve(id + ".out"), "solkey server stopped\n");
                    finish(id, "0");
                    return 0;
                }
                try {
                    run(id, args);
                } finally {
                    System.setOut(originalOut);
                    System.setErr(originalErr);
                    lastRequest = System.currentTimeMillis();
                }
            }
            originalOut.println("solkey server: idle for 30 minutes, stopping");
            return 0;
        } finally {
            Files.deleteIfExists(alive);
        }
    }

    private void run(String id, List<String> args) throws IOException {
        long currentRules = rulesModified();
        if (currentRules != rulesModified) {
            rulesModified = currentRules;
            ProblemInitializer.resetBaseConfig();
        }
        int exit;
        try (OutputStream out = Files.newOutputStream(dir.resolve(id + ".out"));
                OutputStream err = Files.newOutputStream(dir.resolve(id + ".err"));
                OutputStream all = Files.newOutputStream(dir.resolve(id + ".all"))) {
            System.setOut(new PrintStream(new Tee(out, all), true, StandardCharsets.UTF_8));
            System.setErr(new PrintStream(new Tee(err, all), true, StandardCharsets.UTF_8));
            try {
                exit = CLI.execute(args.toArray(String[]::new));
            } catch (Throwable t) {
                System.err.println("Error: " + t);
                exit = 1;
            }
            System.out.flush();
            System.err.flush();
        }
        finish(id, Integer.toString(exit));
    }

    private Path nextRequest() throws IOException {
        List<Path> requests = new ArrayList<>();
        try (Stream<Path> files = Files.list(dir)) {
            files.filter(p -> p.getFileName().toString().endsWith(".req")).forEach(requests::add);
        }
        requests.sort(null);
        for (Path request : requests) {
            Path claimed = request.resolveSibling(
                request.getFileName().toString().replace(".req", ".run"));
            try {
                Files.move(request, claimed, StandardCopyOption.ATOMIC_MOVE);
                return claimed;
            } catch (NoSuchFileException e) {
                continue;
            }
        }
        return null;
    }

    private static List<String> readArgs(Path request) throws IOException {
        String text = Files.readString(request, StandardCharsets.UTF_8);
        if (text.endsWith("\0")) {
            text = text.substring(0, text.length() - 1);
        }
        return text.isEmpty() ? List.of() : Arrays.asList(text.split("\0", -1));
    }

    private void finish(String id, String exit) throws IOException {
        Path tmp = dir.resolve(id + ".exit.tmp");
        Files.writeString(tmp, exit + "\n");
        try {
            Files.move(tmp, dir.resolve(id + ".exit"), StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(tmp, dir.resolve(id + ".exit"), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private void beat(Path alive) {
        while (true) {
            try {
                if (Files.exists(alive)) {
                    Files.setLastModifiedTime(alive,
                        FileTime.fromMillis(System.currentTimeMillis()));
                } else {
                    Files.writeString(alive, "");
                }
                Thread.sleep(HEARTBEAT_MILLIS);
            } catch (IOException e) {
                continue;
            } catch (InterruptedException e) {
                return;
            }
        }
    }

    private long rulesModified() throws IOException {
        if (rules == null || !Files.isDirectory(rules)) {
            return 0;
        }
        try (Stream<Path> files = Files.walk(rules)) {
            return files.mapToLong(CLIServer::modified).max().orElse(0);
        }
    }

    private static long modified(Path path) {
        try {
            return Files.getLastModifiedTime(path).toMillis();
        } catch (IOException e) {
            return -1;
        }
    }

    private static final class Tee extends OutputStream {
        private final OutputStream first;
        private final OutputStream second;

        Tee(OutputStream first, OutputStream second) {
            this.first = first;
            this.second = second;
        }

        @Override
        public void write(int b) throws IOException {
            first.write(b);
            second.write(b);
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            first.write(b, off, len);
            second.write(b, off, len);
        }

        @Override
        public void flush() throws IOException {
            first.flush();
            second.flush();
        }
    }
}
