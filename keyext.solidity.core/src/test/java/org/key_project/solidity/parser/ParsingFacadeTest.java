package org.key_project.solidity.parser;

import org.junit.jupiter.api.Test;
import org.key_project.prover.proof.SessionCaches;
import org.key_project.solidity.common.Services;

import static org.junit.jupiter.api.Assertions.*;

class ParsingFacadeTest {

    private Services services = new Services();

    @Test
    void parseExpression() {
        KeYIO io = new KeYIO(services);
        io.parseExpression("A & B");
    }
}