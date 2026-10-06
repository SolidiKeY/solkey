/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.parser.varcond;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import org.key_project.logic.op.sv.SchemaVariable;
import org.key_project.logic.sort.Sort;
import org.key_project.prover.rules.VariableCondition;
import org.key_project.solidity.logic.sort.GenericSort;
import org.key_project.solidity.parser.varcond.TypeComparisonCondition.Mode;
import org.key_project.solidity.program.ast.abstractions.KeYSolidityType;
import org.key_project.solidity.rule.sv.OperatorSV;
import org.key_project.solidity.rule.sv.ProgramSV;
import org.key_project.solidity.rule.sv.sort.FieldSVSort;
import org.key_project.solidity.rule.taclets.builder.TacletBuilder;

import org.jspecify.annotations.NonNull;

import static org.key_project.solidity.parser.varcond.ArgumentType.SORT;


/// This class manages the register of various factories for the different built-in
/// [VariableCondition]s.
///
/// @author Alexander Weigl
/// @version 1 (12/9/19)
public class TacletBuilderManipulators {
    // region Factories
    // Shortcut for argument types
    private static final ArgumentType TR = ArgumentType.TYPE_RESOLVER;
    private static final ArgumentType KST = ArgumentType.SOLIDITY_TYPE;
    private static final ArgumentType PV = ArgumentType.VARIABLE;
    private static final ArgumentType USV = ArgumentType.VARIABLE;
    private static final ArgumentType TSV = ArgumentType.VARIABLE;
    private static final ArgumentType ASV = ArgumentType.VARIABLE;
    private static final ArgumentType FSV = ArgumentType.VARIABLE;
    private static final ArgumentType SV = ArgumentType.VARIABLE;
    private static final ArgumentType TLSV = ArgumentType.VARIABLE;
    private static final ArgumentType S = ArgumentType.STRING;
    private static final ArgumentType T = ArgumentType.TERM;

    public static final AbstractConditionBuilder DIFFERENT =
        new ConstructorBasedBuilder("different", DifferentInstantiationCondition.class, SV, SV);

    public static final AbstractConditionBuilder NO_FIXED_ARRAY_ELEMENT =
        new ConstructorBasedBuilder("noFixedArrayElement", NoFixedArrayElementCondition.class,
            SV);

    public static final AbstractConditionBuilder APPLY_UPDATE_ON_RIGID =
        new ConstructorBasedBuilder(
            "applyUpdateOnRigid", ApplyUpdateOnRigidCondition.class, USV, SV, SV);
    public static final AbstractTacletBuilderCommand NEW_DEPENDING_ON =
        new AbstractTacletBuilderCommand("newDependingOn", SV, SV) {
            @Override
            public void apply(TacletBuilder<?> tb, Object[] arguments, List<String> parameters,
                    boolean negated) {
                if (negated) {
                    throw new IllegalArgumentException("Negation is not supported");
                }
                tb.addVarsNewDependingOn((SchemaVariable) arguments[0],
                    (SchemaVariable) arguments[1]);
            }
        };

    static class NoFreeVarInTacletBuilderCommand extends AbstractTacletBuilderCommand {
        public NoFreeVarInTacletBuilderCommand(@NonNull ArgumentType... argumentsTypes) {
            super("noFreeVarIn", argumentsTypes);
        }

        @Override
        public void apply(TacletBuilder<?> tacletBuilder, Object[] arguments,
                List<String> parameters, boolean negated) {
            for (Object argument : arguments) {
                tacletBuilder.addNoFreeVarIn((SchemaVariable) argument);
            }
        }
    }

    public static final AbstractTacletBuilderCommand FREE_1 =
        new NoFreeVarInTacletBuilderCommand(SV);
    public static final AbstractTacletBuilderCommand FREE_2 =
        new NoFreeVarInTacletBuilderCommand(SV, SV);
    public static final AbstractTacletBuilderCommand FREE_3 =
        new NoFreeVarInTacletBuilderCommand(SV, SV, SV);
    public static final AbstractTacletBuilderCommand FREE_4 =
        new NoFreeVarInTacletBuilderCommand(SV, SV, SV, SV);
    public static final AbstractTacletBuilderCommand FREE_5 =
        new NoFreeVarInTacletBuilderCommand(SV, SV, SV, SV, SV);

