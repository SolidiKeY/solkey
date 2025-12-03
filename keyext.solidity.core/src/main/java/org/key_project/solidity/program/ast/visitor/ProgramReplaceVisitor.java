package org.key_project.solidity.program.ast.visitor;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.key_project.solidity.common.Services;
import org.key_project.solidity.program.ast.SolidityProgramElement;
import org.key_project.solidity.program.ast.expressions.operators.AssignmentExpression;
import org.key_project.solidity.rule.matching.inst.SVInstantiations;
import org.key_project.util.ExtList;

import java.util.Objects;
import java.util.regex.Pattern;

public class ProgramReplaceVisitor extends CreatingASTVisitor {
    private @Nullable SolidityProgramElement result = null;

    private final SVInstantiations svinsts;

    /// create the ProgramReplaceVisitor
    ///
    /// @param root the ProgramElement where to begin
    /// @param services The Services object.
    /// @param svi Schema Variable Instantiations
    public ProgramReplaceVisitor(SolidityProgramElement root, Services services,
                                 SVInstantiations svi) {
        super(root, false, services);
        svinsts = svi;
    }

    /// starts the walker
    @Override
    public void start() {
        assert result == null : "ProgramReplaceVisitor is not designed for multiple walks";
        stack.push(new ExtList());
        walk(root());
        final ExtList astList = getTop();
        for (int i = 0, sz = astList.size(); result == null && i < sz; i++) {
            final Object element = astList.get(i);
            if (element instanceof SolidityProgramElement pe) {
                result = pe;
            }
        }
    }

    /// @return The result.
    public SolidityProgramElement result() {
        return Objects.requireNonNull(result);
    }

    /// the implemented default action is called if a program element is, and if it has children all
    /// its children too are left unchanged
    @Override
    protected void doDefaultAction(SolidityProgramElement x) {
        addChild(x);
    }

    @Override
    public void performActionOnAssignmentExpression(AssignmentExpression x) {
        ExtList changeList = getTop();
        if (!changeList.isEmpty() && changeList.getFirst() == CHANGED) {
            changeList.removeFirst();
            Pattern pat = changeList.removeFirstOccurrence(Pattern.class);
            /*if (pat != null) {
                if (pat instanceof BindingPattern b) {
                    var pv = b.pv();
                    stack.pop();
                    var el = new ExtList();
                    assert pv != null;
                    el.add(pv);
                    el.addAll(changeList);
                    stack.push(el);
                }
            }*/
            changed();
        }
        super.performActionOnAssignmentExpression(x);
    }
}
