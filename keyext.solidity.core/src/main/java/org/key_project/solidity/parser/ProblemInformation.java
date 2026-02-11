/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.parser;

import org.jspecify.annotations.Nullable;


public class ProblemInformation {
    /// Value of a "\chooseContract". If "\chooseContract" are mentioned in the file, but without a
    /// value, this field is non-null and empty.
    private @Nullable String chooseContract;

    /// Value of a "\proofObligation". If "\proofObligation" are mentioned in the file, but without
    /// a
    /// value, this field is non-null and empty.
    private @Nullable String proofObligation;

    /// Value of a "\profile".
    private @Nullable String profile;

    /// Value of the "\preferences".
    private @Nullable String preferences;

    /// Value of the "\soliditySource".
    private @Nullable String soliditySource;

    ///
    private boolean hasProblemTerm;

    public ProblemInformation() {

    }

    public @Nullable String getChooseContract() {
        return chooseContract;
    }

    public void setChooseContract(@Nullable String chooseContract) {
        this.chooseContract = chooseContract;
    }

    public @Nullable String getProofObligation() {
        return proofObligation;
    }

    public void setProofObligation(@Nullable String proofObligation) {
        this.proofObligation = proofObligation;
    }

    public @Nullable String getProfile() {
        return profile;
    }

    public void setProfile(@Nullable String profile) {
        this.profile = profile;
    }

    public @Nullable String getPreferences() {
        return preferences;
    }

    public void setPreferences(@Nullable String preferences) {
        this.preferences = preferences;
    }

    public @Nullable String getSoliditySource() {
        return soliditySource;
    }

    public void setSoliditySource(@Nullable String soliditySource) {
        this.soliditySource = soliditySource;
    }

    public boolean hasProblemTerm() {
        return hasProblemTerm;
    }

    public void setHasProblemTerm(boolean hasProblemTerm) {
        this.hasProblemTerm = hasProblemTerm;
    }
}
