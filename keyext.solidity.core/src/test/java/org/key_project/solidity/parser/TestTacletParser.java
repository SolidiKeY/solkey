package org.key_project.solidity.parser;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.key_project.logic.Name;
import org.key_project.logic.Namespace;
import org.key_project.logic.Term;
import org.key_project.logic.op.sv.SchemaVariable;
import org.key_project.prover.rules.Taclet;
import org.key_project.prover.sequent.Sequent;
import org.key_project.prover.sequent.SequentFormula;
import org.key_project.solidity.common.Services;
import org.key_project.solidity.logic.NamespaceSet;
import org.key_project.solidity.proof.calculus.SoliditySequentKit;
import org.key_project.solidity.rule.taclets.builder.AntecSuccTacletGoalTemplate;
import org.key_project.solidity.rule.taclets.builder.AntecTacletBuilder;
import org.key_project.util.collection.ImmutableSLList;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestTacletParser {
    private static final String DECLS =
            ("""
                    \\sorts { s; }
                    \\functions {
                      s f(s);
                    }
                    \\schemaVariables {
                      \\formula b,b0,post;
                      \\program Statement #p1, #s ;\s
                      \\program Expression #e2, #e ;\s
                      \\program SimpleExpression #se ;\s
                      \\program Variable #slhs, #arr, #ar, #ar1 ;\s
                      \\program LoopInit #i ;\s
                      \\program Label #lab, #lb0, #lb1 ;\s
                      \\program Label #inner, #outer ;\s
                      \\program Type #typ ;\s
                      \\program Variable #v0, #v, #v1, #k, #boolv ;\s
                      \\program[list] Catch #cf ;\s
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
            return taclets.get(0);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    @Disabled("TODO: Richard")
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

        Taclet impleft = builder.getAntecTaclet(io.getServices());
        String impleftString =
                "imp_left{\\find(b->b0 ==>) \\replacewith(b0 ==>); \\replacewith(==> b)}";
        assertEquals(impleft, parseTaclet(impleftString), "imp-left");
    }
}