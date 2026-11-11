/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.testutil;

/// The directories of `keyext.solidity.examples/`, relative to it. Every test names a directory
/// through these, so a move is one edit here; `scripts/examples-dirs.sh` mirrors them for the
/// shell scripts.
public final class ExampleDirs {
    public static final String TACLETS = "taclets";
    public static final String COVERAGE = "coverage";
    public static final String SEMANTICS = "semantics";
    public static final String SEMANTICS_OPEN = SEMANTICS + "/open";
    public static final String CONTRACTS = "contracts";
    public static final String SOLIDIKEY = CONTRACTS + "/solidikey";
    public static final String REAL_WORLD = CONTRACTS + "/real-world";
    public static final String REAL_WORLD_OPEN = REAL_WORLD + "/open";
    public static final String REAL_WORLD_PLAIN = CONTRACTS + "/real-world-plain";
    public static final String REAL_WORLD_PLAIN_OPEN = REAL_WORLD_PLAIN + "/open";
    public static final String REAL_WORLD_PLAIN_SOLIDIKEY = REAL_WORLD_PLAIN + "/solidikey";
    public static final String BENCHMARK = CONTRACTS + "/benchmark";
    public static final String NET = "net";
    public static final String NET_OBLIGATIONS = NET + "/obligations";
    public static final String NEGATIVE = "negative";
    public static final String ILLEGAL = NEGATIVE + "/illegal";
    public static final String UNPROVABLE = NEGATIVE + "/unprovable";

    private ExampleDirs() {}
}
