package org.key_project.solidity.program.ast.visitor;

import org.key_project.logic.op.sv.SchemaVariable;
import org.key_project.solidity.program.ast.SolidityProgramElement;
import org.key_project.solidity.rule.matching.inst.SVInstantiations;
import org.key_project.util.collection.ImmutableList;
import org.key_project.util.collection.ImmutableSLList;

public class ProgramSVCollector extends SolidityASTWalker {
    private ImmutableList<SchemaVariable> result = ImmutableSLList.nil();

    /// the instantiations needed for unwind loop constructs
    private SVInstantiations instantiations = SVInstantiations.EMPTY_SVINSTANTIATIONS;

    /// create the ProgramSVCollector
    ///
    /// @param root the ProgramElement where to begin
    /// @param vars the IList<SchemaVariable> where to add the new-found ones
    /// @param svInst the SVInstantiations previously found in order to determine the needed labels
    /// for the UnwindLoop construct.
    public ProgramSVCollector(SolidityProgramElement root, ImmutableList<SchemaVariable> vars,
                              SVInstantiations svInst) {
        super(root);
        result = vars;
        instantiations = svInst;
    }

    /// starts the walker
    public void start() {
        walk(root());
    }

    public ImmutableList<SchemaVariable> getSchemaVariables() {
        return result;
    }

    /// the action that is performed just before leaving the node the last time. Not only schema
    /// variables must be taken into consideration, but also program meta constructs with implicit
    /// schema variables containment
    @Override
    protected void doAction(SolidityProgramElement node) {
        if (node instanceof SchemaVariable) {
            result = result.prepend((SchemaVariable) node);
        }
    }
}
