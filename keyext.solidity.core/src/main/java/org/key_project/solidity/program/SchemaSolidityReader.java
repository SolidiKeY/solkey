/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program;

import org.key_project.logic.Namespace;
import org.key_project.logic.op.sv.SchemaVariable;
import org.key_project.solidity.common.Services;
import org.key_project.solidity.logic.NamespaceSet;

import org.jspecify.annotations.NonNull;

public class SchemaSolidityReader extends SolidityReader {

    private Namespace schemaVariables;

    public SchemaSolidityReader(Services services, NamespaceSet nss) {
        super(services, nss);
    }

    public void setSVNamespace(Namespace<@NonNull SchemaVariable> schemaVariables) {
        this.schemaVariables = schemaVariables;
    }
}
