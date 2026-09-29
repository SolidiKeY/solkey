/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.web;

import org.key_project.solidity.program.parser.SolcCompiler;

import org.graalvm.webimage.api.JS;

final class JsSolcCompiler implements SolcCompiler {

    @Override
    public String compile(String standardJsonInput) {
        return compileInHost(standardJsonInput);
    }

    @Override
    public String version() {
        return versionInHost();
    }

    @JS.Coerce
    @JS(args = { "input" }, value = "return globalThis.solkeySolc.compile(input);")
    private static native String compileInHost(String input);

    @JS.Coerce
    @JS("return globalThis.solkeySolc.version();")
    private static native String versionInHost();
}
