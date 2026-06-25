/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.parser;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

import org.key_project.logic.*;
import org.key_project.logic.op.sv.SchemaVariable;
import org.key_project.prover.rules.Taclet;
import org.key_project.prover.sequent.*;
import org.key_project.solidity.common.Services;
import org.key_project.solidity.logic.NamespaceSet;
import org.key_project.solidity.proof.calculus.SoliditySequentKit;
import org.key_project.solidity.rule.sv.ProgramSV;
import org.key_project.solidity.rule.taclets.builder.*;
import org.key_project.util.collection.ImmutableSLList;

import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TacletParserTest {
    private static final String DECLS =
        ("""
                \\sorts { s; }
                \\functions {
                  s f(s);
                }
                \\schemaVariables {
                  \\formula b,b0,post;
                  \\term s x,x0 ;
                  \\skolemTerm s sk ;
                  \\variables s z,z0 ;
                }
                """);

    private Namespace<SchemaVariable> schemaVariableNS;
    private KeYIO io;

    @BeforeEach
    public void setUp() throws IOException {
        NamespaceSet nss = new NamespaceSet();
        Services services = new Services();
        io = new KeYIO(services, nss);
        parseDecls(DECLS);
    }

    //
    // Utility methods for setUp:
    //

    private SchemaVariable lookup_schemavar(String name) {
        return schemaVariableNS.lookup(new Name(name));
    }


    private void parseDecls(String s) throws IOException {
        KeYIO.Loader l = io.load(s);
        l.loadComplete();
        schemaVariableNS = l.getSchemaNamespace();
        io.setSchemaNamespace(schemaVariableNS);
    }

    public Term parseTerm(String s) {
        return io.parseExpression(s);
    }

    public Term parseFma(String s) {
        return parseTerm(s);
    }

    public SequentFormula sf(String s) {
        return new SequentFormula(parseFma(s));
    }

    public Sequent sequent(String a, String s) {
        var antec = ImmutableSLList.<SequentFormula>nil();
        var succ = ImmutableSLList.<SequentFormula>nil();
        if (a != null) {
            antec = ImmutableSLList.singleton(sf(a));
        }
        if (s != null) {
            succ = ImmutableSLList.singleton(sf(s));
        }
        return SoliditySequentKit.createSequent(antec, succ);
    }

    Taclet parseTaclet(String s) {
        s = "\n\\rules { " + s + "; }";
        try {
            KeYIO.Loader l = io.load(s);
            List<Taclet> taclets = l.loadComplete();
            return taclets.getFirst();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    @Test
    public void testPathAwareProgramSVSortDeclarations() throws IOException {
        parseDecls("""
                \\schemaVariables {
                  \\program StoragePath storagePath;
                  \\program SimpleStoragePath simpleStoragePath;
                  \\program ComplexStoragePath complexStoragePath;
                  \\program MemoryPath memoryPath;
                  \\program SimpleMemoryPath simpleMemoryPath;
                  \\program ComplexMemoryPath complexMemoryPath;
                  \\program Path[name=storage.simple.array] arrayPath;
                  \\program Path[name=storage.simple.mapping] mappingPath;
                  \\program Path path;
                }
                """);

        assertInstanceOf(ProgramSV.class, lookup_schemavar("storagePath"));
        assertInstanceOf(ProgramSV.class, lookup_schemavar("simpleStoragePath"));
        assertInstanceOf(ProgramSV.class, lookup_schemavar("complexStoragePath"));
        assertInstanceOf(ProgramSV.class, lookup_schemavar("memoryPath"));
        assertInstanceOf(ProgramSV.class, lookup_schemavar("simpleMemoryPath"));
        assertInstanceOf(ProgramSV.class, lookup_schemavar("complexMemoryPath"));
        assertInstanceOf(ProgramSV.class, lookup_schemavar("arrayPath"));
        assertInstanceOf(ProgramSV.class, lookup_schemavar("mappingPath"));
        assertInstanceOf(ProgramSV.class, lookup_schemavar("path"));
    }

    @Test
    public void testInvalidParameterizedPathAwareProgramSVSortDeclarationFails() {
        assertThrows(Exception.class, () -> parseDecls("""
                \\schemaVariables {
                  \\program Path[name=storage.memory] invalidPath;
                }
                """));
        assertThrows(Exception.class, () -> parseDecls("""
                \\schemaVariables {
                  \\program Path[name=array.mapping] invalidPath;
                }
                """));
    }

    @Test
    public void testHasElementSortVarcondParses() throws IOException {
        parseDecls("""
                \\sorts {
                  \\generic alpha;
                  int;
                  List;
                }
                \\schemaVariables {
                  \\program Path[name=storage.simple] sp;
                }
                """);

        Taclet taclet = parseTaclet("""
                hasElementSortSmoke {
                   \\find(x = x)
                   \\varcond(\\hasElementSort(sp, \\sort(alpha)))
                   \\replacewith(x = x)
                }
                """);

        assertNotNull(taclet);
    }

    @Test
    public void testHasFieldSortVarcondParses() throws IOException {
        parseDecls("""
                \\sorts {
                  \\generic alpha;
                }
                \\schemaVariables {
                  \\program Field a;
                }
                """);

        Taclet taclet = parseTaclet("""
                hasFieldSortSmoke {
                   \\find(x = x)
                   \\varcond(\\hasFieldSort(a, \\sort(alpha)))
                   \\replacewith(x = x)
                }
                """);

        assertNotNull(taclet);
    }

    @Test
    public void testImpLeft() {
        // imp-left rule
        // find(b->b0 =>) replacewith(b0 =>) replacewith(=> b)
        AntecTacletBuilder builder = new AntecTacletBuilder();
        builder.setFind(sequent("b->b0", null));
        builder.setName(new Name("imp_left"));
        final Sequent emptySequent = SoliditySequentKit.getInstance().getEmptySequent();
        builder.addTacletGoalTemplate(
            new AntecSuccTacletGoalTemplate(emptySequent,
                ImmutableSLList.nil(), sequent("b0", null)));

        builder.addTacletGoalTemplate(
            new AntecSuccTacletGoalTemplate(emptySequent,
                ImmutableSLList.nil(), sequent(null, "b")));
        var c = builder.getChoices();

        Taclet impleft = builder.getAntecTaclet(io.getServices());
        String impleftString = """
                    imp_left {
                       \\find(b->b0 ==>)
                       \\replacewith(b0 ==>);
                       \\replacewith(==> b)
                    }
                """;
        assertTrue(equals(impleft, parseTaclet(impleftString)));
    }

    private boolean equals(Taclet a, Taclet b) {
        if (a == b) {
            return true;
        }
        if (a.getClass() != b.getClass()) {
            return false;
        }
        if (a.closeGoal() != b.closeGoal()) {
            return false;
        }
        if (!a.name().equals(b.name())) {
            return false;
        }
        if (!a.getAssumesAndFindVariables().equals(b.getAssumesAndFindVariables())) {
            return false;
        }
        if (!a.getBoundVariables().equals(b.getBoundVariables())) {
            return false;
        }
        if (!a.getChoices().equals(b.getChoices())) {
            return false;
        }
        if (!Objects.equals(a.getTrigger(), b.getTrigger())) {
            return false;
        }
        if (!Objects.equals(a.getVariableConditions(), b.getVariableConditions())) {
            return false;
        }
        if (a.goalTemplates().size() != b.goalTemplates().size()) {
            return false;
        }
        for (int i = 0; i < a.goalTemplates().size(); i++) {
            var agt = a.goalTemplates().get(i);
            var bgt = b.goalTemplates().get(i);
            if (!Objects.equals(agt.name(), bgt.name())) {
                return false;
            }

            SyntaxElement agtElement = agt.replaceWith();
            SyntaxElement bgtElement = bgt.replaceWith();
            if (!compareGTElement(agtElement, bgtElement))
                return false;
            if (!compareGTElement(agt.sequent(), bgt.sequent()))
                return false;
            if (!agt.addedProgVars().equals(bgt.addedProgVars())) {
                return false;
            }
        }
        return true;
    }

    private boolean compareGTElement(SyntaxElement agtElement, SyntaxElement bgtElement) {
        if (agtElement instanceof Term at) {
            if (!(bgtElement instanceof Term bt) || !at.equals(bt)) {
                return true;
            }
        } else if (agtElement instanceof Sequent as) {
            if (!(bgtElement instanceof Sequent bs) || bs.size() != as.size()) {
                return false;
            } else {
                for (int idx = 0; idx < as.size(); idx++) {
                    if (!as.getChild(idx).equals(bs.getChild(idx))) {
                        return false;
                    }
                }
            }
        } else if (agtElement != bgtElement) {
            return false;
        }
        return true;
    }

}
