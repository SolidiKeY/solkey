package org.key_project.solidity.program.ast.visitor;

import org.key_project.solidity.common.Services;
import org.key_project.solidity.program.ast.SolidityProgramElement;
import org.key_project.solidity.program.ast.expressions.operators.AddOperator;
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
}
