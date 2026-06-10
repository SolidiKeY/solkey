/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.proof.io;

import java.nio.file.Path;
import java.util.List;

import org.key_project.logic.Name;
import org.key_project.solidity.common.Profile;
import org.key_project.solidity.proof.init.Includes;
import org.key_project.solidity.proof.init.InitConfig;
import org.key_project.solidity.proof.init.ProofInputException;

import org.jspecify.annotations.NonNull;

public abstract class AbstractEnvInput implements EnvInput {
    protected final Name name;
    protected final Path solidityPath;
    protected final Includes includes;
    protected final Profile profile;

    protected InitConfig initConfig;
    private Path solidityFile;

    // -------------------------------------------------------------------------
    // constructors
    // -------------------------------------------------------------------------

    protected AbstractEnvInput(String name, Path solidityPath,
            Profile profile, List<Path> includes) {
        assert profile != null;
        this.name = new Name(name);
        this.solidityPath = solidityPath;
        this.profile = profile;
        this.includes = new Includes();
        if (includes != null) {
            for (Path path : includes) {
                this.includes.put(path.toString(), RuleSourceFactory.initRuleFile(path));
            }
        }
    }

    // -------------------------------------------------------------------------
    // public interface
    // -------------------------------------------------------------------------

    @Override
    public final @NonNull Name name() {
        return name;
    }

    @Override
    public final int getNumberOfChars() {
        return 1;
    }

    @Override
    public final void setInitConfig(InitConfig initConfig) {
        this.initConfig = initConfig;
    }


    @Override
    public final Includes readIncludes() throws ProofInputException {
        assert initConfig != null;
        return includes;
    }

    @Override
    public final Path readSolidityPath() throws ProofInputException {
        return solidityPath;
    }

    @Override
    public Profile getProfile() {
        return profile;
    }

    @Override
    public Path getSolidityFile() {
        return solidityFile;
    }

    public void setSolidityFile(Path solidityFile) {
        this.solidityFile = solidityFile;
    }
}
