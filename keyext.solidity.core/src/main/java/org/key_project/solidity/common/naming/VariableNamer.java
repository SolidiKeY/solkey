/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.common.naming;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import org.key_project.logic.Name;
import org.key_project.logic.Named;
import org.key_project.logic.Term;
import org.key_project.logic.op.sv.SchemaVariable;
import org.key_project.logic.sort.Sort;
import org.key_project.prover.sequent.PosInOccurrence;
import org.key_project.solidity.common.Services;
import org.key_project.solidity.logic.NamespaceSet;
import org.key_project.solidity.logic.op.ProgramVariable;
import org.key_project.solidity.logic.op.SModality;
import org.key_project.solidity.program.PosInProgram;
import org.key_project.solidity.program.ast.SolidityProgramElement;
import org.key_project.solidity.program.ast.abstractions.Type;
import org.key_project.solidity.program.ast.expressions.Expression;
import org.key_project.solidity.program.ast.statement.Block;
import org.key_project.solidity.program.ast.visitor.SolidityASTWalker;
import org.key_project.solidity.proof.Goal;
import org.key_project.solidity.proof.Node;
import org.key_project.solidity.rule.TacletApp;
import org.key_project.solidity.rule.matching.inst.ContextInstantiationEntry;
import org.key_project.solidity.rule.sv.ProgramSV;
import org.key_project.solidity.rule.sv.sort.ProgramSVSort;
import org.key_project.solidity.rule.taclets.NewVarcond;
import org.key_project.util.collection.ImmutableList;
import org.key_project.util.collection.KeYCollections;

import org.jspecify.annotations.Nullable;

public class VariableNamer {
    /// default basename for variable name proposals
    private static final String DEFAULT_BASENAME = "var";
    /// name of the counter object used for temporary name proposals
    private static final String TEMPCOUNTER_NAME = "VarNamerCnt";

    /// pointer to services object
    protected final Services services;

    protected final HashMap<ProgramVariable, ProgramVariable> map =
        new LinkedHashMap<>();
    protected HashMap<ProgramVariable, ProgramVariable> renamingHistory =
        new LinkedHashMap<>();

    // -------------------------------------------------------------------------
    // constructors
    // -------------------------------------------------------------------------

    /// @param services pointer to services object
    public VariableNamer(Services services) {
        this.services = services;
    }

    /// proposes a unique name for the instantiation of a schema variable
    ///
    /// **Warning:** The current version does not yet guarantee a unique name,
    /// but it is very important that this is implemented in the future.
    ///
    /// @param app the taclet app
    /// @param var the schema variable to be instantiated
    /// @param services not used
    /// @param undoAnchor not used
    /// @param previousProposals list of names which should be considered taken, or null
    /// @return the name proposal, or null if no proposal is available
    public @Nullable String getProposal(TacletApp app, SchemaVariable var,
            Services services, @Nullable Node undoAnchor,
            ImmutableList<String> previousProposals) {
        ContextInstantiationEntry cie = app.instantiations().getContextInstantiation();
        PosInProgram posOfDeclaration = (cie == null ? null : cie.prefix());

        NewVarcond nv = (NewVarcond) app.taclet().varDeclaredNew(var);
        // determine a suitable base name
        String basename = null;
        if (nv != null) {
            Type type = nv.getType();
            if (type != null) {
                basename = getBaseNameProposal(type);
            } else {
                SchemaVariable psv = nv.getPeerSchemaVariable();
                Object inst = app.instantiations().getInstantiation(psv);
                if (inst instanceof Expression e) {
                    Type ty = e.getType();
                    basename = getBaseNameProposal(ty);
                } else {
                    // usually this should never be entered, but because of
                    // naming issues we do not want null pointer exceptions
                    // 'u' for unknown
                    basename = "u";
                }
            }
        }
        // get the proposal
        return getNameProposalForSchemaVariable(basename, var,
            app.posInOccurrence(), posOfDeclaration, previousProposals, services);
    }

    // precondition: sv.sort()==ProgramSVSort.VARIABLE
    public @Nullable String getSuggestiveNameProposalForProgramVariable(
            SchemaVariable sv, TacletApp app,
            Services services, ImmutableList<String> previousProposals) {
        return getProposal(app, sv, services, null, previousProposals);
    }

