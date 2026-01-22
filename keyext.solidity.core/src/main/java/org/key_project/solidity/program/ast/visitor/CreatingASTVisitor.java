/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.visitor;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;

import org.key_project.solidity.common.Services;
import org.key_project.solidity.program.ast.SolidityProgramElement;
import org.key_project.solidity.program.ast.abstractions.*;
import org.key_project.solidity.program.ast.declarations.*;
import org.key_project.solidity.program.ast.declarations.FunctionEnums.DataLocation;
import org.key_project.solidity.program.ast.expressions.*;
import org.key_project.solidity.program.ast.expressions.literals.*;
import org.key_project.solidity.program.ast.expressions.operators.*;
import org.key_project.solidity.program.ast.references.*;
import org.key_project.solidity.program.ast.statement.*;
import org.key_project.util.ExtList;

public class CreatingASTVisitor extends SolidityASTVisitor {
    protected static final Boolean CHANGED = Boolean.TRUE;
    protected final Deque<ExtList> stack = new ArrayDeque<>();

    boolean preservesPositionInfo = true;

    public CreatingASTVisitor(SolidityProgramElement root, boolean preservesPos,
            Services services) {
        super(root, services);
        this.preservesPositionInfo = preservesPos;
    }

    public boolean preservesPositionInfo() {
        return preservesPositionInfo;
    }

    @Override
    protected void walk(SolidityProgramElement node) {
        ExtList l = new ExtList();
        // l.add(node.getPositionInfo());
        stack.push(l);
        super.walk(node);
    }

    @Override
    public String toString() {
        return getTop().toString();
    }

    @Override
    protected void doDefaultAction(SolidityProgramElement x) {
        addChild(x);
    }

    protected ExtList getTop() {
        return Objects.requireNonNull(stack.peek());
    }

    protected void addToTopOfStack(SolidityProgramElement x) {
        if (x != null) {
            ExtList list = getTop();
            list.add(x);
        }
    }

    protected void addChild(SolidityProgramElement x) {
        stack.pop();
        addToTopOfStack(x);
    }

    protected void changed() {
        ExtList list = getTop();
        if (list.isEmpty() || list.getFirst() != CHANGED) {
            list.addFirst(CHANGED);
        }
    }

    protected abstract class DefaultAction {
        protected final SolidityProgramElement pe;

        protected DefaultAction(SolidityProgramElement pe) {
            this.pe = pe;
        }

        abstract SolidityProgramElement createNewElement(ExtList changeList);

        public void doAction(SolidityProgramElement x) {
            ExtList changeList = stack.peek();
            assert changeList != null;
            if (changeList.isEmpty()) {
                doDefaultAction(x);
                return;
            }
            if (changeList.getFirst() == CHANGED) {
                changeList.removeFirst();
                /*
                 * if (!preservesPositionInfo) {
                 * changeList.removeFirstOccurrence(PositionInfo.class);
                 * }
                 */
                addNewChild(changeList);
            } else {
                doDefaultAction(x);
            }
        }

        protected void addNewChild(ExtList changeList) {
            addChild(createNewElement(changeList));
            changed();
        }
    }

    @Override
    public void performActionOnDataLocation(DataLocation x) {
        DefaultAction def = new DefaultAction(x) {
            @Override
            SolidityProgramElement createNewElement(ExtList changeList) {
                return x;
            }
        };
        def.doAction(x);
    }

    @Override
    public void performActionOnArrayType(ArrayType x) {
        DefaultAction def = new DefaultAction(x) {
            @Override
            SolidityProgramElement createNewElement(ExtList changeList) {
                return new ArrayType(changeList);
            }
        };
        def.doAction(x);
    }

    @Override
    public void performActionOnEnumType(EnumType x) {
        DefaultAction def = new DefaultAction(x) {
            @Override
            SolidityProgramElement createNewElement(ExtList changeList) {
                return new EnumType(changeList);
            }
        };
        def.doAction(x);
    }

    @Override
    public void performActionOnKeYSolidityType(KeYSolidityType x) {
        DefaultAction def = new DefaultAction(x) {
            @Override
            SolidityProgramElement createNewElement(ExtList changeList) {
                return new KeYSolidityType(changeList);
            }
        };
        def.doAction(x);
    }

