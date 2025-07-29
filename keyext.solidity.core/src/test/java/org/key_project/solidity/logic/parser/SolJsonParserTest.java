package org.key_project.solidity.logic.parser;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.key_project.logic.SyntaxElement;
import org.key_project.solidity.logic.ast.SolidityProgramElement;
import org.key_project.util.helper.FindResources;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;

class SolJsonParserTest {

    @Test
    void parse() throws IOException {
        SolJsonParser jsonParser = new SolJsonParser();
        String solFileName = "SimpleContract1.json";
        URI fileURI = getFile(solFileName);
        Assertions.assertNotNull(fileURI);
        SolidityProgramElement se = jsonParser.parse(fileURI);
    }

    private static URI getFile(String solFileName) {
        try {
            //return FindResources.getResource(solFileName, SolJsonParserTest.class).toUri();
            return SolJsonParser.class.getResource(solFileName).toURI();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}