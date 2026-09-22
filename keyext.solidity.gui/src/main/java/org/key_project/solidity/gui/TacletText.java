/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.gui;

import org.key_project.solidity.common.Services;
import org.key_project.solidity.pp.LogicPrinter;
import org.key_project.solidity.pp.NotationInfo;
import org.key_project.solidity.pp.PosTableLayouter;
import org.key_project.solidity.rule.TacletApp;

/// Renders the taclet behind a rule application for display. The application's schema-variable
/// instantiations are passed to the printer, so a modality schema variable is shown as the
/// modality it was instantiated with — a diamond as `\<...\>`, not as a box.
final class TacletText {

    private static final int LINE_WIDTH = 80;

    private TacletText() {}

    static String of(TacletApp app, Services services) {
        try {
            LogicPrinter printer = new LogicPrinter(new NotationInfo(), services,
                PosTableLayouter.pure(LINE_WIDTH));
            printer.printTaclet(app.taclet(), app.instantiations(), true, false);
            return printer.result();
        } catch (RuntimeException e) {
            return app.taclet().toString();
        }
    }
}
