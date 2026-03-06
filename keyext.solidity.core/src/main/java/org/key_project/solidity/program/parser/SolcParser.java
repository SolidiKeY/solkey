package org.key_project.solidity.program.parser;

import org.key_project.solidity.common.Services;

public class SolcParser {

    static Services services;

    public SolcParser() {
        this.services = new Services();
    }

    public SolcParser(Services services){
        this.services = services;
    }
}
