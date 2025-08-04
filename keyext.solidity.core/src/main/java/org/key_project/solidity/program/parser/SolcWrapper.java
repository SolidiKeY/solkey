/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.parser;

import java.io.*;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;

public class SolcWrapper {

    private final Path solc;

    public SolcWrapper(Path solc) {
        this.solc = solc;
    }

    public BufferedOutputStream readSolBuff(Path filename) throws IOException {
        ProcessBuilder pb =
            new ProcessBuilder(solc.toAbsolutePath().toString(), "----ast-compact-json",
                filename.toAbsolutePath().toString());
        Process p = pb.start();
        return new BufferedOutputStream(p.getOutputStream());
    }

    public BufferedReader readSolBuff(byte[] contract) throws IOException {
        ProcessBuilder pb = new ProcessBuilder("solc", "--ast-compact-json", "-");

        Process proc = pb.start();
        OutputStream out = proc.getOutputStream();
        out.write(contract);
        out.close();
        BufferedReader procInput = proc.inputReader();
        int exitCode;
        try {
            exitCode = proc.waitFor();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        if (exitCode == 1){
            InputStream errorStream = proc.getErrorStream();
            String errorStr = new String(errorStream.readAllBytes(), UTF_8);
            throw new RuntimeException("Not possible to compile solidity code:\n" + errorStr);
        }

        return new BufferedReader(procInput);
    }

    public BufferedReader readSolBuff(URI file) throws IOException {
        final InputStream solidityInputStream = new BufferedInputStream(file.toURL().openStream());
        byte[] contractContent;
        try (solidityInputStream) {
            contractContent = solidityInputStream.readAllBytes();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return readSolBuff(contractContent);
    }

    public BufferedReader readSolString(String contract) throws IOException {
        return readSolBuff(contract.getBytes(UTF_8));
    }

    public String readSol(URI file) throws IOException {
        BufferedReader reader = readSolBuff(file);
        return extract4lines(reader);
    }

    private static String extract4lines(BufferedReader reader) {
        return reader.lines().skip(4).collect(Collectors.joining());
    }

    public String readSol(String s) throws IOException {
        BufferedReader reader = readSolString(s);
        return extract4lines(reader);
    }

}
