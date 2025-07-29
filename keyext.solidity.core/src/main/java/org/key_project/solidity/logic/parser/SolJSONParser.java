/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.logic.parser;

import java.io.IOException;
import java.math.BigInteger;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import com.fasterxml.jackson.core.io.BigIntegerParser;
import org.key_project.logic.Name;
import org.key_project.solidity.logic.ast.SolidityProgramElement;
import org.key_project.solidity.logic.ast.declarations.ContractDeclaration;
import org.key_project.solidity.logic.ast.declarations.Declaration;
import org.key_project.solidity.logic.ast.declarations.FieldDeclaration;
import org.key_project.solidity.logic.ast.expressions.AddOperation;
import org.key_project.solidity.logic.ast.expressions.Expression;
import org.key_project.solidity.logic.ast.expressions.Literal;
import org.key_project.solidity.logic.ast.expressions.Uint256Literal;
import org.key_project.solidity.logic.ast.references.TypeReference;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class SolJSONParser {
    public List<SolidityProgramElement> parse(URI file) throws IOException {
        List<SolidityProgramElement> result = null;
        // TIP Press <shortcut actionId="ShowIntentionActions"/> with your caret at the highlighted
        // text
        // to see how IntelliJ IDEA suggests fixing it.
        ObjectMapper mapper = new ObjectMapper(); // create once, reuse
        JsonNode root = mapper.readTree(file.toURL());
        System.out.println(root);

        result = json2SolidityAST(root);

        return result;
    }

    private List<SolidityProgramElement> json2SolidityAST(JsonNode root) {
        Iterator<String> iter = root.fieldNames();
        while (iter.hasNext()) {
            System.out.println(iter.next());
        }

        if ("SourceUnit".equals(root.findValue("nodeType").asText())) {
            return parseSourceUnit(root.findValues("nodes"));
        }

        return new ArrayList<>();
    }

    private List<SolidityProgramElement> parseSourceUnit(List<JsonNode> nodes) {
        List<SolidityProgramElement> elements = new ArrayList<>();
        for (var node : nodes) {
            if (node.findValue("contractKind").asText().equals("contract")) {
                elements.add(parseContract(node));
            }
        }
        return elements;
    }

    private HashMap<String, Declaration> id2Name = new HashMap<>();

    private ContractDeclaration parseContract(JsonNode contractNode) {
        String contractName = contractNode.findValue("canonicalName").asText(); // there is also a
                                                                                // field "name"
        // now retrieve declared fields, functions, structs etc.
        List<FieldDeclaration> fields = new ArrayList<>();

        for (Iterator<JsonNode> it = contractNode.findValue("nodes").elements(); it.hasNext(); ) {
            var node = it.next();
            if (node.findValue("nodeType").asText().equals("VariableDeclaration")) {
                fields.add(parseField(node));
            }
        }

        final ContractDeclaration cdecl =  new ContractDeclaration(new Name(contractName), fields);
        String contractId = contractNode.findValue("id").asText();
        id2Name.put(contractId, cdecl);
        return cdecl;
    }

    private FieldDeclaration parseField(JsonNode fieldNode) {
        String fieldName = fieldNode.findValue("name").asText();
        String fieldType = fieldNode.findValue("typeName").findValue("name").asText();
        // boolean isPrimitive =
        // fieldNode.findValue("typeName").findValue("nodeType").asText().equals("ElementaryTypeName");
        String visibility = fieldNode.findValue("visibility").asText();
        // todo: initializer
        JsonNode initializer = fieldNode.findValue("value");
        Expression initializerExp = null;
        if (initializer != null) {
             initializerExp = parseExpression(initializer);
        }
        FieldDeclaration field = new FieldDeclaration(new Name(fieldName), new TypeReference(new Name(fieldType)),
                initializerExp);
        String id = fieldNode.findValue("id").asText();
        id2Name.put(id, field);
        return field;
    }

    private Expression parseExpression(JsonNode initializer) {
        if (initializer.findValue("nodeType").asText().equals("Literal")) {
            return parseLiteral(initializer);
        } else if (initializer.findValue("nodeType").asText().equals("BinaryOperation")) {
            return parseBinaryOperation(initializer);
        }
        throw new RuntimeException("Not yet supported expression");
    }

    private Expression parseBinaryOperation(JsonNode initializer) {
        // TO BE Implemented
        JsonNode leftExpression = null;
        JsonNode rightExpression = null;

        final String operator = initializer.findValue("operator").asText();
        switch (operator) {
            case "+": return new AddOperation(parseExpression(leftExpression),
                    parseExpression(rightExpression));
            default: throw new RuntimeException("Not yet supported binary operation: " + operator);
        }
    }

    private Literal parseLiteral(JsonNode literal) {
        if (literal.findValue("kind").asText().equals("number")) {
            String initializerExp = literal.findValue("value").asText();
            return new Uint256Literal(BigIntegerParser.parseWithFastParser(initializerExp));
        } else if (literal.findValue("kind").asText().equals("bool")) {
            return new Uint256Literal(BigInteger.ZERO); // FIX!!!!
        }
        throw new RuntimeException("Not yet supported literal");
    }
}
