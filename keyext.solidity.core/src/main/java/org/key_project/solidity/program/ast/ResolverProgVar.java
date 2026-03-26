package org.key_project.solidity.program.ast;

import org.key_project.solidity.logic.op.ProgramVariable;

import java.util.HashMap;

public interface ResolverProgVar {
    void resolve(HashMap<Integer, ProgramVariable> id2ProgVar);
}
