/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.parser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static java.nio.charset.StandardCharsets.UTF_8;

public final class SoliditySources {

    private static final Map<Path, String> REGISTERED = new ConcurrentHashMap<>();

    private SoliditySources() {
    }

    public static void register(Path path, String source) {
        REGISTERED.put(keyOf(path), source);
    }

    public static String read(Path path) throws IOException {
        String source = REGISTERED.get(keyOf(path));
        return source != null ? source : Files.readString(path, UTF_8);
    }

    public static boolean exists(Path path) {
        return REGISTERED.containsKey(keyOf(path)) || Files.exists(path);
    }

    public static boolean isFile(Path path) {
        return REGISTERED.containsKey(keyOf(path)) || Files.isRegularFile(path);
    }

    private static Path keyOf(Path path) {
        return path.toAbsolutePath().normalize();
    }
}
