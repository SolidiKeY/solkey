package org.key_project.solidity.parser.builder;

import org.jspecify.annotations.NonNull;
import org.key_project.solidity.parser.KeYSolidityDLParser;
import org.key_project.solidity.parser.KeYSolidityDLParserBaseVisitor;

import java.util.List;

public class ConfigurationBuilder extends KeYSolidityDLParserBaseVisitor<Object> {
    public List<Object> visitCfile(KeYSolidityDLParser.CfileContext ctx) {
        return null;
    }
}
