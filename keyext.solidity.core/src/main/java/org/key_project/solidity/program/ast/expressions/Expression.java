/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.expressions;

import org.key_project.solidity.program.ast.SolidityProgramElement;
import org.key_project.solidity.program.ast.abstractions.Type;

public abstract class Expression implements SolidityProgramElement {
    protected Type type;
    public Type getType(){
        return type;
    }
    public void setType(Type type){
        this.type = type;
    }
}
