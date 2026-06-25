/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast;

import java.util.*;
import java.util.Objects;
import java.util.function.Supplier;

import org.key_project.logic.Name;
import org.key_project.logic.op.Function;
import org.key_project.logic.sort.Sort;
import org.key_project.solidity.common.Services;
import org.key_project.solidity.program.ast.abstractions.*;
import org.key_project.solidity.program.ast.declarations.ContractDeclaration;
import org.key_project.solidity.program.ast.declarations.FunctionDeclaration;
import org.key_project.solidity.program.ast.declarations.FunctionEnums.StateMutability;
import org.key_project.solidity.program.ast.declarations.FunctionEnums.Visibility;
import org.key_project.solidity.program.ast.declarations.StateVariableDeclaration;
import org.key_project.solidity.theory.TheoryInfo;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/// The queryable result of reading a Solidity program: declared contracts and
/// their functions, the registered types, and the state variables.
///
/// SolidityInfo stores **final results only**. Partially built
/// [KeYSolid ityType]s (which exist temporarily while cyclic type references
/// are being resolved, see the [KeYSolidityType] class doc) live inside the
/// parsers and may be registered here only once they are complete, this is enforced by
/// [#put(KeYSolidityType)].
public class SolidityInfo {
    private static final Logger LOGGER = LoggerFactory.getLogger(SolidityInfo.class);
    private static final Map<Name, FunctionDeclaration> BUILTIN_FUNCTIONS =
        createBuiltinFunctions();

    /// Solidity AST type → KeYSolidityType (and the same by type name).
    private final Map<Type, KeYSolidityType> typeMap = new HashMap<>();
    private final Map<String, KeYSolidityType> typeByName = new HashMap<>();

    /// Declared contracts in declaration order.
    private final Map<Name, ContractDeclaration> contracts = new LinkedHashMap<>();

    private final LinkedHashSet<StateVariableDeclaration> stateVariables = new LinkedHashSet<>();

    private boolean initialized;

    public SolidityInfo() {
    }

    // ── Initialisation ──────────────────────────────────────────────────────

    /// Registers the predefined (primitive and pseudo) types. Called once by
    /// [Services#initTheories] after the theory LDTs exist.
    ///
    /// @param unresolvedTypes sort-only [KeYSolidityType]s created while
    /// parsing the LDT `.key` files before the theories were available;
    /// each is completed here (the instances are shared with the taclets
    /// that reference them). The list itself is not modified.
    public void initialize(Services services, List<KeYSolidityType> unresolvedTypes) {
        if (initialized) {
            throw new IllegalStateException("SolidityInfo already initialized");
        }
        registerPredefinedTypes(services, unresolvedTypes);
        initialized = true;
    }

    private void registerPredefinedTypes(Services services, List<KeYSolidityType> unresolved) {
        TheoryInfo theoryInfo = services.getTheoryInfo();
        Sort intSort = Objects.requireNonNull(theoryInfo.getIntLDT().targetSort());
        Sort boolSort = Objects.requireNonNull(theoryInfo.getBoolLDT().targetSort());

        for (PrimitiveType primitiveType : PrimitiveType.all()) {
            String name = primitiveType.name().toString();
            if (name.contains("int")) {
                put(new KeYSolidityType(primitiveType, intSort));
            } else if (name.equals("bool")) {
                put(new KeYSolidityType(primitiveType, boolSort));
            } else if (name.equals("address")) {
                put(new KeYSolidityType(primitiveType, intSort));
            } else if (name.equals("string")) {
                put(new KeYSolidityType(primitiveType, intSort));
            } else {
                LOGGER.info("{} not yet supported. Type skipped", name);
            }
        }

        // PSEUDO types do not exist in Solidity, but there are program
        // variables of that 'type' on the logic side (memory, storage, …).
        // Their sorts come from the theories:
        Map<Type, Supplier<Sort>> pseudoSorts = Map.of(
            PseudoType.MEMORY, () -> Objects.requireNonNull(theoryInfo.getMemoryLDT().targetSort()),
            PseudoType.IDENTITY,
            () -> Objects.requireNonNull(theoryInfo.getMemoryLDT().getIdentitySort()),
            PseudoType.STRUCT,
            () -> Objects.requireNonNull(theoryInfo.getStructLDT().targetSort()));

        List<KeYSolidityType> pending = new ArrayList<>(unresolved);
        for (var entry : pseudoSorts.entrySet()) {
            Type pseudoType = entry.getKey();
            Sort sort = entry.getValue().get();
            // An LDT .key file may already have created a sort-only instance
            // for this pseudo type; complete and reuse it so every holder of
            // the instance sees the finished type.
            KeYSolidityType kst = pending.stream()
                    .filter(t -> t.name().equals(pseudoType.name())).findFirst()
                    .orElseGet(() -> new KeYSolidityType(sort));
            kst.setSolidityType(pseudoType);
            put(kst);
            pending.remove(kst);
        }

        if (!pending.isEmpty()) {
            throw new IllegalStateException(
                "The following KeYSolidityTypes could not be resolved: " + pending);
        }
    }

    // ── Types ───────────────────────────────────────────────────────────────

