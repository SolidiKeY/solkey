package org.key_project.solidity.logic.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.key_project.solidity.logic.ast.SolidityProgramElement;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;

public class SolJsonParser {
    public SolidityProgramElement parse(URI file) throws IOException {
        SolidityProgramElement result = null;
        //TIP Press <shortcut actionId="ShowIntentionActions"/> with your caret at the highlighted text
        // to see how IntelliJ IDEA suggests fixing it.
        ObjectMapper mapper = new ObjectMapper(); // create once, reuse
        JsonNode root = mapper.readTree(file.toURL());
        System.out.println(root);

        return result;
    }
}
