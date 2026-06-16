/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.parser;

import java.util.HashMap;

import org.key_project.logic.*;
import org.key_project.logic.op.Function;
import org.key_project.logic.sort.Sort;
import org.key_project.solidity.common.Services;
import org.key_project.solidity.logic.SolidityDLTheory;
import org.key_project.solidity.logic.op.*;
import org.key_project.solidity.logic.sort.SortImpl;
import org.key_project.solidity.program.ast.abstractions.KeYSolidityType;
import org.key_project.solidity.program.ast.expressions.operators.AssignExpression;
import org.key_project.solidity.program.ast.statement.*;
import org.key_project.util.collection.ImmutableArray;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.key_project.solidity.program.ast.abstractions.PrimitiveType.UINT;

public class ParsingFacadeTest {

    private Services services;

    private HashMap<String, Function> predicates;

    private KeYSolidityType ksType;


    @BeforeEach
    void setup() {
        services = ParserForTesting.load().getServices();

        SortImpl mySort = new SortImpl(new Name("MySort"));
        services.getNamespaces().sorts().addSafely(mySort);

        predicates = new HashMap<>();
        final Function[] atoms = { declareAtom("A"), declareAtom("B"),
            declareAtom("C"), declareAtom("D"), declareAtom("p", mySort),
            declareAtom("q", mySort, mySort) };

        for (Function atom : atoms) {
            predicates.put(atom.name().toString(), atom);
        }

        services.getNamespaces().functions().addSafely(predicates.values());
        ksType = new KeYSolidityType(UINT, new SortImpl(new Name("UINT")));
    }

    private Function declareAtom(String name, Sort... argumentSorts) {
        return new SFunction(new Name(name), SolidityDLTheory.FORMULA,
            new ImmutableArray<>(argumentSorts), true);
    }

    private SFunction declareAtom(String name) {
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
    void quantifiedVariablesAreNotFree() {
        KeYIO io = new KeYIO(services);

        // a closed quantified formula has no free variables (the bound occurrence of x must not
        // leak out, despite being represented by a de Bruijn LogicVariable)
        final Term closed = io.parseExpression("\\forall MySort x; p(x)");
        assertTrue(closed.freeVars().isEmpty(),
            "\\forall x; p(x) has no free variables, but got: " + closed.freeVars());

        // ... while the body p(x), taken on its own, has x free (de Bruijn index 1)
        final Term body = closed.sub(0);
        assertEquals(1, body.freeVars().size());
        assertEquals(1, ((LogicVariable) body.freeVars().iterator().next()).getIndex());

        // nested: \forall x; \forall y; q(x,y) is closed, and its inner \forall y; q(x,y) has x
        // free, shifted to index 1 after crossing the y-binder
        final Term nested =
            io.parseExpression("\\forall MySort x;\\forall MySort y; q(x, y)");
        assertTrue(nested.freeVars().isEmpty(),
            "nested closed formula has no free variables, but got: " + nested.freeVars());
        final Term inner = nested.sub(0); // \forall y; q(x, y)
        assertEquals(1, inner.freeVars().size());
        assertEquals(1, ((LogicVariable) inner.freeVars().iterator().next()).getIndex());
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
        assertTrue(block.getStatements().isEmpty());
    }

    @Test
    void parseSimpleAssignmentFormula() {
        KeYIO io = new KeYIO(services);
        final Term term = io.parseExpression("\\<{ int i = 1; }\\>true");
        assertInstanceOf(SModality.class, term.op());
        assertTrue(((SModality) term.op()).kind() == SModality.SolidityModalityKind.DIA);
        assert (((SModality) term.op()).programBlock().program() instanceof Block);
        Block block = (Block) ((SModality) term.op()).programBlock().program();
        assertEquals(1, block.getStatements().size());
    }

    @Test
    void parseProgramVariable() {
        ProgramVariable px = new ProgramVariable(new Name("x"), ksType, null);
        services.getNamespaces().programVariables().add(px);

        KeYIO io = new KeYIO(services);
        final Term term = io.parseExpression("\\<{ x = 1; }\\>true");
        assertInstanceOf(SModality.class, term.op());
        assertSame(SModality.SolidityModalityKind.DIA, ((SModality) term.op()).kind());
        assert (((SModality) term.op()).programBlock().program() instanceof Block);
        Block block = (Block) ((SModality) term.op()).programBlock().program();
        assertEquals(1, block.getStatements().size());

        assertEquals(px,
            ((AssignExpression) ((ExpressionStatement) block.getStatements().get(0))
                    .getExpression())
                    .getLeft());
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
