/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.speclang.natspec;

import java.util.function.Consumer;

import org.key_project.solidity.parser.SolSpecLexer;
import org.key_project.solidity.parser.SolSpecParser;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.tree.ParseTree;

/// Parses a specification expression with the `SolSpec.g4` grammar. A syntax error is a
/// [SpecException] naming the expression, so the CLI and the GUI report it like an unprovable
/// function.
public final class SpecParser {

    private SpecParser() {}

    public static SolSpecParser.ExprContext parse(String source) {
        BaseErrorListener errors = new BaseErrorListener() {
            @Override
            public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol,
                    int line, int charPositionInLine, String msg, RecognitionException e) {
                throw new SpecException(msg + " at position " + charPositionInLine + " in '"
                    + source + "'");
            }
        };
        SolSpecLexer lexer = new SolSpecLexer(CharStreams.fromString(source));
        lexer.removeErrorListeners();
        lexer.addErrorListener(errors);
        SolSpecParser parser = new SolSpecParser(new CommonTokenStream(lexer));
        parser.removeErrorListeners();
        parser.addErrorListener(errors);
        return parser.spec().expr();
    }

    /// Whether `\old(...)` occurs anywhere in the expression.
    public static boolean usesOld(ParseTree tree) {
        if (tree instanceof SolSpecParser.OldContext) {
            return true;
        }
        for (int i = 0; i < tree.getChildCount(); i++) {
            if (usesOld(tree.getChild(i))) {
                return true;
            }
        }
        return false;
    }

    /// Visits every quantifier of the expression, outermost first.
    public static void forEachQuantifier(ParseTree tree,
            Consumer<SolSpecParser.QuantifierContext> action) {
        if (tree instanceof SolSpecParser.QuantifierContext quantifier) {
            action.accept(quantifier);
        }
        for (int i = 0; i < tree.getChildCount(); i++) {
            forEachQuantifier(tree.getChild(i), action);
        }
    }
}
