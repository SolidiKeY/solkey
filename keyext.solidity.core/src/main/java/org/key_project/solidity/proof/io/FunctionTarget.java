/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.proof.io;

import org.jspecify.annotations.Nullable;

/// Selects the Solidity function to verify when loading a `.sol` file directly. The contract
/// may be omitted when the function name is unambiguous across the loaded contracts.
public record FunctionTarget(@Nullable String contract, String function) {
}