    /// intended to be called when symbolically executing a variable declaration; resolves any
    /// naming
    /// conflicts between the new variable and other global variables by renaming the new variable
    /// and / or other variables
    ///
    /// @param var the new program variable
    /// @param goal the goal
    /// @param posOfFind the PosInOccurrence of the currently executed program
    /// @return the renamed version of the var parameter
    public ProgramVariable rename(ProgramVariable var, Goal goal, PosInOccurrence posOfFind) {
        var name = var.name();
        BasenameAndIndex bai = getBasenameAndIndex(name);
        Iterable<Name> globals = wrapGlobals(goal.getNode().getLocalProgVars());
        map.clear();

        Name proposal = services.getNameRecorder().getProposal();
        Name newName;
        if (proposal != null) {
            newName = proposal;
        } else {
            newName = new Name(bai.basename() + (bai.index() == 0 ? "" : "_" + bai.index()));
            int newcounter = getMaxCounterInGlobalsAndProgram(bai.basename(), globals,
                getProgramFromPIO(posOfFind), null);
            final NamespaceSet namespaces = services.getNamespaces();

            while (!isUniqueInGlobals(newName.toString(), globals)
                    || namespaces.lookupLogicSymbol(newName) != null) {
                newcounter += 1;
                newName = new Name(bai.basename() + "_" + newcounter);
            }
        }

        ProgramVariable newVar = var;
        if (!newName.equals(name)) {
            newVar = new ProgramVariable(newName, var.getKeYSolidityType());
            map.put(var, newVar);
            renamingHistory = map;
        }

        assert isUniqueInGlobals(newVar.name().toString(), globals);
        assert services.getNamespaces().lookupLogicSymbol(newVar.name()) == null;
        return newVar;
    }

    /// returns the maximum counter for the passed basename in the passed globals and the passed
    /// program
    private int getMaxCounterInGlobalsAndProgram(String basename,
            Iterable<Name> globals, SolidityProgramElement program,
            @Nullable PosInProgram posOfDeclaration) {
        int maxInGlobals = getMaxCounterInGlobals(basename, globals);
        int maxInProgram = getMaxCounterInProgram(basename, program, posOfDeclaration);

        return Math.max(maxInGlobals, maxInProgram);
    }

    public HashMap<ProgramVariable, ProgramVariable> getRenamingMap() {
        return renamingHistory;
    }

    /// proposes a base name for a given sort
    private String getBaseNameProposal(Type type) {
        String result;
        String name = type.name().toString();
        name = KeYCollections.filterAlphabetic(name);
        if (!name.isEmpty()) {
            result = name.substring(0, 1).toLowerCase();
        } else {
            result = "x"; // use default name otherwise
        }

        return result;
    }

    /// proposes a unique name for the instantiation of a schema variable (like getProposal(), but
    /// somewhat less nicely)
    ///
    /// @param basename desired base name, or null to use default
    /// @param sv the schema variable
    /// @param posOfFind the PosInOccurrence containing the name's target program
    /// @param posOfDeclaration the PosInProgram where the name will be declared (or null to just be
    /// pessimistic about the scope)
    /// @param previousProposals list of names which should be considered taken, or null
    /// @return the name proposal, or null if no proposal is available
    protected @Nullable String getNameProposalForSchemaVariable(@Nullable String basename,
            SchemaVariable sv, PosInOccurrence posOfFind, @Nullable PosInProgram posOfDeclaration,
            ImmutableList<String> previousProposals, Services services) {
        String result = null;

        if (sv instanceof ProgramSV psv) {
            Sort svSort = psv.sort();

            if (svSort == ProgramSVSort.VARIABLE) {
                if (basename == null || basename.isEmpty()) {
                    basename = DEFAULT_BASENAME;
                }
                int cnt =
                    getMaxCounterInProgram(basename, getProgramFromPIO(posOfFind), posOfDeclaration)
                            + 1;

                Name tmpName = new Name(basename + (cnt == 0 ? "" : "_" + cnt));
                while (services.getNamespaces().lookupLogicSymbol(tmpName) != null) {
                    cnt++;
                    tmpName = new Name(basename + "_" + cnt);
                }

                result = tmpName.toString();

                // avoid using a previous proposal again
                if (previousProposals != null) {
                    boolean collision;
                    do {
                        collision = false;
                        for (String previousProposal : previousProposals) {
                            if (previousProposal.equals(result.toString())) {
                                result = basename + ++cnt;
                                collision = true;
                                break;
                            }
                        }
                    } while (collision);
                }
            }
        }

        return result;
    }

