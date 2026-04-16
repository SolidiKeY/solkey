/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program;

import java.io.IOException;

import org.key_project.logic.Namespace;
import org.key_project.solidity.common.Services;
import org.key_project.solidity.logic.NamespaceSet;
import org.key_project.solidity.logic.SolidityBlock;
import org.key_project.solidity.program.ast.Context;
import org.key_project.solidity.program.ast.SolidityReader;
import org.key_project.solidity.rule.sv.ProgramSV;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class SoliditySchemaReader extends SolidityReader {
    private @Nullable Namespace<@NonNull ProgramSV> svNS;

    public SoliditySchemaReader(Services services, NamespaceSet nss) {
        super(services);
        svNS = null;
    }

    public void setSVNamespace(Namespace<@NonNull ProgramSV> ns) {
        this.svNS = ns;
    }

    public SolidityBlock readBlock(String block, Context context) throws IOException {
        if (svNS == null)
            return super.readBlock(block, context);
        return super.readBlock(block, context, svNS);
    }
}