    @Override
    public void performActionOnMappingType(MappingType x) {
        DefaultAction def = new DefaultAction(x) {
            @Override
            SolidityProgramElement createNewElement(ExtList changeList) {
                return new MappingType(changeList);
            }
        };
        def.doAction(x);
    }

    @Override
    public void performActionOnPrimitiveType(PrimitiveType x) {
        DefaultAction def = new DefaultAction(x) {
            @Override
            SolidityProgramElement createNewElement(ExtList changeList) {
                return new PrimitiveType(changeList);
            }
        };
        def.doAction(x);
    }

    @Override
    public void performActionOnArrayDeclaration(ArrayDeclaration x) {
        DefaultAction def = new DefaultAction(x) {
            @Override
            SolidityProgramElement createNewElement(ExtList changeList) {
                return new ArrayDeclaration(changeList, x.getLength());
            }
        };
        def.doAction(x);
    }

    @Override
    public void performActionOnContractDeclaration(ContractDeclaration x) {
        DefaultAction def = new DefaultAction(x) {
            @Override
            SolidityProgramElement createNewElement(ExtList changeList) {
                return new ContractDeclaration(changeList);
            }
        };
        def.doAction(x);
    }

    @Override
    public void performActionOnEnumDeclaration(EnumDeclaration x) {
        DefaultAction def = new DefaultAction(x) {
            @Override
            SolidityProgramElement createNewElement(ExtList changeList) {
                return new EnumDeclaration(changeList);
            }
        };
        def.doAction(x);
    }

    @Override
    public void performActionOnFieldDeclaration(FieldDeclaration x) {
        DefaultAction def = new DefaultAction(x) {
            @Override
            SolidityProgramElement createNewElement(ExtList changeList) {
                return new FieldDeclaration(changeList);
            }
        };
        def.doAction(x);
    }

    @Override
    public void performActionOnFunctionDeclaration(FunctionDeclaration x) {
        DefaultAction def = new DefaultAction(x) {
            @Override
            SolidityProgramElement createNewElement(ExtList changeList) {
                return new FunctionDeclaration(changeList);
            }
        };
        def.doAction(x);
    }

    @Override
    public void performActionOnMemberEnumDeclaration(MemberEnumDeclaration x) {
        DefaultAction def = new DefaultAction(x) {
            @Override
            SolidityProgramElement createNewElement(ExtList changeList) {
                return new MemberEnumDeclaration(changeList);
            }
        };
        def.doAction(x);
    }

    @Override
    public void performActionOnModifierDeclaration(ModifierDeclaration x) {
        DefaultAction def = new DefaultAction(x) {
            @Override
            SolidityProgramElement createNewElement(ExtList changeList) {
                return new ModifierDeclaration(changeList);
            }
        };
        def.doAction(x);
    }

    @Override
    public void performActionOnParameterDeclaration(ParameterDeclaration x) {
        DefaultAction def = new DefaultAction(x) {
            @Override
            SolidityProgramElement createNewElement(ExtList changeList) {
                return new ParameterDeclaration(changeList);
            }
        };
        def.doAction(x);
    }

    @Override
    public void performActionOnStateVariableDeclaration(StateVariableDeclaration x) {
        DefaultAction def = new DefaultAction(x) {
            @Override
            SolidityProgramElement createNewElement(ExtList changeList) {
                return new StateVariableDeclaration(changeList);
            }
        };
        def.doAction(x);
    }

    @Override
    public void performActionOnStatementVariableDeclaration(StatementVariableDeclaration x) {
        DefaultAction def = new DefaultAction(x) {
            @Override
            SolidityProgramElement createNewElement(ExtList changeList) {
                return new StatementVariableDeclaration(changeList);
            }
        };
        def.doAction(x);
    }

    @Override
    public void performActionOnStructDeclaration(StructDeclaration x) {
        DefaultAction def = new DefaultAction(x) {
            @Override
            SolidityProgramElement createNewElement(ExtList changeList) {
                return new StructDeclaration(changeList);
            }
        };
        def.doAction(x);
    }

    @Override
    public void performActionOnElementaryExpression(ElementaryExpression x) {
        DefaultAction def = new DefaultAction(x) {
            @Override
            SolidityProgramElement createNewElement(ExtList changeList) {
                return new ElementaryExpression(changeList);
            }
        };
        def.doAction(x);
    }

