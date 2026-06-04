/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.speclang;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

import org.key_project.solidity.common.Profile;
import org.key_project.solidity.proof.init.ProofInputException;
import org.key_project.solidity.proof.io.AbstractEnvInput;
import org.key_project.util.collection.ImmutableSet;

import org.jspecify.annotations.Nullable;

public final class SLEnvInput extends AbstractEnvInput {

    // -------------------------------------------------------------------------
    // constructors
    // -------------------------------------------------------------------------

    public SLEnvInput(Path solidityPath, Profile profile,
            List<Path> includes) {
        super(getLanguage() + " specifications", solidityPath, profile,
            includes);
    }

    // -------------------------------------------------------------------------
    // public interface
    // -------------------------------------------------------------------------

    @Override
    public @Nullable ImmutableSet<String> read() throws ProofInputException {
        if (initConfig == null) {
            throw new IllegalStateException("InitConfig not set.");
        }

        // TODO
        // return createSpecs(new JMLSpecExtractor(initConfig.getServices()));
        return null;
    }

    // -------------------------------------------------------------------------
    // internal methods
    // -------------------------------------------------------------------------

    public static String getLanguage() {
        return "no";
        /*
         * GeneralSettings gs = ProofIndependentSettings.DEFAULT_INSTANCE.getGeneralSettings();
         * if (gs.isUseJML()) {
         * return "JML";
         * } else {
         * return "no";
         * }
         */
    }

    @Override
    public Path getInitialFile() {
        throw new UnsupportedOperationException("Not implemented yet.");
    }

}
