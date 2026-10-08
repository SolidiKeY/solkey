/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.keyfile;

import java.util.List;
import java.util.regex.Pattern;

final class Names {
    private static final Pattern IDENTIFIER = Pattern.compile("[A-Za-z_$][A-Za-z0-9_$]*");

    private Names() {}

    static String identifier(String name) {
        if (name == null || !IDENTIFIER.matcher(name).matches()) {
            throw new IllegalArgumentException("not a KeY identifier: " + name);
        }
        return name;
    }

    static List<String> identifiers(List<String> names) {
        names.forEach(Names::identifier);
        return List.copyOf(names);
    }
}