    /// Registers a **complete** KeYSolidityType.
    public void put(KeYSolidityType kst) {
        if (!kst.isComplete()) {
            throw new IllegalArgumentException(
                "SolidityInfo stores only complete results, got " + kst);
        }
        Type st = Objects.requireNonNull(kst.getSolidityType());
        typeByName.put(st.name().toString(), kst);
        typeMap.put(st, kst);
    }

    public @Nullable KeYSolidityType getKeYSolidityType(Type type) {
        return typeMap.get(type);
    }

    public @Nullable KeYSolidityType getKeYSolidityType(String typeName) {
        return typeByName.get(typeName);
    }

    /// Resolves a primitive type name ("uint256", "bool", ...) to its [Type],
    /// or null when the name is not a primitive type.
    public static @Nullable Type getPrimitiveType(String name) {
        try {
            return PrimitiveType.getPrimitiveType(name);
        } catch (NoSuchElementException e) {
            return null;
        }
    }

    // ── Contracts and functions ─────────────────────────────────────────────

    private static Map<Name, FunctionDeclaration> createBuiltinFunctions() {
        FunctionDeclaration revert = new FunctionDeclaration(new Name("revert"), List.of(),
            PrimitiveType.VOID, List.of(), null, "function", Visibility.internal,
            StateMutability.pure, List.of(), "");
        FunctionDeclaration push = new FunctionDeclaration(new Name("push"), List.of(),
            PrimitiveType.VOID, List.of(), null, "function", Visibility.internal,
            StateMutability.nonpayable, List.of(), "");
        FunctionDeclaration pop = new FunctionDeclaration(new Name("pop"), List.of(),
            PrimitiveType.VOID, List.of(), null, "function", Visibility.internal,
            StateMutability.nonpayable, List.of(), "");
        return Map.of(revert.name(), revert, push.name(), push, pop.name(), pop);
    }

    public static @Nullable FunctionDeclaration getBuiltinFunctionDeclaration(Name functionName) {
        return BUILTIN_FUNCTIONS.get(functionName);
    }

    /// Registers a fully parsed contract. Called by the parser after all
    /// references inside the contract are resolved.
    public void registerContract(ContractDeclaration contract) {
        ContractDeclaration previous = contracts.putIfAbsent(contract.name(), contract);
        if (previous != null) {
            throw new IllegalStateException(
                "Contract " + contract.name() + " is already registered");
        }
    }

    /// @return all registered contracts in declaration order
    public Collection<ContractDeclaration> getContracts() {
        return Collections.unmodifiableCollection(contracts.values());
    }

    public @Nullable ContractDeclaration getContract(Name name) {
        return contracts.get(name);
    }

    /// @return the function declarations of the given contract (empty when the
    /// contract is unknown)
    public List<FunctionDeclaration> getFunctions(Name contractName) {
        ContractDeclaration c = contracts.get(contractName);
        return c == null ? List.of() : c.getFunctions();
    }

    /// Finds a function by contract, name, and parameter types (the signature).
    /// Parameter types are compared by type name.
    public @Nullable FunctionDeclaration getFunctionDeclaration(Name contractName,
            Name functionName, List<? extends Type> parameterTypes) {
        for (FunctionDeclaration fd : getFunctions(contractName)) {
            if (fd.name().equals(functionName)
                    && signatureMatches(fd, parameterTypes)) {
                return fd;
            }
        }
        return null;
    }

    /// Finds a function by name (signature is not considered) in declaration
    /// order. The first match is returned otherwise null.
    public @Nullable FunctionDeclaration getFunctionDeclaration(Name functionName) {
        for (ContractDeclaration c : contracts.values()) {
            for (FunctionDeclaration fd : c.getFunctions()) {
                if (fd.name().equals(functionName)) {
                    return fd;
                }
            }
        }
        return getBuiltinFunctionDeclaration(functionName);
    }

    private static boolean signatureMatches(FunctionDeclaration fd,
            List<? extends Type> parameterTypes) {
        var params = fd.getInputParameters();
        if (params.size() != parameterTypes.size()) {
            return false;
        }
        for (int i = 0; i < params.size(); i++) {
            Type expected = parameterTypes.get(i);
            Type actual = params.get(i).getType();
            if (expected == null || actual == null
                    || !expected.name().equals(actual.name())) {
                return false;
            }
        }
        return true;
    }

    /// @return all logic function symbols representing a Solidity function.
    /// Currently none are created during parsing. The program model is
    /// available via [#getContracts] / [#getFunctions].
    public Set<Function> getAllSolidityFunctions() {
        return Set.of();
    }

    // ── State variables ─────────────────────────────────────────────────────

    public void addStateVariable(StateVariableDeclaration stateVariable) {
        Name name = stateVariable.getName();
        if (stateVariables.contains(stateVariable)
                || getStateVariableDeclaration(name) != null) {
            throw new IllegalStateException("State variable " + name + " already exists");
        }
        stateVariables.add(stateVariable);
    }

    public @Nullable StateVariableDeclaration getStateVariableDeclaration(Name name) {
        for (StateVariableDeclaration stateVariable : stateVariables) {
            if (stateVariable.getName().equals(name)) {
                return stateVariable;
            }
        }
        return null;
    }

}
