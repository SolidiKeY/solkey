package org.key_project.solidity.program.ast;

import org.key_project.solidity.program.ast.declarations.Declaration;

import java.util.HashMap;

public interface Resolver {
    void resolve(HashMap<Integer, Declaration> id2Name);
}
