/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.keyfile;

import java.math.BigInteger;
import java.util.List;

public sealed interface KeyTerm {

    record Constant(String name) implements KeyTerm {
        public Constant {
            Names.identifier(name);
        }
    }

    record IntLiteral(BigInteger value) implements KeyTerm {
    }

    record Apply(String function, List<String> sorts, List<KeyTerm> arguments)
            implements KeyTerm {
        public Apply {
            Names.identifier(function);
            sorts = Names.identifiers(sorts);
            arguments = List.copyOf(arguments);
            if (arguments.isEmpty()) {
                throw new IllegalArgumentException(function + " applied to no arguments");
            }
        }
    }

    record Arithmetic(Operator operator, KeyTerm left, KeyTerm right) implements KeyTerm {
        public enum Operator {
            PLUS("+"), MINUS("-"), TIMES("*");

            final String symbol;

            Operator(String symbol) {
                this.symbol = symbol;
            }

            public static Operator of(String symbol) {
                for (Operator operator : values()) {
                    if (operator.symbol.equals(symbol)) {
                        return operator;
                    }
                }
                throw new IllegalArgumentException("unknown arithmetic operator " + symbol);
            }
        }
    }

    record IfThenElse(KeyFormula condition, KeyTerm then, KeyTerm otherwise) implements KeyTerm {
    }
}
