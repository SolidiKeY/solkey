/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.control;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.key_project.logic.Choice;
import org.key_project.logic.Namespace;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public final class TacletChoices {

    private TacletChoices() {
    }

    public static @Nullable String malformed(List<String> choices) {
        for (String choice : choices) {
            int colon = choice.indexOf(':');
            if (colon < 1 || colon != choice.lastIndexOf(':') || colon == choice.length() - 1) {
                return "--option expects <category>:<choice>, but got: " + choice;
            }
        }
        return null;
    }

    public static @Nullable String unknown(List<String> choices,
            Namespace<@NonNull Choice> declared) {
        if (choices.isEmpty()) {
            return null;
        }
        Map<String, Set<String>> byCategory = byCategory(declared);
        for (String choice : choices) {
            String category = categoryOf(choice);
            Set<String> known = byCategory.get(category);
            if (known == null) {
                return "no such taclet option category: " + category + "; known categories: "
                    + String.join(", ", byCategory.keySet());
            }
            if (!known.contains(choice)) {
                return "no such choice for " + category + ": " + valueOf(choice)
                    + "; known choices: "
                    + known.stream().map(TacletChoices::valueOf).collect(Collectors.joining(", "));
            }
        }
        return null;
    }

    public static Map<String, Set<String>> byCategory(Namespace<@NonNull Choice> declared) {
        Map<String, Set<String>> byCategory = new TreeMap<>();
        for (Choice c : declared.allElements()) {
            byCategory.computeIfAbsent(c.category(), k -> new TreeSet<>())
                    .add(c.name().toString());
        }
        return byCategory;
    }

    public static String categoryOf(String choice) {
        return choice.substring(0, choice.indexOf(':'));
    }

    public static String valueOf(String choice) {
        return choice.substring(choice.indexOf(':') + 1);
    }
}
