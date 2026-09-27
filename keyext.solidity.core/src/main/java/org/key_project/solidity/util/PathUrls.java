/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.util;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;

public final class PathUrls {

    private PathUrls() {
    }

    public static URL toURL(Path path) throws MalformedURLException {
        try {
            return path.toUri().toURL();
        } catch (MalformedURLException | IllegalArgumentException e) {
            return new File(path.toAbsolutePath().toString()).toURI().toURL();
        }
    }
}
