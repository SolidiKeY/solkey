/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.speclang.natspec;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/// The `@custom:key` clauses of one natspec comment, as solc hands the comment over in the AST's
/// `documentation.text`.
///
/// ```
/// /// @custom:key invariant <expr> contract level, conjoined into CInv
/// /// @custom:key requires <expr> function level, assumed before the call
/// /// @custom:key ensures <expr> function level, proved after the call
/// /// @custom:key assignable <text> accepted and ignored
/// /// @custom:key skip no obligation for this function
/// /// @custom:key box box modality for an unspecified function
/// ```
///
/// A tag starts a line, as in solc's own natspec; a clause runs until the next tag, so lines
/// without one continue the previous clause, and a tag quoted inside a sentence is prose.
public record KeyNatspec(List<Clause> clauses) {

    public static final String TAG = "@custom:key";

    public static final KeyNatspec EMPTY = new KeyNatspec(List.of());

    private static final Pattern TAG_START = Pattern.compile("(?m)^[ \\t]*@[A-Za-z][\\w:-]*");

    public enum Kind {
        BOX, SKIP, INVARIANT, REQUIRES, ENSURES, ASSIGNABLE;

        static Kind of(String word) {
            try {
                return valueOf(word.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                throw new SpecException("unknown " + TAG + " directive '" + word
                    + "'; expected one of box, skip, invariant, requires, ensures, assignable");
            }
        }
    }

    public record Clause(Kind kind, String text) {
    }

    public static KeyNatspec of(String documentation) {
        if (documentation == null || !documentation.contains(TAG)) {
            return EMPTY;
        }
        List<Clause> clauses = new ArrayList<>();
        Matcher matcher = TAG_START.matcher(documentation);
        int tagEnd = -1;
        boolean keyTag = false;
        while (true) {
            boolean found = matcher.find();
            int next = found ? matcher.start() : documentation.length();
            if (keyTag) {
                clauses.add(clause(documentation.substring(tagEnd, next)));
            }
            if (!found) {
                return new KeyNatspec(List.copyOf(clauses));
            }
            keyTag = matcher.group().trim().equals(TAG);
            tagEnd = matcher.end();
        }
    }

    private static Clause clause(String body) {
        String text = body.trim().replaceAll("\\s+", " ");
        int space = text.indexOf(' ');
        String word = space < 0 ? text : text.substring(0, space);
        String rest = space < 0 ? "" : text.substring(space + 1);
        if (word.isEmpty()) {
            throw new SpecException(TAG + " needs a directive: box, skip, invariant, requires,"
                + " ensures or assignable");
        }
        Kind kind = Kind.of(word);
        boolean needsExpression = kind == Kind.INVARIANT || kind == Kind.REQUIRES
                || kind == Kind.ENSURES;
        if (needsExpression && rest.isEmpty()) {
            throw new SpecException(TAG + " " + word + " needs an expression");
        }
        if (!needsExpression && kind != Kind.ASSIGNABLE && !rest.isEmpty()) {
            throw new SpecException(TAG + " " + word + " takes no argument, got '" + rest + "'");
        }
        return new Clause(kind, rest);
    }

    public boolean box() {
        return has(Kind.BOX);
    }

    public boolean skip() {
        return has(Kind.SKIP);
    }

    public List<String> invariants() {
        return texts(Kind.INVARIANT);
    }

    public List<String> requires() {
        return texts(Kind.REQUIRES);
    }

    public List<String> ensures() {
        return texts(Kind.ENSURES);
    }

    /// Whether the comment carries a specification proper: an invariant, a precondition or a
    /// postcondition. `box` and `skip` alone leave the plain obligation in place.
    public boolean isSpecified() {
        return has(Kind.INVARIANT) || has(Kind.REQUIRES) || has(Kind.ENSURES);
    }

    private boolean has(Kind kind) {
        return clauses.stream().anyMatch(c -> c.kind() == kind);
    }

    private List<String> texts(Kind kind) {
        return clauses.stream().filter(c -> c.kind() == kind).map(Clause::text).toList();
    }
}
