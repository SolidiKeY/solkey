/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.logic.equality;

import java.util.HashMap;
import java.util.Map;

import org.key_project.logic.Name;
import org.key_project.logic.Property;
import org.key_project.logic.SyntaxElement;
import org.key_project.logic.SyntaxElementCursor;
import org.key_project.solidity.logic.NameAbstractionTable;
import org.key_project.solidity.logic.op.ProgramVariable;
import org.key_project.solidity.program.ast.SolidityProgramElement;

public class RenamingProgramElementProperty implements Property<SolidityProgramElement> {
    /// The single instance of this property.
    public static final RenamingProgramElementProperty RENAMING_PROGRAM_ELEMENT_PROPERTY =
        new RenamingProgramElementProperty();

    /// This constructor is private as a single instance of this class should be shared. The
    /// instance
    /// can be accessed through
    /// [#RENAMING_PROGRAM_ELEMENT_PROPERTY].
    private RenamingProgramElementProperty() {}

    /// Checks if `rpe2` is a [SolidityProgramElement] syntactically equal to `rpe1`
    /// modulo
    /// renaming.
    ///
    /// When this method is supplied with a [NameAbstractionTable], it will use this table to
    /// compare the abstract names of the source elements. If no [NameAbstractionTable] is
    /// supplied, a new one will be created.
    ///
    /// @param rpe1 the first element of the equality check
    /// @param rpe2 the second element of the equality check
    /// @param v can be a single [NameAbstractionTable] for this equality check
    /// @return `true` iff `rpe2` is a source element syntactically equal to `rpe1`
    /// modulo renaming
    /// @param <V> is supposed to be [NameAbstractionTable] for this equality check
    @Override
    public <V> boolean equalsModThisProperty(SolidityProgramElement rpe1,
            SolidityProgramElement rpe2,
            V... v) {
        NameAbstractionTable nat;
        if (v.length > 0 && (v[0] instanceof NameAbstractionTable n)) {
            nat = n;
        } else {
            nat = new NameAbstractionTable();
        }

        SyntaxElementCursor c1 = rpe1.getCursor(), c2 = rpe2.getCursor();
        SyntaxElement next1, next2;
        boolean hasNext1, hasNext2; // Check at the end if both cursors have reached the end

        do {
            // First nodes can never be null as cursor is initialized with 'this'
            next1 = c1.getCurrentNode();
            next2 = c2.getCurrentNode();
            if (next1 instanceof ProgramVariable pv1) {
                if (!handleProgramVariable(pv1, next2, nat)) {
                    return false;
                }
            } else if (next1.getChildCount() > 0) {
                if (!handleSolidityNonTerminalProgramElement(next1,
                    next2)) {
                    return false;
                }
            } else {
                if (!handleStandard(next1, next2)) {
                    return false;
                }
            }
            // walk to the next nodes in the tree
        } while ((hasNext1 = c1.goToNext()) & (hasNext2 = c2.goToNext()));

        return hasNext1 == hasNext2;
    }

    // TODO: hashCodeModThisProperty currently does not take a NameAbstractionTable as an argument.
    // This is because the current implementation of hashCodeModThisProperty is not parameterized
    // with a vararg. Variables occurring in multiple formulas and SolidityBlocks are considered in
    // isolation as a newly created NameAbstractionTable that does not contain entries from previous
    // SolidityBlocks is used. This could possibly lead to more collisions but if this is a concern,
    // the
    // method can be changed to also take a generic vararg. That way, the NameAbstractionTable can
    // be passed to the method and hash codes can take previous usage of variables into account.
    @Override
    public int hashCodeModThisProperty(SolidityProgramElement SolidityProgramElement) {
        NameAbstractionMap absMap = new NameAbstractionMap();

        int hashCode = 1;
        SyntaxElementCursor c = SolidityProgramElement.getCursor();
        SyntaxElement next;

        do {
            // First node can never be null as cursor is initialized with 'this'
            next = c.getCurrentNode();
            // Handle special cases so that hashCodeModThisProperty follows equalsModThisProperty
            if (next instanceof ProgramVariable pv) {
                Name name = pv.name();
                hashCode = 31 * hashCode + absMap.getAbstractName(name);
            } else if (next.getChildCount() > 0) {
                hashCode = 31 * hashCode + next.getChildCount();
            } else {
                hashCode = 31 * hashCode + next.hashCode();
            }
            // walk to the next nodes in the tree
        } while (c.goToNext());

        return hashCode;
    }

    /*------------- Helper methods for special cases in equalsModThisProperty --------------*/
    /// Handles the standard case of comparing two [SyntaxElement]s modulo renaming.
    ///
    /// @param se1 the first [SyntaxElement] to be compared
    /// @param se2 the second [SyntaxElement] to be compared
    /// @return `true` iff the two source elements are equal under the standard `equals`
    /// method
    private boolean handleStandard(SyntaxElement se1, SyntaxElement se2) {
        return se1.equals(se2);
    }

    /// Handles the special case of comparing a [] to a [SyntaxElement].
    ///
    /// @param solNTE the Solidity program element with children to be compared
    /// @param se the [SyntaxElement] to be compared
    /// @return `true` iff `se` is of the same class and has the same number of children
    /// as `jnte`
    private boolean handleSolidityNonTerminalProgramElement(SyntaxElement solNTE,
            SyntaxElement se) {
        /*
         * In the case of non-terminal SolidityProgramElements, we must not traverse the children
         * recursively through the normal equals method. This is the case as we might have to
         * add some entries of children nodes to a NameAbstractionTable so that they can be
         * compared later on.
         */
        if (se == solNTE) {
            return true;
        }
        if (se.getClass() != solNTE.getClass()) {
            return false;
        }
        return solNTE.getChildCount() == se.getChildCount();
    }

    /// Handles the special case of comparing a [ProgramVariable] to a [SyntaxElement].
    ///
    /// @param se1 the [ProgramVariable]
    /// @param se2 the [SyntaxElement] to be compared
    /// @param nat the [NameAbstractionTable] that should be used to check whether `se1`
    /// and `se2` have the same abstract name
    /// @return `true` iff `se1` and `se2` have the same abstract name
    private boolean handleProgramVariable(ProgramVariable se1, SyntaxElement se2,
            NameAbstractionTable nat) {
        if (se1 == se2) {
            return true;
        }
        if (se1.getClass() != se2.getClass()) {
            return false;
        }

        return nat.sameAbstractName(se1.name(), ((ProgramVariable) se2).name());
    }


    /* ---------- End of helper methods for special cases in equalsModThisProperty ---------- */

    /// A helper class to map [Name]s to an abstract name.
    ///
    /// As names are abstracted from in this property, we need to give named elements abstract names
    /// for them to be used in the hash code. This approach is similar to
    /// [NameAbstractionTable], where we collect elements with names in the order they are
    /// declared. Each element is associated with the number of previously added elements, which is
    /// then used as the abstract name.
    private static class NameAbstractionMap {
        private int nextAbstractName = 0;

        /// The map that associates [Name]s with their abstract names.
        private final Map<Name, Integer> map = new HashMap<>();

        /// Adds a [Name] to the map.
        ///
        /// @param name the [Name] to be added
        public void add(Name name) {
            map.put(name, nextAbstractName++);
        }

        /// Returns the abstract name of a [Name] or `-1` if the element
        /// is not in the map.
        /// ee
        ///
        /// @param name the [Name] whose abstract name should be returned
        /// @return the abstract name of the [Name] or `-1` if the element
        /// is
        /// not in the map
        public int getAbstractName(Name name) {
            final Integer result = map.get(name);
            return result != null ? result : -1;
        }
    }
}
