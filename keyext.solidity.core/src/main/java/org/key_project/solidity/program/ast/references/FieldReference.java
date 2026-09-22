/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.references;

import java.util.Objects;

import org.key_project.logic.Name;
import org.key_project.logic.SyntaxElement;
import org.key_project.solidity.program.ast.abstractions.Type;
import org.key_project.solidity.program.ast.declarations.StateVariableDeclaration;
import org.key_project.solidity.program.ast.expressions.SolidityExpression;
import org.key_project.solidity.program.ast.visitor.Visitor;
import org.key_project.util.ExtList;


/// A purely syntactic reference to a contract state variable (a field access, possibly with an
/// implicit `this` receiver). Unlike the old KeY-Java-style design, this node carries **no** logic
/// operator: the field's logic symbol is registered once under
/// [StateVariableDeclaration#getFieldConstantName] and is looked up lazily by a symbolic-execution
/// rule when it moves the field access into the logic (e.g. as part of a `selectSt`/`storeSt`
/// term). This keeps program syntax decoupled from the logic encoding and avoids any
/// `convertToTerm`-style translation in Java.
public class FieldReference extends SolidityExpression implements VariableReference {
    private final StateVariableDeclaration field;

    public FieldReference(StateVariableDeclaration field, Type type) {
        super(type);
        this.field = field;
    }

    public FieldReference(ExtList children, Type type) {
        super(type);
        this.field =
            Objects.requireNonNull(children.removeFirstOccurrence(StateVariableDeclaration.class));
    }

    /// The (namespaced) name under which the field's logic symbol is registered.
    public Name getFieldConstantName() {
        return field.getFieldConstantName();
    }

    @Override
    public String toString() {
        return field.getName().toString();
    }

    @Override
    public SyntaxElement getChild(int n) {
        throw outOfBounds(n);
    }

    @Override
    public int getChildCount() {
        return 0;
    }

    @Override
    public StateVariableDeclaration mainProgramElement() {
        return field;
    }

    @Override
    public void visit(Visitor v) {
        v.performActionOnFieldReference(this);
    }
}
