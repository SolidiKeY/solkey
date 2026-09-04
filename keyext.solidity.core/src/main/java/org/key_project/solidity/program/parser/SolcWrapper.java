/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.parser;


import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;

public class SolcWrapper {

    /// Identifies a contract file revision: the same path with the same size and modification
    /// time yields the same AST, so the JSON can be reused instead of forking solc again.
    private record ContractRevision(Path path, long size, long lastModified) {
    }

    private static final Map<ContractRevision, String> JSON_CACHE = new ConcurrentHashMap<>();

    /// The solc AST JSON of `contractPath`, memoized per file revision.
    ///
    /// A single obligation forks solc twice — once to enumerate the contract's functions, once
    /// while loading — and a suite run repeats that for every function of the same file.
    public static String getJsonSolidity(Path contractPath) throws IOException {
        ContractRevision revision = revisionOf(contractPath);
        if (revision == null) {
            return runSolcOn(contractPath);
        }
        String cached = JSON_CACHE.get(revision);
        if (cached == null) {
            cached = runSolcOn(contractPath);
            JSON_CACHE.put(revision, cached);
        }
        return cached;
    }

    private static ContractRevision revisionOf(Path contractPath) {
        try {
            Path real = contractPath.toRealPath();
            return new ContractRevision(real, Files.size(real),
                Files.getLastModifiedTime(real).toMillis());
        } catch (IOException e) {
            return null;
        }
    }

    private static String runSolcOn(Path contractPath) throws IOException {
        String fileName = contractPath.toAbsolutePath().toString();
        ProcessBuilder pb = new ProcessBuilder(getSolcCommand(), "--ast-compact-json", fileName);
        Process proc = pb.start();
        OutputStream out = proc.getOutputStream();
        out.close();
        return finishesSolcCommand(proc);
    }

    private static void exportSolc(Path targetPath) throws IOException {
        InputStream is = SolcWrapper.class.getResourceAsStream("/solc");
        if (is == null) {
            throw new IOException("no solc on PATH and none bundled with this build");
        }
        Files.copy(is, targetPath, StandardCopyOption.REPLACE_EXISTING);
        targetPath.toFile().setExecutable(true);
    }

    private static boolean canRunCommand(String cmd) {
        try {
            // We run a simple version check to see if it exists
            Process process = new ProcessBuilder(cmd, "--version").start();
            process.waitFor();
            return true;
        } catch (IOException | InterruptedException e) {
            // IOException happens if the command isn't found in PATH
            return false;
        }
    }

    static String getSolcCommand() throws IOException {
        if (canRunCommand("solc"))
            return "solc";
        Path tempDir = Paths.get(System.getProperty("java.io.tmpdir"));
        Path targetPath = tempDir.resolve("solc");

        if (!Files.exists(targetPath))
            exportSolc(targetPath);
        return targetPath.toAbsolutePath().toString();
    }

    /// solc `--combined-json bin,bin-runtime` for `contractPath`, as raw JSON. Used by the
    /// runtime cross-check tests to execute the examples on a real EVM.
    public static String getCombinedBinJson(Path contractPath) throws IOException {
        String fileName = contractPath.toAbsolutePath().toString();
        ProcessBuilder pb =
            new ProcessBuilder(getSolcCommand(), "--combined-json", "bin,bin-runtime", fileName);
        Process proc = pb.start();
        proc.getOutputStream().close();
        return finishesSolcCommand(proc, false);
    }

    /// Consumes solc's output and waits for it to exit.
    ///
    /// Both pipes have to be drained while the process is still running. The AST JSON of a
    /// contract with more than a handful of function bodies exceeds the operating system's pipe
    /// buffer, at which point solc blocks writing; waiting for exit before reading then
    /// deadlocks both processes permanently.
    static String finishesSolcCommand(Process proc) throws IOException {
        return finishesSolcCommand(proc, true);
    }

    /// `skipAstHeader` drops the four banner lines solc prints before `--ast-compact-json`
    /// output; `--combined-json` prints bare JSON, which has to be read in full.
    static String finishesSolcCommand(Process proc, boolean skipAstHeader) throws IOException {
        final StringBuilder errors = new StringBuilder();
        Thread errorDrain = new Thread(() -> {
            try (BufferedReader reader =
                new BufferedReader(new InputStreamReader(proc.getErrorStream(), UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    errors.append(line).append('\n');
                }
            } catch (IOException ignored) {
                // the diagnostics are best-effort; the exit code decides the outcome
            }
        }, "solc-stderr");
        errorDrain.setDaemon(true);
        errorDrain.start();

        final String output;
        try (BufferedReader procInput = proc.inputReader()) {
            output = skipAstHeader ? extract4lines(procInput)
                    : procInput.lines().collect(Collectors.joining());
        }

        final int exitCode;
        try {
            exitCode = proc.waitFor();
            errorDrain.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
        if (exitCode != 0) {
            throw new RuntimeException("Not possible to compile solidity code:\n" + errors);
        }
        return output;
    }

    public static String readSolBuff(byte[] contract) throws IOException {
        ProcessBuilder pb = new ProcessBuilder(getSolcCommand(), "--ast-compact-json", "-");
        Process proc = pb.start();
        OutputStream out = proc.getOutputStream();
        out.write(contract);
        out.close();
        return finishesSolcCommand(proc);
    }

    public static String readSolString(String contract) throws IOException {
        return readSolBuff(contract.getBytes(UTF_8));
    }

    private static String extract4lines(BufferedReader reader) {
        return reader.lines().skip(4).collect(Collectors.joining());
    }

    public static String readSol(String s) throws IOException {
        return readSolString(s);
    }
}
