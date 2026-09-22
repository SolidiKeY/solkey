/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.parser;

import java.io.IOException;

/// A Solidity compiler addressed through solc's Standard JSON interface.
public interface SolcCompiler {

    String compile(String standardJsonInput) throws IOException;

    String version();
}