    public static final AbstractConditionBuilder DROP_EFFECTLESS_ELEMENTARIES =
        new ConstructorBasedBuilder("dropEffectlessElementaries",
            DropEffectlessElementariesCondition.class, USV, SV, SV);
    public static final AbstractConditionBuilder EQUAL_UNIQUE =
        new ConstructorBasedBuilder("equalUnique", EqualUniqueCondition.class, TSV, TSV, FSV);
    public static final AbstractConditionBuilder SIMPLIFY_ITE_UPDATE =
        new ConstructorBasedBuilder("simplifyIfThenElseUpdate",
            SimplifyIfThenElseUpdateCondition.class, FSV, USV, USV, FSV, SV);
    public static final AbstractConditionBuilder GET_INVARIANT =
        new AbstractConditionBuilder("\\getInvariant", PV, PV, SV) {
            @Override
            public VariableCondition build(Object[] arguments, List<String> parameters,
                    boolean negated) {
                return new LoopInvariantCondition((ProgramSV) arguments[0],
                    (ProgramSV) arguments[1], (SchemaVariable) arguments[2], false);
            }
        };
    public static final AbstractConditionBuilder GET_VARIANT =
        new AbstractConditionBuilder("\\getVariant", PV, PV, SV) {
            @Override
            public VariableCondition build(Object[] arguments, List<String> parameters,
                    boolean negated) {
                return new LoopInvariantCondition((ProgramSV) arguments[0],
                    (ProgramSV) arguments[1], (SchemaVariable) arguments[2], true);
            }
        };

    public static AbstractTacletBuilderCommand NEW_TYPE_OF =
        new AbstractTacletBuilderCommand("newTypeOf", SV, SV) {
            @Override
            public void apply(TacletBuilder<?> tacletBuilder, Object[] arguments,
                    List<String> parameters, boolean negated) {
                if (negated) {
                    throw new IllegalArgumentException("Negation is not supported");
                }
                tacletBuilder.addVarsNew((SchemaVariable) arguments[0],
                    (SchemaVariable) arguments[1]);

            }
        };
    public static final AbstractTacletBuilderCommand NEW_SOLIDITY_TYPE =
        new AbstractTacletBuilderCommand("new", SV, KST) {
            @Override
            public void apply(TacletBuilder<?> tacletBuilder, Object[] arguments,
                    List<String> parameters, boolean negated) {
                if (negated) {
                    throw new IllegalArgumentException("Negation is not supported");
                }
                var krt = (KeYSolidityType) arguments[1];
                tacletBuilder.addVarsNew((SchemaVariable) arguments[0], krt);
            }
        };

    public static final AbstractConditionBuilder SAME =
        new AbstractConditionBuilder("same", TR, TR) {
            @Override
            public TypeComparisonCondition build(Object[] arguments, List<String> parameters,
                    boolean negated) {
                return new TypeComparisonCondition((TypeResolver) arguments[0],
                    (TypeResolver) arguments[1],
                    negated ? Mode.NOT_SAME : Mode.SAME);
            }
        };

    public static final AbstractConditionBuilder IS_SUBTYPE =
        new AbstractConditionBuilder("sub", TR, TR) {
            @Override
            public TypeComparisonCondition build(Object[] arguments, List<String> parameters,
                    boolean negated) {
                return new TypeComparisonCondition((TypeResolver) arguments[0],
                    (TypeResolver) arguments[1],
                    negated ? Mode.NOT_IS_SUBTYPE : Mode.IS_SUBTYPE);
            }
        };

    static class SolidityTypeToSortConditionBuilder extends AbstractConditionBuilder {
        public SolidityTypeToSortConditionBuilder(@NonNull String triggerName) {
            super(triggerName, SV, SORT);
        }

        @Override
        public VariableCondition build(Object[] arguments, List<String> parameters,
                boolean negated) {
            var v = (OperatorSV) arguments[0];
            Sort s = (Sort) arguments[1];
            if (!SolidityTypeToSortCondition.checkSortedSV(v)) {
                throw new IllegalArgumentException(
                    "Expected schema variable of kind EXPRESSION or TYPE, but is " + v);
            } else if (s instanceof GenericSort gs) {
                return new SolidityTypeToSortCondition(v, gs);
            } else {
                throw new IllegalArgumentException(
                    "Generic or parametric sort is expected. Got: " + s);
            }
        }
    }

    public static final AbstractConditionBuilder HAS_SORT =
        new SolidityTypeToSortConditionBuilder("hasSort");

    private static final class SortBindingConditionBuilder extends AbstractConditionBuilder {
        private final String keyword;
        private final boolean requireFieldSort;
        private final BiFunction<ProgramSV, GenericSort, VariableCondition> factory;

        SortBindingConditionBuilder(String triggerName, boolean requireFieldSort,
                BiFunction<ProgramSV, GenericSort, VariableCondition> factory) {
            super(triggerName, SV, SORT);
            this.keyword = "\\" + triggerName;
            this.requireFieldSort = requireFieldSort;
            this.factory = factory;
        }

