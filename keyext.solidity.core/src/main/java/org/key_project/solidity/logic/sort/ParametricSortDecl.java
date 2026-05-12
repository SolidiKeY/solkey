/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.logic.sort;

import org.key_project.logic.Name;
import org.key_project.logic.Named;
import org.key_project.logic.sort.Sort;
import org.key_project.solidity.logic.SolidityDLTheory;
import org.key_project.util.collection.ImmutableList;
import org.key_project.util.collection.ImmutableSet;
import org.key_project.util.collection.Immutables;

import org.jspecify.annotations.NonNull;

/// Abstract declaration of a parametric sort, e.g., `List<[E]>`.
///
/// Get instantiated versions using
/// [ParametricSortInstance#get(ParametricSortDecl, ImmutableList, Services)]
public class ParametricSortDecl implements Named {
    private final Name name;
    private final boolean isAbstract;

    private final ImmutableList<GenericParameter> parameters;
    private final ImmutableSet<Sort> extendedSorts;

    public ParametricSortDecl(Name name, boolean isAbstract, ImmutableSet<Sort> ext,
            ImmutableList<GenericParameter> sortParams) {
        this.name = name;
        this.isAbstract = isAbstract;
        this.extendedSorts = ext.isEmpty() ? ImmutableSet.singleton(SolidityDLTheory.ANY) : ext;
        this.parameters = sortParams;
        assert Immutables.isDuplicateFree(parameters)
                : "The caller should have made sure that generic sorts are not duplicated";
    }

    public ImmutableList<GenericParameter> getParameters() {
        return parameters;
    }

    @Override
    public @NonNull Name name() {
        return name;
    }

    public boolean isAbstract() {
        return isAbstract;
    }

    public ImmutableSet<Sort> getExtendedSorts() {
        return extendedSorts;
    }

}
