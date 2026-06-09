/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.proof;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.key_project.logic.Name;
import org.key_project.logic.Term;
import org.key_project.prover.rules.RuleApp;
import org.key_project.prover.rules.Taclet;
import org.key_project.prover.sequent.SequentChangeInfo;
import org.key_project.solidity.logic.TermBuilder;
import org.key_project.solidity.logic.op.SModality;
import org.key_project.solidity.program.ProgramPrefix;
import org.key_project.solidity.program.ast.SolidityProgramElement;
import org.key_project.solidity.proof.io.ProofSaver;
import org.key_project.solidity.rule.PosTacletApp;
import org.key_project.solidity.rule.TacletApp;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/// The node info object contains additional information about a node used to give user feedback.
/// The
/// content does not have any influence on the proof or carry something of logical value.
public class NodeInfo {
    private static final Logger LOGGER = LoggerFactory.getLogger(NodeInfo.class);

    /// has the rule app of the node been applied interactively?
    private boolean interactiveApplication = false;

    private String branchLabel = null;
    /// the node this info object belongs to
    private final Node node;

    /// flag true if the first and active statement have been determined
    private boolean determinedFstAndActiveExpr = false;
    /// firstStatement stripped of method frames
    private @Nullable SolidityProgramElement activeStatement = null;
    /// used for proof tree annotation when applicable
    private @Nullable SolidityProgramElement firstExpr = null;

    private String firstExprString = null;

    /// Information about changes respective to the parent of this node.
    private SequentChangeInfo sequentChangeInfo;

    public NodeInfo(Node node) {
        this.node = node;
    }

    /// returns the branch label
    ///
    /// @return branch label
    public String getBranchLabel() {
        return branchLabel;
    }

    /// sets the branch label of a node. Schema variables occurring in string <tt>s</tt> are
    /// replaced
    /// by their instantiations if possible
    ///
    /// @param s the String to be set
    public void setBranchLabel(@Nullable String s) {
        if (s == null) {
            return;
        }
        if (node.parent() == null) {
            return;
        }
        RuleApp ruleApp = node.parent().getAppliedRuleApp();
        if (ruleApp instanceof TacletApp tacletApp) {
            Pattern p = Pattern.compile("#\\w+");
            Matcher m = p.matcher(s);
            StringBuffer sb = new StringBuffer();
            while (m.find()) {
                String arg = m.group();
                Object val = tacletApp.instantiations().lookupValue(new Name(arg));
                if (val == null) {
                    // chop off the leading '#'
                    String arg2 = arg.substring(1);
                    val = tacletApp.instantiations().lookupValue(new Name(arg2));
                }
                String res;
                if (val == null) {
                    LOGGER.warn(
                        "No instantiation for {}. Probably branch label not up to date in {}", arg,
                        tacletApp.rule().name());
                    res = arg; // use sv name instead
                } else {
                    // if (val instanceof ProgramVariable pv) {
                    // var originTracker = node.proof().lookup(LocationVariableTracker.class);
                    // if (originTracker != null) {
                    // var origin = originTracker.getCreatedBy(pv);
                    // if (origin instanceof PosTacletApp posTacletApp) {
                    // var name = posTacletApp.taclet().displayName();
                    // if (name.equals("ifElseUnfold") || name.equals("ifUnfold")) {
                    // val =
                    // posTacletApp.instantiations().lookupValue(new Name("#nse"));
                    // }
                    // }
                    // }
                    // }
                    res = ProofSaver.printAnything(val, node.proof().getServices());
                }
                m.appendReplacement(sb, res.replace("$", "\\$"));
            }
            m.appendTail(sb);
            // eliminate annoying whitespaces
            Pattern whiteSpacePattern = Pattern.compile("\\s+");
            Matcher whiteSpaceMatcher = whiteSpacePattern.matcher(sb);
            branchLabel = whiteSpaceMatcher.replaceAll(" ");
        } else {
            branchLabel = s;
        }
    }

    /// parameter indicated if the rule has been applied interactively or not
    ///
    /// @param b a boolean indicating interactive application
    public void setInteractiveRuleApplication(boolean b) {
        interactiveApplication = b;
    }

    /// returns true if the rule applied on this node has been performed manually by the user
    ///
    /// @return boolean for interactive rule application as described above
    public boolean getInteractiveRuleApplication() {
        return interactiveApplication;
    }

    /// returns a string representation of the first active expression or null if no such exists
    ///
    /// @return string representation of first expression as described above
    public String getFirstActiveExprString() {
        determineFirstAndActiveExpr();
        if (firstExpr != null) {
            firstExprString = String.valueOf(activeStatement);
            return firstExprString;
        }
        return null;
    }

    /// determines the first and active expr if the applied taclet worked on a modality
    private void determineFirstAndActiveExpr() {
        if (determinedFstAndActiveExpr) {
            return;
        }
        final RuleApp ruleApp = node.getAppliedRuleApp();
        if (ruleApp instanceof PosTacletApp) {
            firstExpr = computeFirstStatement(ruleApp);
            firstExprString = null;
            activeStatement = computeFirstStatement(ruleApp);
            determinedFstAndActiveExpr = true;
        }
    }

    /// Computes the first statement in the given [RuleApp].
    ///
    /// This functionality is independent of concrete [NodeInfo]s and used for instance by
    /// the symbolic execution tree extraction.
    ///
    /// @param ruleApp The given [RuleApp].
    /// @return The first statement or `null` if no one is provided.
    public static @Nullable SolidityProgramElement computeFirstStatement(
            RuleApp ruleApp) {
        SolidityProgramElement firstExpr = null;
        if (ruleApp instanceof PosTacletApp pta) {
            Term t = TermBuilder.goBelowUpdates(pta.posInOccurrence().subTerm());
            if (t.op() instanceof SModality mod) {
                final SolidityProgramElement pe = mod.programBlock().program();
                firstExpr = (SolidityProgramElement) pe.getChild(0);
            }
        }
        return firstExpr;
    }

    /// Computes the active statement in the given [RuleApp].
    ///
    /// This functionality is independent of concrete [NodeInfo]s and used for instance by
    /// the symbolic execution tree extraction.
    ///
    /// @param ruleApp The given [RuleApp].
    /// @return The active statement or `null` if no one is provided.
    public static @Nullable SolidityProgramElement computeActiveStatement(
            RuleApp ruleApp) {
        SolidityProgramElement firstStatement = computeFirstStatement(ruleApp);
        return computeActiveStatement(firstStatement);
    }

    /// Computes the active expression in the given [SolidityProgramElement].
    ///
    /// This functionality is independent of concrete [NodeInfo]s and used for instance by
    /// the symbolic execution tree extraction.
    ///
    /// @param firstExpr The given [SolidityProgramElement].
    /// @return The active expr or `null` if no one is provided.
    public static @Nullable SolidityProgramElement computeActiveStatement(
            SolidityProgramElement firstExpr) {
        SolidityProgramElement activeStatement = null;
        if (firstExpr != null) {
            activeStatement = firstExpr;
            while (activeStatement instanceof ProgramPrefix pp && pp.isPrefix()) {
                activeStatement = (SolidityProgramElement) activeStatement.getChild(0);
            }
        }
        return activeStatement;
    }

    public void updateNoteInfo() {
        determinedFstAndActiveExpr = false;
        firstExpr = null;
        firstExprString = null;
        activeStatement = null;
        determineFirstAndActiveExpr();
    }
}
