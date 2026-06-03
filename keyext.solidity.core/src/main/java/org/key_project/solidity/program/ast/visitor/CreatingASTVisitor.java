/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.visitor;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;

import org.key_project.solidity.common.Services;
import org.key_project.solidity.program.ast.SolidityProgramElement;
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
                return new FunctionCallExpression(changeList, x.getType());
            }
        };
        def.doAction(x);
    }

    @Override
    public void performActionOnIndexExpression(IndexExpression x) {
        DefaultAction def = new DefaultAction(x) {
            @Override
            SolidityProgramElement createNewElement(ExtList changeList) {
                return new IndexExpression(changeList, x.getType());
            }
        };
        def.doAction(x);
    }

    @Override
    public void performActionOnIndexRangeExpression(IndexRangeExpression x) {
        DefaultAction def = new DefaultAction(x) {
            @Override
            SolidityProgramElement createNewElement(ExtList changeList) {
                return new IndexRangeExpression(changeList, x.getType());
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
    public void performActionOnNewExpression(NewExpression x) {
        DefaultAction def = new DefaultAction(x) {
            @Override
            SolidityProgramElement createNewElement(ExtList changeList) {
                return new NewExpression(x.getFunction(), x.getType());
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
    public void performActionOnBinaryExpression(BinaryExpression x) {
        DefaultAction def = new DefaultAction(x) {
            @Override
            SolidityProgramElement createNewElement(ExtList changeList) {
                return new BinaryExpression(changeList, x.getType());
            }
        };
        def.doAction(x);
    }

    @Override
    public void performActionOnOperator(Operator x) {
        doDefaultAction(x);
    }

    @Override
    public void performActionOnUnaryExpression(UnaryExpression x) {
        DefaultAction def = new DefaultAction(x) {
            @Override
            SolidityProgramElement createNewElement(ExtList changeList) {
                return new UnaryExpression(changeList, x.getType());
            }
        };
        def.doAction(x);
    }

    @Override
    public void performActionOnTernaryOperator(TernaryExpression x) {
        DefaultAction def = new DefaultAction(x) {
            @Override
            SolidityProgramElement createNewElement(ExtList changeList) {
                return new TernaryExpression(changeList, x.getType());
            }
        };
        def.doAction(x);
    }

    @Override
    public void performActionOnContractReference(ContractReference x) {
        DefaultAction def = new DefaultAction(x) {
            @Override
            SolidityProgramElement createNewElement(ExtList changeList) {
                return new ContractReference(x.getContractDeclaration(), x.getType(), x.id);
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
                return new FunctionReference(x.referencedDeclaration, x.getType());
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
                return new UnresolvedReferenceException();
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
    public void performActionOnCatchClause(CatchClause x) {
        DefaultAction def = new DefaultAction(x) {
            @Override
            SolidityProgramElement createNewElement(ExtList changeList) {
                return new CatchClause(changeList);
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
    public void performActionOnForInit(ForInit x) {
        DefaultAction def = new DefaultAction(x) {
            @Override
            SolidityProgramElement createNewElement(ExtList changeList) {
                return new ForInit(changeList);
            }
        };
        def.doAction(x);
    }

    @Override
    public void performActionOnForUpdate(ForUpdate x) {
        DefaultAction def = new DefaultAction(x) {
            @Override
            SolidityProgramElement createNewElement(ExtList changeList) {
                return new ForUpdate(changeList);
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
    public void performActionOnReturnStatment(ReturnStatement x) {
        DefaultAction def = new DefaultAction(x) {
            @Override
            SolidityProgramElement createNewElement(ExtList changeList) {
                return new ReturnStatement(changeList);
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