    @Override
    public void performActionOnFunctionCallExpression(FunctionCallExpression x) {
        DefaultAction def = new DefaultAction(x) {
            @Override
            SolidityProgramElement createNewElement(ExtList changeList) {
                return new FunctionCallExpression(changeList);
            }
        };
        def.doAction(x);
    }

    @Override
    public void performActionOnIndexExpression(IndexExpression x) {
        DefaultAction def = new DefaultAction(x) {
            @Override
            SolidityProgramElement createNewElement(ExtList changeList) {
                return new IndexExpression(changeList);
            }
        };
        def.doAction(x);
    }

    @Override
    public void performActionOnIndexRangeExpression(IndexRangeExpression x) {
        DefaultAction def = new DefaultAction(x) {
            @Override
            SolidityProgramElement createNewElement(ExtList changeList) {
                return new IndexRangeExpression(changeList);
            }
        };
        def.doAction(x);
    }

    @Override
    public void performActionOnMemberExp(MemberExp x) {
        DefaultAction def = new DefaultAction(x) {
            @Override
            SolidityProgramElement createNewElement(ExtList changeList) {
                return new MemberExp(changeList);
            }
        };
        def.doAction(x);
    }

    @Override
    public void performActionOnTupleExpression(TupleExpression x) {
        DefaultAction def = new DefaultAction(x) {
            @Override
            SolidityProgramElement createNewElement(ExtList changeList) {
                return new TupleExpression(changeList);
            }
        };
        def.doAction(x);
    }

    @Override
    public void performActionOnUnresolvedTypeException(UnresolvedTypeException x) {
        DefaultAction def = new DefaultAction(x) {
            @Override
            SolidityProgramElement createNewElement(ExtList changeList) {
                return new UnresolvedTypeException(changeList);
            }
        };
        def.doAction(x);
    }

    @Override
    public void performActionOnBoolLiteral(BoolLiteral x) {
        DefaultAction def = new DefaultAction(x) {
            @Override
            SolidityProgramElement createNewElement(ExtList changeList) {
                return new BoolLiteral(changeList);
            }
        };
        def.doAction(x);
    }

    @Override
    public void performActionOnUint256Literal(Uint256Literal x) {
        DefaultAction def = new DefaultAction(x) {
            @Override
            SolidityProgramElement createNewElement(ExtList changeList) {
                return new Uint256Literal(changeList);
            }
        };
        def.doAction(x);
    }

    @Override
    public void performActionOnAddOperator(AddOperator x) {
        DefaultAction def = new DefaultAction(x) {
            @Override
            SolidityProgramElement createNewElement(ExtList changeList) {
                return new AddOperator(changeList, x.getType());
            }
        };
        def.doAction(x);
    }

    @Override
    public void performActionOnAndEqualOperator(AndEqualOperator x) {
        DefaultAction def = new DefaultAction(x) {
            @Override
            SolidityProgramElement createNewElement(ExtList changeList) {
                return new AndEqualOperator(changeList, x.getType());
            }
        };
        def.doAction(x);
    }

    @Override
    public void performActionOnAndOperator(AndOperator x) {
        DefaultAction def = new DefaultAction(x) {
            @Override
            SolidityProgramElement createNewElement(ExtList changeList) {
                return new AndOperator(changeList, x.getType());
            }
        };
        def.doAction(x);
    }

    @Override
    public void performActionOnAssignmentExpression(AssignmentExpression x) {
        DefaultAction def = new DefaultAction(x) {
            @Override
            SolidityProgramElement createNewElement(ExtList changeList) {
                return new AssignmentExpression(changeList, x.getType());
            }
        };
        def.doAction(x);
    }

    @Override
    public void performActionOnBitwiseAndOperator(BitwiseAndOperator x) {
        DefaultAction def = new DefaultAction(x) {
            @Override
            SolidityProgramElement createNewElement(ExtList changeList) {
                return new BitwiseAndOperator(changeList, x.getType());
            }
        };
        def.doAction(x);
    }

