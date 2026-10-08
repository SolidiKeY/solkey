/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.keyfile;

import java.math.BigInteger;
import java.util.List;

import org.key_project.solidity.keyfile.KeyFormula.Assignment;
import org.key_project.solidity.keyfile.KeyFormula.Binary.Connective;
import org.key_project.solidity.keyfile.KeyFormula.Call;
import org.key_project.solidity.keyfile.KeyFormula.Compare.Relation;
import org.key_project.solidity.keyfile.KeyTerm.Arithmetic.Operator;

public final class Key {

    public static final KeyFormula TRUE = new KeyFormula.Truth(true);
    public static final KeyFormula FALSE = new KeyFormula.Truth(false);

    private Key() {}

    public static KeyTerm constant(String name) {
        return new KeyTerm.Constant(name);
    }

    public static KeyTerm num(long value) {
        return new KeyTerm.IntLiteral(BigInteger.valueOf(value));
    }

    public static KeyTerm num(String value) {
        try {
            return new KeyTerm.IntLiteral(new BigInteger(value));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("not an integer literal: " + value, e);
        }
    }

    public static KeyTerm apply(String function, KeyTerm... arguments) {
        return new KeyTerm.Apply(function, List.of(), List.of(arguments));
    }

    public static KeyTerm apply(String function, List<KeyTerm> arguments) {
        return new KeyTerm.Apply(function, List.of(), arguments);
    }

    public static KeyTerm typed(String function, String sort, KeyTerm... arguments) {
        return new KeyTerm.Apply(function, List.of(sort), List.of(arguments));
    }

    public static KeyTerm arithmetic(Operator operator, KeyTerm left, KeyTerm right) {
        return new KeyTerm.Arithmetic(operator, left, right);
    }

    public static KeyTerm plus(KeyTerm left, KeyTerm right) {
        return arithmetic(Operator.PLUS, left, right);
    }

    public static KeyTerm ifThenElse(KeyFormula condition, KeyTerm then, KeyTerm otherwise) {
        return new KeyTerm.IfThenElse(condition, then, otherwise);
    }

    public static KeyFormula predicate(String name, KeyTerm... arguments) {
        return new KeyFormula.Predicate(name, List.of(arguments));
    }

    public static KeyFormula eq(KeyTerm left, KeyTerm right) {
        return new KeyFormula.Equals(left, right);
    }

    public static KeyFormula compare(Relation relation, KeyTerm left, KeyTerm right) {
        return new KeyFormula.Compare(relation, left, right);
    }

    public static KeyFormula not(KeyFormula operand) {
        return new KeyFormula.Not(operand);
    }

    public static KeyFormula and(KeyFormula... conjuncts) {
        return and(List.of(conjuncts));
    }

    public static KeyFormula and(List<KeyFormula> conjuncts) {
        return switch (conjuncts.size()) {
            case 0 -> TRUE;
            case 1 -> conjuncts.get(0);
            default -> new KeyFormula.And(conjuncts);
        };
    }

    public static KeyFormula or(KeyFormula left, KeyFormula right) {
        return new KeyFormula.Binary(Connective.OR, left, right);
    }

    public static KeyFormula implies(KeyFormula left, KeyFormula right) {
        return new KeyFormula.Binary(Connective.IMPLIES, left, right);
    }

    public static KeyFormula equivalent(KeyFormula left, KeyFormula right) {
        return new KeyFormula.Binary(Connective.EQUIVALENT, left, right);
    }

    public static KeyFormula quantified(boolean universal, String sort, String variable,
            KeyFormula body) {
        return new KeyFormula.Quantified(universal, sort, variable, body);
    }

    public static Assignment assign(String variable, KeyTerm value) {
        return new Assignment(variable, value);
    }

    public static KeyFormula update(List<Assignment> assignments, KeyFormula target) {
        return assignments.isEmpty() ? target : new KeyFormula.Update(assignments, target);
    }

    public static Call call(List<String> results, String function, List<String> arguments,
            String contract) {
        return new Call(results, function, arguments, contract);
    }

    public static KeyFormula modality(boolean box, Call call, KeyFormula post) {
        return new KeyFormula.Modality(box, call, post);
    }

    public static KeyFormula labeled(String label, KeyFormula formula) {
        return new KeyFormula.Labeled(label, formula);
    }
}