    /// returns the maximum counter value already associated with the passed basename in the passed
    /// program (ignoring temporary counters), or -1
    protected int getMaxCounterInProgram(String basename, SolidityProgramElement program,
            @Nullable PosInProgram posOfDeclaration) {
        class MyWalker extends CustomSolidityASTWalker {
            public String basename;
            public int maxCounter = -1;

            public MyWalker(SolidityProgramElement program, @Nullable PosInProgram posOfDeclaration,
                    Services services) {
                super(program, posOfDeclaration, services);
            }

            protected void doAction(SolidityProgramElement node) {
                if (node instanceof ProgramVariable var) {
                    Name name = var.name();
                    if (name.toString().equals(basename) && 0 > maxCounter) {
                        maxCounter = 0;
                    } else if (name.toString().contains("_")) {
                        BasenameAndIndex bai = getBasenameAndIndex(name);
                        if (bai != null && bai.basename.equals(basename)
                                && bai.index > maxCounter) {
                            maxCounter = bai.index;
                        }
                    }
                }
            }

        }

        MyWalker walker = new MyWalker(program, posOfDeclaration, services);
        walker.basename = basename;
        walker.run();

        return walker.maxCounter;
    }

    /// creates a Globals object for use with other internal methods
    protected Iterable<Name> wrapGlobals(Iterable<? extends Named> globals) {
        List<Name> result = new ArrayList<>();
        for (Named named : globals) {
            result.add(named.name());
        }
        return result;
    }

    /// returns the maximum counter value already associated with the passed basename in the passed
    /// list of global variables, or -1
    protected int getMaxCounterInGlobals(String basename, Iterable<Name> globals) {
        int result = -1;

        for (var name : globals) {
            BasenameAndIndex bai = getBasenameAndIndex(name);
            if (bai.basename.equals(basename) && bai.index > result) {
                result = bai.index;
            }
        }

        return result;
    }

    /// tells whether a name is unique in the passed list of global variables
    protected boolean isUniqueInGlobals(String name, Iterable<Name> globals) {
        for (var n : globals) {
            if (n.toString().equals(name)) {
                return false;
            }
        }
        return true;
    }

    /// proposes a unique name; intended for use in places where the information required by
    /// getProposal() is not available
    ///
    /// @param basename desired base name, or null to use default
    /// @return the name proposal
    public Name getTemporaryNameProposal(String basename) {
        if (basename == null || basename.isEmpty()) {
            basename = DEFAULT_BASENAME;
        }
        int cnt = services.getCounter(TEMPCOUNTER_NAME).getCountPlusPlus();
        return new Name(basename + (cnt == 0 ? "" : "_" + cnt));
    }

    /// a customized Solidity AST walker
    private abstract static class CustomSolidityASTWalker extends SolidityASTWalker {
        private @Nullable SolidityProgramElement declarationNode = null;
        private int declarationScopeDepth = -2;
        private int currentScopeDepth = -2;

        CustomSolidityASTWalker(SolidityProgramElement program,
                @Nullable PosInProgram posOfDeclaration,
                Services services) {
            super(program);
            if (posOfDeclaration != null) {
                declarationNode = PosInProgram.getProgramAt(posOfDeclaration, program);
            }
        }

        protected void walk(SolidityProgramElement node) {
            if (node instanceof Block) {
                currentScopeDepth = depth();
            } else if (node == declarationNode) {
                declarationScopeDepth = currentScopeDepth;
            } else if (depth() <= declarationScopeDepth) {
                return;
            }
            super.walk(node);
        }
    }

    protected BasenameAndIndex getBasenameAndIndex(Name name) {
        String s = name.toString();
        var splits = s.split("_");
        try {
            String last = splits[splits.length - 1];
            int idx = Integer.parseInt(last);
            var base = s.substring(0, s.length() - last.length() - 1);
            return new BasenameAndIndex(base, idx);
        } catch (NumberFormatException e) {
            return new BasenameAndIndex(s, 0);
        }
    }

    /// returns the program contained in a PosInOccurrence
    protected SolidityProgramElement getProgramFromPIO(PosInOccurrence pio) {
        Term progTerm;
        if (pio != null && (progTerm = findProgramInTerm(pio.subTerm())) != null) {
            var mod = (SModality) progTerm.op();
            return mod.programBlock().program();
        } else {
            return null;
            // return new EmptyStatement();
        }
    }

    /// returns the subterm containing a java block, or null (helper for getProgramFromPIO())
    private @Nullable Term findProgramInTerm(Term term) {
        if (term.op() instanceof SModality) {
            return term;
        }
        for (int i = 0; i < term.arity(); i++) {
            Term subterm = findProgramInTerm(term.sub(i));
            if (subterm != null) {
                return subterm;
            }
        }
        return null;
    }

    protected record BasenameAndIndex(String basename, int index) {
    }
}
