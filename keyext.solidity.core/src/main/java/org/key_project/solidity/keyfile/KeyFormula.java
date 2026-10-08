/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.keyfile;

import java.util.List;

public sealed interface KeyFormula {

    record Truth(boolean value) implements KeyFormula {
    }

    record Predicate(String name, List<KeyTerm> arguments) implements KeyFormula {
        public Predicate {
            Names.identifier(name);
            arguments = List.copyOf(arguments);
            if (arguments.isEmpty()) {
                throw new IllegalArgumentException(name + " applied to no arguments");
            }
        }
    }

    record Equals(KeyTerm left, KeyTerm right) implements KeyFormula {
    }

    record Compare(Relation relation, KeyTerm left, KeyTerm right) implements KeyFormula {
        public enum Relation {
            LT("<"), LEQ("<="), GT(">"), GEQ(">=");

            final String symbol;

            Relation(String symbol) {
                this.symbol = symbol;
            }

            public static Relation of(String symbol) {
                for (Relation relation : values()) {
                    if (relation.symbol.equals(symbol)) {
                        return relation;
                    }
                }
                throw new IllegalArgumentException("unknown relation " + symbol);
            }
        }
    }

    record Not(KeyFormula operand) implements KeyFormula {
    }

    record And(List<KeyFormula> conjuncts) implements KeyFormula {
        public And {
            conjuncts = List.copyOf(conjuncts);
            if (conjuncts.size() < 2) {
                throw new IllegalArgumentException("a conjunction needs two conjuncts");
            }
        }
    }

    record Binary(Connective connective, KeyFormula left, KeyFormula right)
            implements KeyFormula {
        public enum Connective {
            OR("|"), IMPLIES("->"), EQUIVALENT("<->");

            final String symbol;

            Connective(String symbol) {
                this.symbol = symbol;
            }
        }
    }

    record Quantified(boolean universal, String sort, String variable, KeyFormula body)
            implements KeyFormula {
        public Quantified {
            Names.identifier(sort);
            Names.identifier(variable);
        }
    }

    record Update(List<Assignment> assignments, KeyFormula target) implements KeyFormula {
        public Update {
            assignments = List.copyOf(assignments);
            if (assignments.isEmpty()) {
                throw new IllegalArgumentException("an update needs an assignment");
            }
        }
    }

    record Assignment(String variable, KeyTerm value) {
        public Assignment {
            Names.identifier(variable);
        }
    }

    record Modality(boolean box, Call call, KeyFormula post) implements KeyFormula {
    }

    record Call(List<String> results, String function, List<String> arguments,
            String contract) {
        public Call {
            results = Names.identifiers(results);
            Names.identifier(function);
            arguments = Names.identifiers(arguments);
            Names.identifier(contract);
        }
    }

    record Labeled(String label, KeyFormula formula) implements KeyFormula {
        public Labeled {
            label = label.strip().replaceAll("\\s+", " ");
        }
    }
}
