/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.rule.sv;

import java.util.ArrayList;

import org.key_project.logic.Name;
import org.key_project.logic.SyntaxElement;
import org.key_project.logic.Term;
import org.key_project.logic.op.UpdateableOperator;
import org.key_project.solidity.common.Services;
import org.key_project.solidity.program.ast.SolidityProgramElement;
import org.key_project.solidity.program.ast.SourceData;
import org.key_project.solidity.program.ast.abstractions.Type;
import org.key_project.solidity.program.ast.expressions.Expression;
import org.key_project.solidity.program.ast.statement.Statement;
import org.key_project.solidity.program.ast.visitor.Visitor;
import org.key_project.solidity.rule.matching.inst.MatchConditions;
import org.key_project.solidity.rule.matching.inst.ProgramList;
import org.key_project.solidity.rule.matching.inst.SVInstantiations;
import org.key_project.solidity.rule.sv.sort.ProgramSVSort;
import org.key_project.util.collection.ImmutableArray;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProgramSV extends OperatorSV implements Expression, Statement, UpdateableOperator {
    public static final Logger LOGGER = LoggerFactory.getLogger(ProgramSV.class);

    private final boolean isListSV;
    private final Type type;

    private static final ProgramList EMPTY_LIST_INSTANTIATION =
        new ProgramList(new ImmutableArray<>(new SolidityProgramElement[0]));

    /// creates a new SchemaVariable used as a placeholder for program constructs
    ///
    /// @param name the Name of the SchemaVariable allowed to match a list of program constructs
    ProgramSV(Name name, ProgramSVSort s, boolean isListSV) {
        this(name, s, isListSV, null);
    }

    ProgramSV(Name name, ProgramSVSort s, boolean isListSV, Type type) {
        super(name, s, false, false);
        this.isListSV = isListSV;
        this.type = type;
    }

    public boolean isListSV() {
        return isListSV;
    }

    @Override
    public @NonNull SyntaxElement getChild(int n) {
        throw new IndexOutOfBoundsException("Index should be 0 <= " + n + " < " + getChildCount());
    }

    @Override
    public int getChildCount() {
        return 0;
    }

    @Override
    public void visit(Visitor v) {
        v.performActionOnSchemaVariable(this);
    }

    @Override
    public Type getType() {
        return type;
    }



    /**
     * adds a found mapping from schema variable <code>var</code> to program element <code>pe</code>
     * and returns the updated match conditions or null if mapping is not possible because of
     * violating some variable condition
     *
     * @param pe the ProgramElement <code>var</code> is mapped to
     * @param matchCond the MatchConditions to be updated
     * @param services the Services provide access to the Java model
     * @return the updated match conditions including mapping <code>var</code> to <code>pe</code> or
     *         null if some variable condition would be hurt by the mapping
     */
    private MatchConditions addProgramInstantiation(SolidityProgramElement pe,
            MatchConditions matchCond,
            Services services) {
        if (matchCond == null) {
            return null;
        }

        SVInstantiations insts = matchCond.getInstantiations();

        final Object foundInst = insts.getInstantiation(this);

        if (foundInst != null) {
            final Object newInst;
            if (foundInst instanceof Term) {
                newInst = Services.convertToLogicElement(pe, services);
            } else {
                newInst = pe;
            }

            if (foundInst.equals(newInst)) {
                return matchCond;
            } else {
                return null;
            }
        }

        insts = insts.add(this, pe, services);
        return insts == null ? null : matchCond.setInstantiations(insts);
    }

    /**
     * adds a found mapping from schema variable <code>var</code> to the list of program elements
     * <code>list</code> and returns the updated match conditions or null if mapping is not possible
     * because of violating some variable condition
     *
     * @param list the ProgramList <code>var</code> is mapped to
     * @param matchCond the MatchConditions to be updated
     * @param services the Services provide access to the Java model
     * @return the updated match conditions including mapping <code>var</code> to <code>list</code>
     *         or null if some variable condition would be hurt by the mapping
     */
    private MatchConditions addProgramInstantiation(ImmutableArray<SolidityProgramElement> list,
            MatchConditions matchCond,
            Services services) {
        if (matchCond == null) {
            return null;
        }

        SVInstantiations insts = matchCond.getInstantiations();
        final var pl = (ImmutableArray<SolidityProgramElement>) insts.getInstantiation(this);
        if (pl != null) {
            if (pl.equals(list)) {
                return matchCond;
            } else {
                return null;
            }
        }

        insts = insts.add(this, new ProgramList(list), services);
        return insts == null ? null : matchCond.setInstantiations(insts);
    }

    /**
     * returns true, if the given SchemaVariable can stand for the ProgramElement
     *
     * @param match the ProgramElement to be matched
     * @param services the Services object encapsulating information about the java datastructures
     *        like (static)types etc.
     * @return true if the SchemaVariable can stand for the given element
     */
    private boolean check(SolidityProgramElement match, Services services) {
        if (match == null) {
            return false;
        }
        return ((ProgramSVSort) sort()).canStandFor(match, services);
    }

    @Override
    public MatchConditions match(SourceData source, MatchConditions matchCond) {
        if (isListSV()) {
            return matchListSV(source, matchCond);
        }

        final Services services = source.getServices();
        final SolidityProgramElement src = source.getSource();

        final SVInstantiations instantiations = matchCond.getInstantiations();

        if (!check(src, services)) {
            return null;
        }

        final Object instant = instantiations.getInstantiation(this);
        if (instant == null || instant.equals(src)
                || (instant instanceof Term instantiation && instantiation.op() == src)) {

            matchCond = addProgramInstantiation(src, matchCond, services);

            if (matchCond == null) {
                // FAILED due to incompatibility with already found matchings
                // (e.g. generic sorts)
                return null;
            }
        } else {
            LOGGER.debug("Match failed: Former match of "
                + " SchemaVariable incompatible with " + " the current match.");
            return null; // FAILED mismatch
        }
        source.next();
        return matchCond;
    }

    private MatchConditions matchListSV(SourceData source, MatchConditions matchCond) {
        final Services services = source.getServices();
        SolidityProgramElement src = source.getSource();

        if (src == null) {
            return addProgramInstantiation(EMPTY_LIST_INSTANTIATION.list(), matchCond, services);
        }

        SVInstantiations instantiations = matchCond.getInstantiations();

        final ArrayList<SolidityProgramElement> matchedElements =
            new ArrayList<>();

        while (src != null) {
            if (!check(src, services)) {
                break;
            }
            matchedElements.add(src);
            source.next();
            src = source.getSource();
        }

        return addProgramInstantiation(new ImmutableArray<>(matchedElements), matchCond, services);
    }
}
