/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.testutil;

import java.io.IOException;
import java.nio.file.Path;

import org.key_project.solidity.program.parser.SolcWrapper;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

public final class SolcCacheExtension implements BeforeAllCallback {

    public static final String DIRECTORY_PROPERTY = "org.key_project.solidity.solcCache";

    private static boolean installed;

    @Override
    public void beforeAll(ExtensionContext context) throws IOException {
        install();
    }

    private static synchronized void install() throws IOException {
        String directory = System.getProperty(DIRECTORY_PROPERTY);
        if (installed || directory == null) {
            return;
        }
        SolcWrapper.useCompiler(new CachingSolcCompiler(Path.of(directory)));
        installed = true;
    }
}