        @Override
        public VariableCondition build(Object[] arguments, List<String> parameters,
                boolean negated) {
            if (negated) {
                throw new IllegalArgumentException(keyword + " does not support negation");
            }
            String expected = keyword + " expects a " + (requireFieldSort ? "field " : "")
                + "program schema variable as its first argument, but got: ";
            if (!(arguments[0] instanceof ProgramSV sv)) {
                throw new IllegalArgumentException(expected + arguments[0]);
            }
            if (requireFieldSort && !(sv.sort() instanceof FieldSVSort)) {
                throw new IllegalArgumentException(expected + sv);
            }
            if (arguments[1] instanceof GenericSort gs) {
                return factory.apply(sv, gs);
            }
            throw new IllegalArgumentException(
                "Generic or parametric sort is expected. Got: " + arguments[1]);
        }
    }

    public static final AbstractConditionBuilder HAS_FIELD_SORT =
        new SortBindingConditionBuilder("hasFieldSort", true,
            (sv, gs) -> new FieldExpressionTypeToSortCondition(sv, gs, false));

    public static final AbstractConditionBuilder HAS_MEMORY_FIELD_SORT =
        new SortBindingConditionBuilder("hasMemoryFieldSort", true,
            (sv, gs) -> new FieldExpressionTypeToSortCondition(sv, gs, true));

    public static final AbstractConditionBuilder HAS_ELEMENT_SORT =
        new SortBindingConditionBuilder("hasElementSort", false,
            (sv, gs) -> new IndexedExpressionTypeToSortCondition(sv, gs, false));

    public static final AbstractConditionBuilder HAS_MEMORY_ELEMENT_SORT =
        new SortBindingConditionBuilder("hasMemoryElementSort", false,
            (sv, gs) -> new IndexedExpressionTypeToSortCondition(sv, gs, true));

    public static final TacletBuilderCommand NEW_LOCAL_VARS =
        new ConstructorBasedBuilder("newLocalVars", NewLocalVarsCondition.class, SV, SV, SV, SV);

    /// `\sameAsTerm(v, s)` — ties a program schema variable `v` to a term schema variable `s`
    /// (`s` equals the logic conversion of `v`). See [SameAsTermCondition].
    public static final AbstractConditionBuilder SAME_AS_TERM =
        new AbstractConditionBuilder("sameAsTerm", SV, SV) {
            @Override
            public VariableCondition build(Object[] arguments, List<String> parameters,
                    boolean negated) {
                if (negated) {
                    throw new IllegalArgumentException(
                        "\\sameAsTerm does not support negation");
                }
                if (!(arguments[0] instanceof ProgramSV programSV)) {
                    throw new IllegalArgumentException(
                        "\\sameAsTerm expects a program schema variable as its first argument, "
                            + "but got: " + arguments[0]);
                }
                return new SameAsTermCondition(programSV, (SchemaVariable) arguments[1]);
            }
        };

    private static final List<TacletBuilderCommand> tacletBuilderCommands = new ArrayList<>(2);

    static {
        register(DIFFERENT, APPLY_UPDATE_ON_RIGID, NEW_DEPENDING_ON,
            FREE_1, FREE_2, FREE_3, FREE_4,
            FREE_5, EQUAL_UNIQUE,
            DROP_EFFECTLESS_ELEMENTARIES, SIMPLIFY_ITE_UPDATE,
            NEW_TYPE_OF, NEW_SOLIDITY_TYPE,
            IS_SUBTYPE, SAME, HAS_SORT, HAS_FIELD_SORT, HAS_MEMORY_FIELD_SORT, HAS_ELEMENT_SORT,
            HAS_MEMORY_ELEMENT_SORT,
            NEW_LOCAL_VARS, GET_INVARIANT, GET_VARIANT, SAME_AS_TERM,
            NO_FIXED_ARRAY_ELEMENT);
    }

    /// Announce a [TacletBuilderCommand] for the use during the interpretation of asts. This
    /// affects every following interpretation of rule contexts in
    /// [TacletPBuilder].
    public static void register(TacletBuilderCommand... cb) {
        for (TacletBuilderCommand a : cb) {
            register(a);
        }
    }

    /// @see #register(TacletBuilderCommand...)
    public static void register(TacletBuilderCommand cb) {
        tacletBuilderCommands.add(cb);
    }


    /// Returns all available [TacletBuilderCommand]s that response on the given name.
    ///
    /// @see TacletBuilderCommand#isSuitableFor(String)
    public static List<TacletBuilderCommand> getConditionBuildersFor(String name) {
        return tacletBuilderCommands.stream().filter(it -> it.isSuitableFor(name))
                .collect(Collectors.toList());
    }
}
