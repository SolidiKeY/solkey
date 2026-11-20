/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package de.uka.ilkd.key.java.transformations.pipeline;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.stmt.SwitchEntry;
import com.github.javaparser.symbolsolver.javaparsermodel.declarations.JavaParserEnumConstantDeclaration;

/// This transformation is made to transform any found [EnumDeclaration] into a corresponding
/// [EnumClassDeclaration].
///
/// @author mulbrich, drodt
/// @since 2006-11-20
/// @version 2026-03-03
public class EnumClassBuilder extends JavaTransformerAbstract {
    public EnumClassBuilder(TransformationPipelineServices pipelineServices) {
        super(pipelineServices);
    }

    @Override
    public void apply(CompilationUnit cu) {
        cu.walk(NameExpr.class, it -> {
            if (it.getParentNode().isPresent() && it.getParentNode().get() instanceof SwitchEntry
                    && it.resolve() instanceof JavaParserEnumConstantDeclaration ed) {
                it.replace(
                    new FieldAccessExpr(new NameExpr(ed.getType().toString()), ed.getName()));
            }
        });
        for (var declaration : cu.findAll(EnumDeclaration.class)) {
            declaration.replace(new EnumClassDeclaration(declaration));
        }
    }
}