    @Override
    public void performActionOnBitwiseEqualOperator(BitwiseEqualOperator x) {
        DefaultAction def = new DefaultAction(x) {
            @Override
            SolidityProgramElement createNewElement(ExtList changeList) {
                return new BitwiseEqualOperator(changeList, x.getType());
            }
        };
        def.doAction(x);
    }

    @Override
    public void performActionOnBitwiseNotOperator(BitwiseNotOperator x) {
        DefaultAction def = new DefaultAction(x) {
            @Override
            SolidityProgramElement createNewElement(ExtList changeList) {
                return new BitwiseNotOperator(changeList, x.getType());
            }
        };
        def.doAction(x);
    }

    @Override
    public void performActionOnBitwiseOrOperator(BitwiseOrOperator x) {
        DefaultAction def = new DefaultAction(x) {
            @Override
            SolidityProgramElement createNewElement(ExtList changeList) {
                return new BitwiseOrOperator(changeList, x.getType());
            }
        };
        def.doAction(x);
    }

    @Override
    public void performActionOnDeleteOperator(DeleteOperator x) {
        DefaultAction def = new DefaultAction(x) {
            @Override
            SolidityProgramElement createNewElement(ExtList changeList) {
                return new DeleteOperator(changeList, x.getType());
            }
        };
        def.doAction(x);
    }

    @Override
    public void performActionOnDivOperator(DivOperator x) {
        DefaultAction def = new DefaultAction(x) {
            @Override
            SolidityProgramElement createNewElement(ExtList changeList) {
                return new DivOperator(changeList, x.getType());
            }
        };
        def.doAction(x);
    }

    @Override
    public void performActionOnDivisionEqualOperator(DivisionEqualOperator x) {
        DefaultAction def = new DefaultAction(x) {
            @Override
            SolidityProgramElement createNewElement(ExtList changeList) {
                return new DivisionEqualOperator(changeList, x.getType());
            }
        };
        def.doAction(x);
    }

    @Override
    public void performActionOnEqualOperator(EqualOperator x) {
        DefaultAction def = new DefaultAction(x) {
            @Override
            SolidityProgramElement createNewElement(ExtList changeList) {
                return new EqualOperator(changeList, x.getType());
            }
        };
        def.doAction(x);
    }

    @Override
    public void performActionOnExponentialOperator(ExponentialOperator x) {
        DefaultAction def = new DefaultAction(x) {
            @Override
            SolidityProgramElement createNewElement(ExtList changeList) {
                return new ExponentialOperator(changeList, x.getType());
            }
        };
        def.doAction(x);
    }

    @Override
    public void performActionOnGreaterEqualOperator(GreaterEqualOperator x) {
        DefaultAction def = new DefaultAction(x) {
            @Override
            SolidityProgramElement createNewElement(ExtList changeList) {
                return new GreaterEqualOperator(changeList, x.getType());
            }
        };
        def.doAction(x);
    }

    @Override
    public void performActionOnGreaterOperator(GreaterOperator x) {
        DefaultAction def = new DefaultAction(x) {
            @Override
            SolidityProgramElement createNewElement(ExtList changeList) {
                return new GreaterOperator(changeList, x.getType());
            }
        };
        def.doAction(x);
    }

    @Override
    public void performActionOnLeftShiftEqualOperator(LeftShiftEqualOperator x) {
        DefaultAction def = new DefaultAction(x) {
            @Override
            SolidityProgramElement createNewElement(ExtList changeList) {
                return new LeftShiftEqualOperator(changeList, x.getType());
            }
        };
        def.doAction(x);
    }

    @Override
    public void performActionOnLeftShiftOperator(LeftShiftOperator x) {
        DefaultAction def = new DefaultAction(x) {
            @Override
            SolidityProgramElement createNewElement(ExtList changeList) {
                return new LeftShiftOperator(changeList, x.getType());
            }
        };
        def.doAction(x);
    }

    @Override
    public void performActionOnLessEqualOperator(LessEqualOperator x) {
        DefaultAction def = new DefaultAction(x) {
            @Override
            SolidityProgramElement createNewElement(ExtList changeList) {
                return new LessEqualOperator(changeList, x.getType());
            }
        };
        def.doAction(x);
    }

