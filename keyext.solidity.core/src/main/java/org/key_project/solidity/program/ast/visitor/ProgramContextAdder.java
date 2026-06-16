/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.visitor;


import java.rmi.UnexpectedException;

import org.key_project.logic.IntIterator;
import org.key_project.solidity.program.PosInProgram;
import org.key_project.solidity.program.ast.SolidityProgramElement;
import org.key_project.solidity.program.ast.statement.Block;
import org.key_project.solidity.program.ast.statement.Statement;
import org.key_project.solidity.program.ext.ContextStatementBlock;
import org.key_project.solidity.rule.matching.inst.ContextBlockExpressionInstantiation;
import org.key_project.util.collection.ImmutableArray;

import org.jspecify.annotations.Nullable;

/// Wraps the prefix/suffix context recorded in a [ContextBlockExpressionInstantiation] around the
/// statements of the replacement [ContextStatementBlock] (the `c# ... #c` block of a taclet's
/// `\replacewith`). This is the apply-time counterpart to [ContextStatementBlock#match]: matching
/// records which surrounding statements were hidden by the context, and this class splices them
/// back around the replacement.
///
/// Ported from KeY-Java's `ProgramContextAdder`, restricted to the statement blocks that occur in
/// the Solidity calculus.
public class ProgramContextAdder {
    /// singleton instance of the program context adder
    public final static ProgramContextAdder INSTANCE = new ProgramContextAdder();

    /// an empty private constructor to ensure the singleton property
    private ProgramContextAdder() {
    }

    /// wraps the context around the statements found in the putIn block
    public SolidityProgramElement start(SolidityProgramElement context,
            ContextStatementBlock putIn, ContextBlockExpressionInstantiation ct) {
        return wrap(context, putIn, ct.prefix().iterator(), ct.suffix());
    }

    protected SolidityProgramElement wrap(SolidityProgramElement context,
            ContextStatementBlock putIn, IntIterator prefixPos, PosInProgram suffix) {

        final SolidityProgramElement next =
            prefixPos.hasNext() ? (SolidityProgramElement) context.getChild(prefixPos.next())
                    : null;

        if (!prefixPos.hasNext()) {
            return createWrapperBody(context, putIn, suffix);
        } else {
            final SolidityProgramElement body = wrap(next, putIn, prefixPos, suffix);
            if (context instanceof Block block) {
                return createBlockWrapper(block, body);
            } else {
                throw new RuntimeException(
                    new UnexpectedException("Unexpected block type: " + context.getClass()));
            }
        }
    }

    /// Inserts the content of `putIn` and appends the succeeding (suffix) children of the innermost
    /// context block.
    private Block createWrapperBody(SolidityProgramElement wrapper, ContextStatementBlock putIn,
            PosInProgram suffix) {
        final int putInLength = putIn.getChildCount();

        // ATTENTION: may be -1
        final int lastChild = suffix.last();
        final int childLeft = wrapper.getChildCount() - lastChild;
        final int childrenToAdd = putInLength + childLeft;

        if (childLeft == 0 || lastChild == -1) {
            return new Block(putIn.getStatements());
        }

        final Statement[] body = new Statement[childrenToAdd];
        for (int i = 0; i < putInLength; i++) {
            body[i] = (Statement) putIn.getChild(i);
        }
        for (int i = putInLength; i < childrenToAdd; i++) {
            body[i] = (Statement) wrapper.getChild(lastChild + (i - putInLength));
        }
        return new Block(new ImmutableArray<>(body));
    }

    /// Replaces the first statement of a block by the (already wrapped) replacement. Optimised: if
    /// the block had a single statement and the replacement is itself a block, the replacement is
    /// returned directly.
    protected Block createBlockWrapper(Block wrapper,
            @Nullable SolidityProgramElement replacement) {
        final int childrenCount = wrapper.getChildCount();
        if (childrenCount <= 1 && replacement instanceof Block block) {
            return block;
        }
        final Statement[] body = new Statement[childrenCount > 0 ? childrenCount : 1];
        body[0] = (Statement) replacement;
        for (int i = 1; i < childrenCount; i++) {
            body[i] = (Statement) wrapper.getChild(i);
        }
        return new Block(new ImmutableArray<>(body));
    }
}
