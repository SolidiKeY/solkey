/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.parser;

import org.key_project.solidity.util.parsing.BuildingException;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.Token;
import org.jspecify.annotations.Nullable;

public final class ThrowingErrorListener extends BaseErrorListener {
    public static final ThrowingErrorListener INSTANCE = new ThrowingErrorListener();

    private ThrowingErrorListener() {
    }

    @Override
    public void syntaxError(Recognizer<?, ?> recognizer, @Nullable Object offendingSymbol,
            int line, int charPositionInLine, String msg, @Nullable RecognitionException e) {
        String message = "Syntax error: " + msg;
        if (offendingSymbol instanceof Token token) {
            throw new BuildingException(token, message, e);
        }
        throw new BuildingException(message + " at " + recognizer.getInputStream().getSourceName()
            + ":" + line + ":" + charPositionInLine, e);
    }
}