    @Override
    public void performActionOnLessOperator(LessOperator x) {
        DefaultAction def = new DefaultAction(x) {
            @Override
            SolidityProgramElement createNewElement(ExtList changeList) {
                return new LessOperator(changeList, x.getType());
            }
        };
        def.doAction(x);
    }

    @Override
    public void performActionOnLogicalRightShiftEqualOperator(LogicalRightShiftEqualOperator x) {
        DefaultAction def = new DefaultAction(x) {
            @Override
            SolidityProgramElement createNewElement(ExtList changeList) {
                return new LogicalRightShiftEqualOperator(changeList, x.getType());
            }
        };
        def.doAction(x);
    }

    @Override
    public void performActionOnLogicalRightShiftOperator(LogicalRightShiftOperator x) {
        DefaultAction def = new DefaultAction(x) {
            @Override
            SolidityProgramElement createNewElement(ExtList changeList) {
                return new LogicalRightShiftOperator(changeList, x.getType());
            }
        };
        def.doAction(x);
    }

    @Override
    public void performActionOnMinusEqualOperator(MinusEqualOperator x) {
        DefaultAction def = new DefaultAction(x) {
            @Override
            SolidityProgramElement createNewElement(ExtList changeList) {
                return new MinusEqualOperator(changeList, x.getType());
            }
        };
        def.doAction(x);
    }

    @Override
    public void performActionOnMinusMinusOperator(MinusMinusOperator x) {
        DefaultAction def = new DefaultAction(x) {
            @Override
            SolidityProgramElement createNewElement(ExtList changeList) {
                return new MinusMinusOperator(changeList, x.getType());
            }
        };
        def.doAction(x);
    }

    @Override
    public void performActionOnModEqualOperator(ModEqualOperator x) {
        DefaultAction def = new DefaultAction(x) {
            @Override
            SolidityProgramElement createNewElement(ExtList changeList) {
                return new ModEqualOperator(changeList, x.getType());
            }
        };
        def.doAction(x);
    }

    @Override
    public void performActionOnModOperator(ModOperator x) {
        DefaultAction def = new DefaultAction(x) {
            @Override
            SolidityProgramElement createNewElement(ExtList changeList) {
                return new ModOperator(changeList, x.getType());
            }
        };
        def.doAction(x);
    }

    @Override
    public void performActionOnMultiplicationEqualOperator(MultiplicationEqualOperator x) {
        DefaultAction def = new DefaultAction(x) {
            @Override
            SolidityProgramElement createNewElement(ExtList changeList) {
                return new MultiplicationEqualOperator(changeList, x.getType());
            }
        };
        def.doAction(x);
    }

    @Override
    public void performActionOnMultiplicationOperator(MultiplicationOperator x) {
        DefaultAction def = new DefaultAction(x) {
            @Override
            SolidityProgramElement createNewElement(ExtList changeList) {
                return new MultiplicationOperator(changeList, x.getType());
            }
        };
        def.doAction(x);
    }

    @Override
    public void performActionOnNegateOperator(NegateOperator x) {
        DefaultAction def = new DefaultAction(x) {
            @Override
            SolidityProgramElement createNewElement(ExtList changeList) {
                return new NegateOperator(changeList, x.getType());
            }
        };
        def.doAction(x);
    }

    @Override
    public void performActionOnNotOperator(NotOperator x) {
        DefaultAction def = new DefaultAction(x) {
            @Override
            SolidityProgramElement createNewElement(ExtList changeList) {
                return new NotOperator(changeList, x.getType());
            }
        };
        def.doAction(x);
    }

    @Override
    public void performActionOnOrEqualOperator(OrEqualOperator x) {
        DefaultAction def = new DefaultAction(x) {
            @Override
            SolidityProgramElement createNewElement(ExtList changeList) {
                return new OrEqualOperator(changeList, x.getType());
            }
        };
        def.doAction(x);
    }

    @Override
    public void performActionOnOrOperator(OrOperator x) {
        DefaultAction def = new DefaultAction(x) {
            @Override
            SolidityProgramElement createNewElement(ExtList changeList) {
                return new OrOperator(changeList, x.getType());
            }
        };
        def.doAction(x);
    }

