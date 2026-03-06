/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.parser;


import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;

public class SolcWrapper {

    public static String getJsonSolidity(Path contractPath) throws IOException {
        String fileName = contractPath.toAbsolutePath().toString();
        ProcessBuilder pb = new ProcessBuilder(getSolcCommand(), "--ast-compact-json", fileName);
        Process proc = pb.start();
        OutputStream out = proc.getOutputStream();
        out.close();
        return finishesSolcCommand(proc);
    }

    private static void exportSolc(Path targetPath) throws IOException {
        InputStream is = SolcWrapper.class.getResourceAsStream("/solc");
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

    static String finishesSolcCommand(Process proc) throws IOException {
        BufferedReader procInput = proc.inputReader();
        int exitCode;
        try {
            exitCode = proc.waitFor();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        if (exitCode == 1) {
            InputStream errorStream = proc.getErrorStream();
            String errorStr = new String(errorStream.readAllBytes(), UTF_8);
            throw new RuntimeException("Not possible to compile solidity code:\n" + errorStr);
        }
        return extract4lines(procInput);
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
