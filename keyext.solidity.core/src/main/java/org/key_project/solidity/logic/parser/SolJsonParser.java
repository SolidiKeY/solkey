/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.logic.parser;

import java.io.IOException;
import java.net.URI;

import org.key_project.solidity.logic.ast.SolidityProgramElement;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class SolJsonParser {
    public SolidityProgramElement parse(URI file) throws IOException {
        SolidityProgramElement result = null;
        // TIP Press <shortcut actionId="ShowIntentionActions"/> with your caret at the highlighted
        // text
        // to see how IntelliJ IDEA suggests fixing it.
        ObjectMapper mapper = new ObjectMapper(); // create once, reuse
        JsonNode root = mapper.readTree(file.toURL());
        System.out.println(root);

        return result;
    }
}
