/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.logic.op;

import java.util.OptionalInt;

import org.key_project.logic.Name;
import org.key_project.logic.Term;
import org.key_project.logic.sort.Sort;

/// The `FixedField` constant of a fixed-size array member, carrying the declared length.
public class FixedArrayField extends SFunction {
    private final int length;

    public FixedArrayField(Name name, Sort sort, int length) {
        super(name, sort, true, true);
        this.length = length;
    }

    public int length() {
        return length;
    }

    /// The declared length of the field `term` denotes, or of the last field of the path list
    /// `term` denotes; empty when that field is not a fixed-size array member.
    public static OptionalInt lengthOf(Term term) {
        Term current = term;
        while (true) {
            if (current.op() instanceof FixedArrayField field) {
                return OptionalInt.of(field.length());
            }
            if (current.arity() != 2) {
                return OptionalInt.empty();
            }
            String name = current.op().name().toString();
            if (name.equals("consr")) {
                current = current.sub(1);
            } else if (name.equals("cons")) {
                current = current.sub(1).op().name().toString().equals("nil") ? current.sub(0)
                        : current.sub(1);
            } else {
                return OptionalInt.empty();
            }
        }
    }
}
