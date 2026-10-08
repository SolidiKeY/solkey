/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.keyfile;

import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;

public record KeyProblem(Path source, List<String> options, List<Function> functions,
        List<Variable> programVariables, List<Taclet> rules, KeyFormula problem) {

    private static final Pattern OPTION =
        Pattern.compile("[A-Za-z_$][A-Za-z0-9_$]*:[A-Za-z_$][A-Za-z0-9_$]*");

    public KeyProblem {
        if (source.toString().contains("\"")) {
            throw new IllegalArgumentException("a source path cannot contain a quote: " + source);
        }
        for (String option : options) {
            if (!OPTION.matcher(option).matches()) {
                throw new IllegalArgumentException("not a taclet option: " + option);
            }
        }
        options = List.copyOf(options);
        functions = List.copyOf(functions);
        programVariables = List.copyOf(programVariables);
        rules = List.copyOf(rules);
    }

    public record Function(boolean unique, String sort, String name) {
        public Function {
            Names.identifier(sort);
            Names.identifier(name);
        }
    }

    public record Variable(String sort, String name) {
        public Variable {
            Names.identifier(sort);
            Names.identifier(name);
        }
    }

    public record Taclet(String name, String schemaSort, List<String> schemaVariables,
            boolean noFreeVariables, KeyFormula find, KeyFormula replacewith, String heuristic) {
        public Taclet {
            Names.identifier(name);
            Names.identifier(schemaSort);
            schemaVariables = Names.identifiers(schemaVariables);
            Names.identifier(heuristic);
            if (schemaVariables.isEmpty()) {
                throw new IllegalArgumentException("taclet " + name + " has no schema variable");
            }
        }
    }
}
