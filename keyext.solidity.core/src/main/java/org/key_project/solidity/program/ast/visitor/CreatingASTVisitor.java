package org.key_project.solidity.program.ast.visitor;

import org.key_project.solidity.common.Services;
import org.key_project.solidity.program.ast.SolidityProgramElement;
import org.key_project.solidity.program.ast.abstractions.*;
import org.key_project.solidity.program.ast.declarations.*;
import org.key_project.solidity.program.ast.expressions.*;
import org.key_project.solidity.program.ast.expressions.literals.*;
import org.key_project.solidity.program.ast.expressions.operators.*;
import org.key_project.solidity.program.ast.references.*;
import org.key_project.solidity.program.ast.statement.*;
import org.key_project.util.ExtList;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;

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
    public void performActionOnAddOperator(AddOperator x) {
        DefaultAction def = new DefaultAction(x) {
            @Override
            SolidityProgramElement createNewElement(ExtList changeList) {
                return new AddOperator(changeList);
            }
        };
        def.doAction(x);
    }

    @Override
    public void performActionOnAndEqualOperator(AndEqualOperator x) {
        DefaultAction def = new DefaultAction(x) {
            @Override
            SolidityProgramElement createNewElement(ExtList changeList) {
                return new AndEqualOperator(changeList);
            }
        };
        def.doAction(x);
    }

    @Override
    public void performActionOnAndOperator(AndOperator x) {
        DefaultAction def = new DefaultAction(x) {
            @Override
            SolidityProgramElement createNewElement(ExtList changeList) {
                return new AndOperator(changeList);
            }
        };
        def.doAction(x);
    }

    @Override
    public void performActionOnAssignmentExpression(AssignmentExpression x) {
        DefaultAction def = new DefaultAction(x) {
            @Override
            SolidityProgramElement createNewElement(ExtList changeList) {
                return new AssignmentExpression(changeList);
            }
        };
        def.doAction(x);
    }

    @Override
    public void performActionOnBitwiseAndOperator(BitwiseAndOperator x) {
        DefaultAction def = new DefaultAction(x) {
            @Override
            SolidityProgramElement createNewElement(ExtList changeList) {
                return new BitwiseAndOperator(changeList);
            }
        };
        def.doAction(x);
    }

    @Override
    public void performActionOnBitwiseEqualOperator(BitwiseEqualOperator x) {
        DefaultAction def = new DefaultAction(x) {
            @Override
            SolidityProgramElement createNewElement(ExtList changeList) {
                return new BitwiseEqualOperator(changeList);
            }
        };
        def.doAction(x);
    }

    @Override
    public void performActionOnBitwiseNotOperator(BitwiseNotOperator x) {
        DefaultAction def = new DefaultAction(x) {
            @Override
            SolidityProgramElement createNewElement(ExtList changeList) {
                return new BitwiseNotOperator(changeList);
            }
        };
        def.doAction(x);
    }

    @Override
    public void performActionOnBitwiseOrOperator(BitwiseOrOperator x) {
        DefaultAction def = new DefaultAction(x) {
            @Override
            SolidityProgramElement createNewElement(ExtList changeList) {
                return new BitwiseOrOperator(changeList);
            }
        };
        def.doAction(x);
    }

    @Override
    public void performActionOnDeleteOperator(DeleteOperator x) {
        DefaultAction def = new DefaultAction(x) {
            @Override
            SolidityProgramElement createNewElement(ExtList changeList) {
                return new DeleteOperator(changeList);
            }
        };
        def.doAction(x);
    }

    @Override
    public void performActionOnDivisionEqualOperator(DivisionEqualOperator x) {
        DefaultAction def = new DefaultAction(x) {
            @Override
            SolidityProgramElement createNewElement(ExtList changeList) {
                return new DivisionEqualOperator(changeList);
            }
        };
        def.doAction(x);
    }

    @Override
    public void performActionOnDivOperator(DivOperator x) {
        DefaultAction def = new DefaultAction(x) {
            @Override
            SolidityProgramElement createNewElement(ExtList changeList) {
                return new DivOperator(changeList);
            }
        };
        def.doAction(x);
    }

    @Override
    public void performActionOnEqualOperator(EqualOperator x) {
        DefaultAction def = new DefaultAction(x) {
            @Override
            SolidityProgramElement createNewElement(ExtList changeList) {
                return new EqualOperator(changeList);
            }
        };
        def.doAction(x);
    }

    @Override
    public void performActionOnExponentialOperator(ExponentialOperator x) {
        DefaultAction def = new DefaultAction(x) {
            @Override
            SolidityProgramElement createNewElement(ExtList changeList) {
                return new ExponentialOperator(changeList);
            }
        };
        def.doAction(x);
    }

    @Override
    public void performActionOnGreaterEqualOperator(GreaterEqualOperator x) {
        DefaultAction def = new DefaultAction(x) {
            @Override
            SolidityProgramElement createNewElement(ExtList changeList) {
                return new GreaterEqualOperator(changeList);
            }
        };
        def.doAction(x);
    }

    @Override
    public void performActionOnGreaterOperator(GreaterOperator x) {
        DefaultAction def = new DefaultAction(x) {
            @Override
            SolidityProgramElement createNewElement(ExtList changeList) {
                return new GreaterOperator(changeList);
            }
        };
        def.doAction(x);
    }

    @Override
    public void performActionOnLeftShiftEqualOperator(LeftShiftEqualOperator x) {
        DefaultAction def = new DefaultAction(x) {
            @Override
            SolidityProgramElement createNewElement(ExtList changeList) {
                return new LeftShiftEqualOperator(changeList);
            }
        };
        def.doAction(x);
    }

    @Override
    public void performActionOnLeftShiftOperator(LeftShiftOperator x) {
        DefaultAction def = new DefaultAction(x) {
            @Override
            SolidityProgramElement createNewElement(ExtList changeList) {
                return new LeftShiftOperator(changeList);
            }
        };
        def.doAction(x);
    }

    @Override
    public void performActionOnLessEqualOperator(LessEqualOperator x) {
        DefaultAction def = new DefaultAction(x) {
            @Override
            SolidityProgramElement createNewElement(ExtList changeList) {
                return new LessEqualOperator(changeList);
            }
        };
        def.doAction(x);
    }

    @Override
    public void performActionOnLessOperator(LessOperator x) {
        DefaultAction def = new DefaultAction(x) {
            @Override
            SolidityProgramElement createNewElement(ExtList changeList) {
                return new LessOperator(changeList);
            }
        };
        def.doAction(x);
    }

    @Override
    public void performActionOnLogicalRightShiftEqualOperator(LogicalRightShiftEqualOperator x) {
        DefaultAction def = new DefaultAction(x) {
            @Override
            SolidityProgramElement createNewElement(ExtList changeList) {
                return new LogicalRightShiftEqualOperator(changeList);
            }
        };
        def.doAction(x);
    }

    @Override
    public void performActionOnLogicalRightShiftOperator(LogicalRightShiftOperator x) {
        DefaultAction def = new DefaultAction(x) {
            @Override
            SolidityProgramElement createNewElement(ExtList changeList) {
                return new LogicalRightShiftOperator(changeList);
            }
        };
        def.doAction(x);
    }

    @Override
    public void performActionOnMinusEqualOperator(MinusEqualOperator x) {
        DefaultAction def = new DefaultAction(x) {
            @Override
            SolidityProgramElement createNewElement(ExtList changeList) {
                return new MinusEqualOperator(changeList);
            }
        };
        def.doAction(x);
    }

    @Override
    public void performActionOnMinusMinusOperator(MinusMinusOperator x) {
        DefaultAction def = new DefaultAction(x) {
            @Override
            SolidityProgramElement createNewElement(ExtList changeList) {
                return new MinusMinusOperator(changeList);
            }
        };
        def.doAction(x);
    }

    @Override
    public void performActionOnModEqualOperator(ModEqualOperator x) {
        DefaultAction def = new DefaultAction(x) {
            @Override
            SolidityProgramElement createNewElement(ExtList changeList) {
                return new ModEqualOperator(changeList);
            }
        };
        def.doAction(x);
    }

    @Override
    public void performActionOnModOperator(ModOperator x) {
        DefaultAction def = new DefaultAction(x) {
            @Override
            SolidityProgramElement createNewElement(ExtList changeList) {
                return new ModOperator(changeList);
            }
        };
        def.doAction(x);
    }

    @Override
    public void performActionOnMultiplicationEqualOperator(MultiplicationEqualOperator x) {
        DefaultAction def = new DefaultAction(x) {
            @Override
            SolidityProgramElement createNewElement(ExtList changeList) {
                return new MultiplicationEqualOperator(changeList);
            }
        };
        def.doAction(x);
    }

    @Override
    public void performActionOnMultiplicationOperator(MultiplicationOperator x) {
        DefaultAction def = new DefaultAction(x) {
            @Override
            SolidityProgramElement createNewElement(ExtList changeList) {
                return new MultiplicationOperator(changeList);
            }
        };
        def.doAction(x);
    }

    @Override
    public void performActionOnNegateOperator(NegateOperator x) {
        DefaultAction def = new DefaultAction(x) {
            @Override
            SolidityProgramElement createNewElement(ExtList changeList) {
                return new NegateOperator(changeList);
            }
        };
        def.doAction(x);
    }

    @Override
    public void performActionOnNotOperator(NotOperator x) {
        DefaultAction def = new DefaultAction(x) {
            @Override
            SolidityProgramElement createNewElement(ExtList changeList) {
                return new NotOperator(changeList);
            }
        };
        def.doAction(x);
    }

    @Override
    public void performActionOnOrEqualOperator(OrEqualOperator x) {
        DefaultAction def = new DefaultAction(x) {
            @Override
            SolidityProgramElement createNewElement(ExtList changeList) {
                return new OrEqualOperator(changeList);
            }
        };
        def.doAction(x);
    }

    @Override
    public void performActionOnOrOperator(OrOperator x) {
        DefaultAction def = new DefaultAction(x) {
            @Override
            SolidityProgramElement createNewElement(ExtList changeList) {
                return new OrOperator(changeList);
            }
        };
        def.doAction(x);
    }

    @Override
    public void performActionOnPlusEqualOperator(PlusEqualOperator x) {
        DefaultAction def = new DefaultAction(x) {
            @Override
            SolidityProgramElement createNewElement(ExtList changeList) {
                return new PlusEqualOperator(changeList);
            }
        };
        def.doAction(x);
    }

    @Override
    public void performActionOnPlusPlusOperator(PlusPlusOperator x) {
        DefaultAction def = new DefaultAction(x) {
            @Override
            SolidityProgramElement createNewElement(ExtList changeList) {
                return new PlusPlusOperator(changeList);
            }
        };
        def.doAction(x);
    }

    @Override
    public void performActionOnRightShiftEqualOperator(RightShiftEqualOperator x) {
        DefaultAction def = new DefaultAction(x) {
            @Override
            SolidityProgramElement createNewElement(ExtList changeList) {
                return new RightShiftEqualOperator(changeList);
            }
        };
        def.doAction(x);
    }

    @Override
    public void performActionOnRightShiftOperator(RightShiftOperator x) {
        DefaultAction def = new DefaultAction(x) {
            @Override
            SolidityProgramElement createNewElement(ExtList changeList) {
                return new RightShiftOperator(changeList);
            }
        };
        def.doAction(x);
    }

    @Override
    public void performActionOnSubtractionOperator(SubtractionOperator x) {
        DefaultAction def = new DefaultAction(x) {
            @Override
            SolidityProgramElement createNewElement(ExtList changeList) {
                return new SubtractionOperator(changeList);
            }
        };
        def.doAction(x);
    }

    @Override
    public void performActionOnTernaryOperator(TernaryOperator x) {
        DefaultAction def = new DefaultAction(x) {
            @Override
            SolidityProgramElement createNewElement(ExtList changeList) {
                return new TernaryOperator(changeList);
            }
        };
        def.doAction(x);
    }

    @Override
    public void performActionOnUnequalOperator(UnequalOperator x) {
        DefaultAction def = new DefaultAction(x) {
            @Override
            SolidityProgramElement createNewElement(ExtList changeList) {
                return new UnequalOperator(changeList);
            }
        };
        def.doAction(x);
    }

    @Override
    public void performActionOnXorEqualOperator(XorEqualOperator x) {
        DefaultAction def = new DefaultAction(x) {
            @Override
            SolidityProgramElement createNewElement(ExtList changeList) {
                return new XorEqualOperator(changeList);
            }
        };
        def.doAction(x);
    }
}