    @Override
    public void performActionOnPlusEqualOperator(PlusEqualOperator x) {
        DefaultAction def = new DefaultAction(x) {
            @Override
            SolidityProgramElement createNewElement(ExtList changeList) {
                return new PlusEqualOperator(changeList, x.getType());
            }
        };
        def.doAction(x);
    }

    @Override
    public void performActionOnPlusPlusOperator(PlusPlusOperator x) {
        DefaultAction def = new DefaultAction(x) {
            @Override
            SolidityProgramElement createNewElement(ExtList changeList) {
                return new PlusPlusOperator(changeList, x.getType());
            }
        };
        def.doAction(x);
    }

    @Override
    public void performActionOnRightShiftEqualOperator(RightShiftEqualOperator x) {
        DefaultAction def = new DefaultAction(x) {
            @Override
            SolidityProgramElement createNewElement(ExtList changeList) {
                return new RightShiftEqualOperator(changeList, x.getType());
            }
        };
        def.doAction(x);
    }

    @Override
    public void performActionOnRightShiftOperator(RightShiftOperator x) {
        DefaultAction def = new DefaultAction(x) {
            @Override
            SolidityProgramElement createNewElement(ExtList changeList) {
                return new RightShiftOperator(changeList, x.getType());
            }
        };
        def.doAction(x);
    }

    @Override
    public void performActionOnSubtractionOperator(SubtractionOperator x) {
        DefaultAction def = new DefaultAction(x) {
            @Override
            SolidityProgramElement createNewElement(ExtList changeList) {
                return new SubtractionOperator(changeList, x.getType());
            }
        };
        def.doAction(x);
    }

    @Override
    public void performActionOnTernaryOperator(TernaryOperator x) {
        DefaultAction def = new DefaultAction(x) {
            @Override
            SolidityProgramElement createNewElement(ExtList changeList) {
                return new TernaryOperator(changeList, x.getType());
            }
        };
        def.doAction(x);
    }

    @Override
    public void performActionOnUnequalOperator(UnequalOperator x) {
        DefaultAction def = new DefaultAction(x) {
            @Override
            SolidityProgramElement createNewElement(ExtList changeList) {
                return new UnequalOperator(changeList, x.getType());
            }
        };
        def.doAction(x);
    }

    @Override
    public void performActionOnXorEqualOperator(XorEqualOperator x) {
        DefaultAction def = new DefaultAction(x) {
            @Override
            SolidityProgramElement createNewElement(ExtList changeList) {
                return new XorEqualOperator(changeList, x.getType());
            }
        };
        def.doAction(x);
    }

    @Override
    public void performActionOnContractReference(ContractReference x) {
        DefaultAction def = new DefaultAction(x) {
            @Override
            SolidityProgramElement createNewElement(ExtList changeList) {
                return new ContractReference(changeList, x.getType(), x.id, x.name);
            }
        };
        def.doAction(x);
    }

    @Override
    public void performActionOnEnumReference(EnumReference x) {
        DefaultAction def = new DefaultAction(x) {
            @Override
            SolidityProgramElement createNewElement(ExtList changeList) {
                return new EnumReference(changeList, x.getType());
            }
        };
        def.doAction(x);
    }

    @Override
    public void performActionOnFunctionReference(FunctionReference x) {
        DefaultAction def = new DefaultAction(x) {
            @Override
            SolidityProgramElement createNewElement(ExtList changeList) {
                return new FunctionReference(changeList, x.getType(), x.id, x.name);
            }
        };
        def.doAction(x);
    }

    @Override
    public void performActionOnModifierReference(ModifierReference x) {
        DefaultAction def = new DefaultAction(x) {
            @Override
            SolidityProgramElement createNewElement(ExtList changeList) {
                return new ModifierReference(x.name);
            }
        };
        def.doAction(x);
    }

    @Override
    public void performActionOnParameterVariableReference(ParameterVariableReference x) {
        DefaultAction def = new DefaultAction(x) {
            @Override
            SolidityProgramElement createNewElement(ExtList changeList) {
                return new ParameterVariableReference(changeList, x.getType(), x.name);
            }
        };
        def.doAction(x);
    }

    @Override
    public void performActionOnStateVariableReference(StateVariableReference x) {
        DefaultAction def = new DefaultAction(x) {
            @Override
            SolidityProgramElement createNewElement(ExtList changeList) {
                return new StateVariableReference(changeList, x.getType(), x.name);
            }
        };
        def.doAction(x);
    }

