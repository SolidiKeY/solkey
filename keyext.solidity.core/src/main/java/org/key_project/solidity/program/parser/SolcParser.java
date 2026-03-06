/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.parser;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.key_project.logic.SyntaxElement;
import org.key_project.solidity.common.Services;
import org.key_project.solidity.program.ast.declarations.ContractDeclaration;

import static org.key_project.solidity.program.parser.SolcWrapper.getJsonSolidity;

public class SolcParser {

    static Services services;
    static SolJSONParser jsonParser;

    public SolcParser() {
        services = new Services();
        jsonParser = new SolJSONParser(services);
    }

    public SolcParser(Services services) {
        this.services = services;
        jsonParser = new SolJSONParser(services);
    }

    public static List<SyntaxElement> getDeclsJsonParser(String contract) throws IOException {
        String contractJson = SolcWrapper.readSol(contract);
        return jsonParser.parse(contractJson);
    }

    public static ContractDeclaration getDeclStrJsonParser(String contract) throws IOException {
        SolcWrapper solcWrapper = new SolcWrapper();
        String contractJson = solcWrapper.readSol(contract);
        SyntaxElement programElement = getSolidityFromStrJsonParser(contractJson);
        return (ContractDeclaration) programElement;
    }

    public static ContractDeclaration getDeclStrJsonParser(Path contract) throws IOException {
        SyntaxElement programElement = getSolidityFromStrJsonParser(contract);
        return (ContractDeclaration) programElement;
    }

    public static SyntaxElement getSolidityFromStrJsonParser(Path contractPath) throws IOException {
        String jsonSolidity = getJsonSolidity(contractPath);
        List<SyntaxElement> unit = jsonParser.parse(jsonSolidity);
        return unit.getFirst();
    }

    private static SyntaxElement getSolidityFromStrJsonParser(String contract)
            throws IOException {
        List<SyntaxElement> unit = jsonParser.parse(contract);
        return unit.getFirst();
    }

    public static ContractDeclaration getDeclStr(String contract) throws IOException {
        services = new Services();
        jsonParser = new SolJSONParser(services);
        return getDeclStrJsonParser(contract);
    }

}
