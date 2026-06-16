/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.parser;

import java.net.URI;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.Map;

import org.key_project.solidity.proof.io.IProofFileParser;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Token;
import org.jspecify.annotations.NonNull;

/// Replays a saved `\proof { ... }` s-expression by streaming its tokens to an
/// [IProofFileParser]. Mirrors KeY-Java's / KeY-Rust's `ProofReplayer`: it lexes the proof body
/// directly (no parser, no AST) and signals the begin/end of each `(id "arg" ...)` element.
public class ProofReplayer {
    /// translation between the symbols in the s-expression and the corresponding proof tag
    private static final Map<String, IProofFileParser.ProofElementID> proofSymbolElementId =
        new LinkedHashMap<>(32);

    static {
        for (IProofFileParser.ProofElementID id : IProofFileParser.ProofElementID.values()) {
            proofSymbolElementId.put(id.getRawName(), id);
        }
    }

    private ProofReplayer() {
    }

    /// Replays the proof represented by the expression following `token` in `input`.
    ///
    /// @param token the `\proof` token in the input stream
    /// @param input a valid input stream
    /// @param prl the proof replayer instance
    /// @param source the source of the stream, used for producing exceptions with locations
    public static void run(@NonNull Token token, CharStream input, IProofFileParser prl,
            URI source) {
        input.seek(1 + token.getStopIndex()); // position right after "\proof"
        run(input, prl, token.getLine(), source);
    }

    /// Replays the proof behind the given `input`, lexing it and consuming the tokens manually,
    /// signalling start/end of each s-expression element to the [IProofFileParser].
    ///
    /// @param input a valid input stream
    /// @param prl the proof replayer interface
    /// @param startLine the starting line of the s-expression (for `prl` positions)
    /// @param source the source of the stream, used for producing exceptions with locations
    public static void run(CharStream input, IProofFileParser prl, final int startLine,
            URI source) {
        KeYSolidityDLLexer lexer = ParsingFacade.createLexer(input);
        CommonTokenStream stream = new CommonTokenStream(lexer);
        // currently open proof elements and the positions where they were opened
        ArrayDeque<IProofFileParser.ProofElementID> stack = new ArrayDeque<>();
        Deque<Integer> posStack = new ArrayDeque<>();
        while (true) {
            int type = stream.LA(1); // current token type
            switch (type) {
                case KeYSolidityDLLexer.LPAREN -> {
                    // expected "(" <id> ["string"]
                    stream.consume(); // consume the "("
                    Token idToken = stream.LT(1); // element id
                    IProofFileParser.ProofElementID cur =
                        proofSymbolElementId.get(idToken.getText());
                    if (cur == null) {
                        throw new RuntimeException("Unknown proof element at line "
                            + (idToken.getLine() + startLine - 1) + ": " + idToken.getText());
                    }
                    stream.consume();
                    String arg = null;
                    int pos = idToken.getLine() + startLine;
                    if (stream.LA(1) == KeYSolidityDLLexer.STRING_LITERAL) {
                        // argument was given
                        arg = stream.LT(1).getText();
                        arg = unescape(arg.substring(1, arg.length() - 1));
                        stream.consume(); // throw the string away
                    }
                    prl.beginExpr(cur, arg);
                    stack.push(cur);
                    posStack.push(pos);
                }
                case KeYSolidityDLLexer.RPAREN -> {
                    prl.endExpr(stack.pop(), posStack.pop());
                    stream.consume();
                }
                case KeYSolidityDLLexer.EOF -> {
                    return;
                }
                default -> stream.consume();
            }
        }
    }

    private static String unescape(String text) {
        return text.replace("\\\\", "\\").replace("\\\"", "\"");
    }
}