    @Override
    public void performActionOnTypeReference(TypeReference x) {
        DefaultAction def = new DefaultAction(x) {
            @Override
            SolidityProgramElement createNewElement(ExtList changeList) {
                return new TypeReference(x.referencedType);
            }
        };
        def.doAction(x);
    }

    @Override
    public void performActionOnUnresolvedReferenceException(UnresolvedReferenceException x) {
        DefaultAction def = new DefaultAction(x) {
            @Override
            SolidityProgramElement createNewElement(ExtList changeList) {
                return new UnresolvedReferenceException(changeList);
            }
        };
        def.doAction(x);
    }

    @Override
    public void performActionOnBlock(Block x) {
        DefaultAction def = new DefaultAction(x) {
            @Override
            SolidityProgramElement createNewElement(ExtList changeList) {
                return new Block(changeList);
            }
        };
        def.doAction(x);
    }

    @Override
    public void performActionOnBreakStatement(BreakStatement x) {
        DefaultAction def = new DefaultAction(x) {
            @Override
            SolidityProgramElement createNewElement(ExtList changeList) {
                return new BreakStatement();
            }
        };
        def.doAction(x);
    }

    @Override
    public void performActionOnConditionStatement(ConditionStatement x) {
        DefaultAction def = new DefaultAction(x) {
            @Override
            SolidityProgramElement createNewElement(ExtList changeList) {
                return new ConditionStatement(changeList);
            }
        };
        def.doAction(x);
    }

    @Override
    public void performActionOnContinueStatement(ContinueStatement x) {
        DefaultAction def = new DefaultAction(x) {
            @Override
            SolidityProgramElement createNewElement(ExtList changeList) {
                return new ContinueStatement();
            }
        };
        def.doAction(x);
    }

    @Override
    public void performActionOnDeclarationStatement(DeclarationStatement x) {
        DefaultAction def = new DefaultAction(x) {
            @Override
            SolidityProgramElement createNewElement(ExtList changeList) {
                return new DeclarationStatement(changeList);
            }
        };
        def.doAction(x);
    }

    @Override
    public void performActionOnDoWhileStatement(DoWhileStatement x) {
        DefaultAction def = new DefaultAction(x) {
            @Override
            SolidityProgramElement createNewElement(ExtList changeList) {
                return new DoWhileStatement(changeList);
            }
        };
        def.doAction(x);
    }

    @Override
    public void performActionOnExpressionStatement(ExpressionStatement x) {
        DefaultAction def = new DefaultAction(x) {
            @Override
            SolidityProgramElement createNewElement(ExtList changeList) {
                return new ExpressionStatement(changeList);
            }
        };
        def.doAction(x);
    }

    @Override
    public void performActionOnForStatement(ForStatement x) {
        DefaultAction def = new DefaultAction(x) {
            @Override
            SolidityProgramElement createNewElement(ExtList changeList) {
                return new ForStatement(changeList);
            }
        };
        def.doAction(x);
    }

    @Override
    public void performActionOnPlaceholdStatement(PlaceholdStatement x) {
        DefaultAction def = new DefaultAction(x) {
            @Override
            SolidityProgramElement createNewElement(ExtList changeList) {
                return new PlaceholdStatement();
            }
        };
        def.doAction(x);
    }

    @Override
    public void performActionOnReturnStatment(ReturnStatment x) {
        DefaultAction def = new DefaultAction(x) {
            @Override
            SolidityProgramElement createNewElement(ExtList changeList) {
                return new ReturnStatment(changeList);
            }
        };
        def.doAction(x);
    }

    @Override
    public void performActionOnTryStatement(TryStatement x) {
        DefaultAction def = new DefaultAction(x) {
            @Override
            SolidityProgramElement createNewElement(ExtList changeList) {
                return new TryStatement(changeList);
            }
        };
        def.doAction(x);
    }

    @Override
    public void performActionOnWhileStatement(WhileStatement x) {
        DefaultAction def = new DefaultAction(x) {
            @Override
            SolidityProgramElement createNewElement(ExtList changeList) {
                return new WhileStatement(changeList);
            }
        };
        def.doAction(x);
    }
}
