/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.parser.builder;

import org.key_project.solidity.common.Services;
import org.key_project.solidity.logic.NamespaceSet;

public class ProblemFinder extends ExpressionBuilder {
    public ProblemFinder(Services services, NamespaceSet nss) {
        super(services, nss);
    }
}
