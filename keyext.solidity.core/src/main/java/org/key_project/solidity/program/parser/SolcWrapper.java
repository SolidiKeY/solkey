/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.parser;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Path;

public class SolcWrapper {

    private final Path solc;

    public SolcWrapper(Path solc) {
        this.solc = solc;
    }

    public BufferedOutputStream readSol(Path filename) throws IOException {
        ProcessBuilder pb =
            new ProcessBuilder(solc.toAbsolutePath().toString(), "----ast-compact-json",
                filename.toAbsolutePath().toString());
        Process p = pb.start();
        return new BufferedOutputStream(p.getOutputStream());
    }

    public BufferedReader readSol(URI file) throws IOException {
        ProcessBuilder pb = new ProcessBuilder(solc.toFile().getAbsoluteFile().toString(),
            "--ast-compact-json", "-");

        final InputStream solidityInputStream = new BufferedInputStream(file.toURL().openStream());
        byte[] contractContent;
        try (solidityInputStream) {
            contractContent = solidityInputStream.readAllBytes();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Process proc = pb.start();
        proc.getOutputStream().write(contractContent);
        proc.getOutputStream().flush();

        return new BufferedReader(proc.inputReader());
    }

}
