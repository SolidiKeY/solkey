/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.proof.mgt;


// Object should be contract
public record RuleJustificationBySpec(Object spec) implements RuleJustification {
    public boolean isAxiomJustification() {
        // TODO
        return false;
    }
}
