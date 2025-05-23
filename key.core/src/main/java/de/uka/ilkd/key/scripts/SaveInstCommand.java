/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package de.uka.ilkd.key.scripts;

import java.util.Map;

import de.uka.ilkd.key.control.AbstractUserInterfaceControl;
import de.uka.ilkd.key.logic.Term;
import de.uka.ilkd.key.logic.op.JFunction;
import de.uka.ilkd.key.logic.op.SchemaVariable;
import de.uka.ilkd.key.pp.AbbrevMap;
import de.uka.ilkd.key.rule.RuleApp;
import de.uka.ilkd.key.rule.TacletApp;

import org.key_project.logic.Name;

/**
 * Special "Let" usually to be applied immediately after a manual rule application. Saves the
 * instantiation of a {@link SchemaVariable} by the last {@link TacletApp} into an abbreviation for
 * later use. A nice use case is a manual loop invariant rule application, where the newly
 * introduced anonymizing Skolem constants can be saved for later interactive instantiations. As for
 * the {@link LetCommand}, it is not allowed to call this command multiple times with the same name
 * argument (all names used for remembering instantiations are "final").
 *
 * @author Dominic Steinhoefel
 */
public class SaveInstCommand extends AbstractCommand<Map<String, Object>> {
    public SaveInstCommand() {
        super(null);
    }

    @Override
    public Map<String, Object> evaluateArguments(EngineState state, Map<String, Object> arguments) {
        return arguments;
    }

    @Override
    public void execute(AbstractUserInterfaceControl uiControl, Map<String, Object> args,
            EngineState stateMap) throws ScriptException, InterruptedException {

        AbbrevMap abbrMap = stateMap.getAbbreviations();
        for (Map.Entry<String, Object> entry : args.entrySet()) {
            String key = entry.getKey();
            final var value = entry.getValue();
            if ("#1".equals(key)) {
                continue;
            }
            if ("#literal".equals(key)) {
                continue;
            }
            if (!key.startsWith("@")) {
                throw new ScriptException("Unexpected parameter to let, only @var allowed: " + key);
            }

            // get rid of @
            key = key.substring(1);

            if (abbrMap.containsAbbreviation(key)) {
                throw new ScriptException(key + " is already fixed in this script");
            }

            try {
                final RuleApp ruleApp =
                    stateMap.getFirstOpenAutomaticGoal().node().parent().getAppliedRuleApp();
                if (ruleApp instanceof TacletApp tacletApp) {
                    final Object inst = tacletApp.matchConditions().getInstantiations()
                            .lookupValue(new Name(value.toString()));
                    if (inst != null && ((Term) inst).op() instanceof JFunction) {
                        abbrMap.put((Term) inst, key, true);
                    } else {
                        throw new ScriptException(String.format(
                            "Tried to remember instantiation of schema variable %s "
                                + "as \"%s\", but instantiation is \"%s\" and not a function",
                            value, key, inst == null ? "null" : inst.toString()));
                    }
                }
            } catch (Exception e) {
                throw new ScriptException(e);
            }
        }

    }

    @Override
    public String getName() {
        return "saveInst";
    }
}
