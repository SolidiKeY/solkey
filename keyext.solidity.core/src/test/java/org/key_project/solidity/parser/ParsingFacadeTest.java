/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.parser;

import java.util.HashMap;

import org.key_project.logic.Name;
import org.key_project.logic.Term;
import org.key_project.logic.op.Function;
import org.key_project.logic.sort.Sort;
import org.key_project.solidity.common.Services;
import org.key_project.solidity.logic.SolidityDLTheory;
import org.key_project.solidity.logic.op.Junctor;
import org.key_project.solidity.logic.op.LogicVariable;
import org.key_project.solidity.logic.op.Quantifier;
import org.key_project.solidity.logic.op.SFunction;
import org.key_project.solidity.logic.op.SModality;
import org.key_project.solidity.logic.sort.SortImpl;
import org.key_project.solidity.program.ast.statement.Block;
import org.key_project.util.collection.ImmutableArray;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ParsingFacadeTest {

    private Services services;

    private HashMap<String, Function> predicates;


    @BeforeEach
    void setup() {
        services = new Services();

        SortImpl mySort = new SortImpl(new Name("MySort"));
        services.getNamespaces().sorts().addSafely(mySort);

        predicates = new HashMap<>();
        final Function[] atoms = { delcareAtom("A"), delcareAtom("B"),
            delcareAtom("C"), delcareAtom("D"), delcareAtom("p", mySort),
            delcareAtom("q", mySort, mySort) };

        for (Function atom : atoms) {
            predicates.put(atom.name().toString(), atom);
        }

        services.getNamespaces().functions().addSafely(predicates.values());

    }

    private Function delcareAtom(String name, Sort... argumentSorts) {
        return new SFunction(new Name(name), SolidityDLTheory.FORMULA,
            new ImmutableArray<>(argumentSorts), true);
    }

    private SFunction delcareAtom(String name) {
        return new SFunction(new Name(name), SolidityDLTheory.FORMULA, new ImmutableArray<>(),
            true);
    }

    @Test
    void parseTruthConstants() {
        KeYIO io = new KeYIO(services);
        final Term trueTerm = io.parseExpression("true");
        assertSame(Junctor.TRUE, trueTerm.op());
        final Term falseTerm = io.parseExpression("false");
        assertSame(Junctor.FALSE, falseTerm.op());
    }

    @Test
    void parsePropositionalFormula() {
        KeYIO io = new KeYIO(services);
        final Term term = io.parseExpression("A & (!B -> (C | D))");
        assertSame(Junctor.AND, term.op());
        assertSame(predicates.get("A"), term.sub(0).op());
        assertSame(Junctor.IMP, term.sub(1).op());
        assertSame(Junctor.NOT, term.sub(1).sub(0).op());
        assertSame(predicates.get("B"), term.sub(1).sub(0).sub(0).op());
        assertSame(Junctor.OR, term.sub(1).sub(1).op());
        assertSame(predicates.get("C"), term.sub(1).sub(1).sub(0).op());
        assertSame(predicates.get("D"), term.sub(1).sub(1).sub(1).op());
    }


    @Test
    void parseQuantifiedFormula() {
        KeYIO io = new KeYIO(services);
        final Term term = io.parseExpression("\\forall MySort x; p(x)");
        assertSame(Quantifier.ALL, term.op());
        assertSame(predicates.get("p"), term.sub(0).op());
    }

    @Test
    void parseNestedQuantifiedFormula() {
        KeYIO io = new KeYIO(services);
        final Term term = io.parseExpression("\\forall MySort x;\\forall MySort y; q(x, y)");
        assertSame(Quantifier.ALL, term.op());
        assertSame(Quantifier.ALL, term.sub(0).op());
        assertSame(predicates.get("q"), term.sub(0).sub(0).op());
        assertInstanceOf(LogicVariable.class, term.sub(0).sub(0).sub(0).op());
        assertEquals(2, ((LogicVariable) term.sub(0).sub(0).sub(0).op()).getIndex());
        assertEquals(1, ((LogicVariable) term.sub(0).sub(0).sub(1).op()).getIndex());
    }


    @Test
    void parseNestedQuantifiedFormula2() {
        KeYIO io = new KeYIO(services);
        final Term term = io.parseExpression("\\forall MySort y;\\forall MySort x; q(x, y)");
        assertSame(Quantifier.ALL, term.op());
        assertSame(Quantifier.ALL, term.sub(0).op());
        assertSame(predicates.get("q"), term.sub(0).sub(0).op());
        assertInstanceOf(LogicVariable.class, term.sub(0).sub(0).sub(0).op());
        assertEquals(1, ((LogicVariable) term.sub(0).sub(0).sub(0).op()).getIndex());
        assertEquals(2, ((LogicVariable) term.sub(0).sub(0).sub(1).op()).getIndex());
    }

    @Test
    void parseEmptyDiamondFormula() {
        KeYIO io = new KeYIO(services);
        final Term term = io.parseExpression("\\<{ }\\>true");
        assertInstanceOf(SModality.class, term.op());
        assertTrue(((SModality) term.op()).kind() == SModality.SolidityModalityKind.DIA);
        assert (((SModality) term.op()).programBlock().program() instanceof Block);
        Block block = (Block) ((SModality) term.op()).programBlock().program();
        assertTrue(block.getChildCount() == 0);
    }

    @Test
    void parseSimpleAssignmentFormula() {
        KeYIO io = new KeYIO(services);
        final Term term = io.parseExpression("\\<{ int i = 1; }\\>true");
        assertInstanceOf(SModality.class, term.op());
        assertTrue(((SModality) term.op()).kind() == SModality.SolidityModalityKind.DIA);
        assert (((SModality) term.op()).programBlock().program() instanceof Block);
        Block block = (Block) ((SModality) term.op()).programBlock().program();
        assertTrue(block.getChildCount() == 1);
        // assertTrue(block.getChild(0) instanceof Decl); local variable declaration representation
        // in AST
    }

    @Test
    void parseSequent() {
        KeYIO io = new KeYIO(services);
        final var sequent = io.parseSequent(
            "\\forall MySort y;\\forall MySort x; q(x, y) ==> \\exists MySort x; p(x)");
        assertEquals(2, sequent.size());
        assertEquals(1, sequent.antecedent().size());
        assertEquals(1, sequent.succedent().size());
        assertEquals(Quantifier.ALL, sequent.antecedent().get(0).formula().op());
        assertEquals(Quantifier.ALL, sequent.antecedent().get(0).formula().sub(0).op());
        assertEquals(Quantifier.EX, sequent.succedent().get(0).formula().op());
        assertEquals(predicates.get("p"), sequent.succedent().get(0).formula().sub(0).op());
    }

}
