/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.abstractions;

import java.util.HashMap;
import java.util.Objects;

import org.key_project.logic.Name;
import org.key_project.logic.SyntaxElement;
import org.key_project.logic.sort.Sort;
import org.key_project.solidity.program.ast.Resolver;
import org.key_project.util.ExtList;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/// Pairs a Solidity AST [Type] with its logic-side [Sort].
///
/// ### Life cycle
/// Type graphs can be cyclic (a contract whose function takes the contract
/// itself as a parameter, mutually referencing contracts, …), so a
/// KeYSolidityType sometimes has to exist before the AST type it stands for
/// is fully built. The protocol is:
///
/// 1. The creator allocates the instance with the sort only
/// ([#KeYSolidityType(Sort)]) and hands the *same instance* to everyone
/// who needs the type early.
/// 2. Once the AST type is complete, the creator calls [#setSolidityType]
/// **exactly once** — completing the shared instance for all holders.
/// 3. Only complete instances may be registered in
/// [org.key_project.solidity.program.ast.SolidityInfo].
///
/// [#setSolidityType] rejects a second completion, so an instance can never
/// silently change its meaning.
public class KeYSolidityType implements Type, Resolver {
    /// the AST type
    private @Nullable Type solidityType = null;
    /// the logic sort
    private @Nullable Sort sort = null;
    private int typeId;

    public KeYSolidityType() {
    }

    public KeYSolidityType(Type solidityType, Sort sort) {
        this.solidityType = solidityType;
        this.sort = sort;
    }

    public KeYSolidityType(int typeId) {
        this.typeId = typeId;
    }

    public KeYSolidityType(ExtList children) {
        this.solidityType = Objects.requireNonNull(children.removeFirstOccurrence(Type.class));;
        this.sort = Objects.requireNonNull(children.removeFirstOccurrence(Sort.class));;
    }

    public KeYSolidityType(Type solidityType) {
        this.solidityType = solidityType;
    }

    public KeYSolidityType(Sort sort) {
        this.sort = sort;
    }

    public @Nullable Sort getSort() {
        return sort;
    }

    public void setSort(Sort sort) {
        this.sort = sort;
    }

    public @Nullable Type getSolidityType() {
        return solidityType;
    }

    /// Completes this instance (see the class doc); may be called only once.
    public void setSolidityType(Type solidityType) {
        if (this.solidityType != null && !this.solidityType.equals(solidityType)) {
            throw new IllegalStateException(
                "KeYSolidityType already completed with " + this.solidityType
                    + "; cannot rebind to " + solidityType);
        }
        this.solidityType = solidityType;
    }

    /// @return true iff both the AST type and the sort are set
    public boolean isComplete() {
        return solidityType != null && sort != null;
    }

    @Override
    public @NonNull Name name() {
        return solidityType == null ? Objects.requireNonNull(sort).name() : solidityType.name();
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (o == this)
            return true;
        if (o == null || o.getClass() != this.getClass())
            return false;
        return Objects.equals(solidityType, ((KeYSolidityType) o).solidityType)
                && Objects.equals(sort, ((KeYSolidityType) o).sort);
    }

    @Override
    public @NonNull SyntaxElement getChild(int n) {
        throw new IndexOutOfBoundsException("Index should be 0 <= " + n + " < " + getChildCount());
    }

    @Override
    public int getChildCount() {
        return 0;
    }

    @Override
    public void resolve(HashMap<Integer, SyntaxElement> id2Name) {
        if (solidityType == null)
            solidityType = (Type) id2Name.get(typeId);
    }

    @Override
    public String toString() {
        return "KeYSolidityType(" + solidityType + "," + sort + ")[" + typeId + "]";
    }
}
