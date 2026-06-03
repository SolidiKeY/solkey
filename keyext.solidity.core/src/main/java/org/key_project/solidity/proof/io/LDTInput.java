/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.proof.io;


import java.io.File;
import java.util.ArrayList;

import org.key_project.logic.Name;
import org.key_project.solidity.common.Profile;
import org.key_project.solidity.program.ast.abstractions.KeYSolidityType;
import org.key_project.solidity.proof.init.Includes;
import org.key_project.solidity.proof.init.InitConfig;
import org.key_project.solidity.proof.init.ProofInputException;
import org.key_project.solidity.util.parsing.BuildingIssue;
import org.key_project.util.collection.DefaultImmutableSet;
import org.key_project.util.collection.ImmutableSet;

import org.jspecify.annotations.NonNull;


public class LDTInput implements EnvInput {

    private static final Name NAME = new Name("language data types");

    private final KeYFile[] keyFiles;
    private final Profile profile;

    private InitConfig initConfig;

    public LDTInput(KeYFile[] keyFiles, Profile profile) {
        assert profile != null;
        this.keyFiles = keyFiles;
        this.profile = profile;
    }


    @Override
    public int getNumberOfChars() {
        int sum = 0;
        for (KeYFile keyFile : keyFiles) {
            sum = sum + keyFile.getNumberOfChars();
        }
        return sum;
    }

    @Override
    public void setInitConfig(InitConfig initConfig) {
        this.initConfig = initConfig;
        for (KeYFile keyFile : keyFiles) {
            keyFile.setInitConfig(initConfig);
        }
    }

    @Override
    public Includes readIncludes() throws ProofInputException {
        Includes result = new Includes();
        for (KeYFile keyFile : keyFiles) {
            result.putAll(keyFile.readIncludes());
        }
        return result;
    }

    @Override
    public String readSolidityPath() throws ProofInputException {
        return "";
    }

    /// reads all LDTs, i.e., all associated .key files with respect to the given modification
    /// strategy. Reading is done in a special order: first all sort declarations then all function
    /// and predicate declarations and third the rules. This procedure makes it possible to use all
    /// declared sorts in all rules.
    ///
    /// @return a list of warnings during the parsing the process
    @Override
    public ImmutableSet<String> read() {
        var warnings = new ArrayList<String>();
        var unresolvedTypes = new ArrayList<KeYSolidityType>();

        if (initConfig == null) {
            throw new IllegalStateException("LDTInput: InitConfig not set.");
        }

        for (KeYFile keYFile : keyFiles) {
            var pending = keYFile.readSorts();
            warnings.addAll(pending.issues().stream().map(BuildingIssue::message).toList());
            unresolvedTypes.addAll(pending.unresolvedTypes());
        }

        for (KeYFile file : keyFiles) {
            var w = file.readFuncAndPred();
            warnings.addAll(w);
        }
        // create LDT objects to have them available for parsing
        initConfig.getServices().initTheories(unresolvedTypes);

        // TODO read rules once they are added
        for (KeYFile keyFile : keyFiles) {
            keyFile.readRules();
        }

        return DefaultImmutableSet.fromCollection(warnings);
    }

    @Override
    public Profile getProfile() {
        return profile;
    }

    @Override
    public File getInitialFile() {
        return null;
    }

    @Override
    public @NonNull Name name() {
        return NAME;
    }
}
