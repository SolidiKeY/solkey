/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.parser;

import java.io.IOException;
import java.nio.file.Path;

import org.key_project.solidity.common.Services;
import org.key_project.solidity.program.ast.declarations.ContractDeclaration;

public class SolcParserNoServices {
    public static ContractDeclaration getDeclStrJson(Path contract) throws IOException {
        SolcParser solcParser = new SolcParser(new Services());
        return solcParser.getDeclStrJsonParser(contract);
    }

    public static ContractDeclaration getDeclStr(String contract) throws IOException {
        SolcParser solcParser = new SolcParser(new Services());
        return solcParser.getDeclStrJsonParser(contract);
    }
}
