/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program;

import org.checkerframework.checker.initialization.qual.UnknownInitialization;
import org.jspecify.annotations.Nullable;
import org.key_project.rusty.ast.expr.FunctionFrame;
import org.key_project.rusty.logic.PossibleProgramPrefix;

public class ProgramPrefixUtil {
    public record ProgramPrefixInfo(int length/* , @Nullable MethodFrame innermostMethodFrame*/) {
    }

    public static ProgramPrefixInfo computeEssentials(
            @UnknownInitialization ProgramPrefix prefix) {
        int length = 1;
        while (prefix.hasNextPrefixElement()) {
            prefix = prefix.getNextPrefixElement();
            if (!prefix.isPrefix())
                break;
            length++;
        }
        return new ProgramPrefixInfo(length);
    }

}
