/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.speclang.natspec;

/// A malformed `@custom:key` clause. An [IllegalArgumentException] so the CLI and the GUI report
/// it through the same path as an unprovable function.
public class SpecException extends IllegalArgumentException {

    public SpecException(String message) {
        super(message);
    }
}
