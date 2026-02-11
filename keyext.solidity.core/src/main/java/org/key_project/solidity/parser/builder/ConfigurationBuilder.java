/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.parser.builder;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

import org.key_project.solidity.parser.KeYSolidityDLParser;
import org.key_project.solidity.parser.KeYSolidityDLParserBaseVisitor;
import org.key_project.solidity.settings.Configuration;

import org.jspecify.annotations.NonNull;


public class ConfigurationBuilder extends KeYSolidityDLParserBaseVisitor<Object> {
    @Override
    public List<Object> visitCfile(KeYSolidityDLParser.CfileContext ctx) {
        return ctx.cvalue().stream().map(it -> it.accept(this)).collect(Collectors.toList());
    }

    @Override
    public String visitCkey(KeYSolidityDLParser.CkeyContext ctx) {
        if (ctx.STRING_LITERAL() != null)
            return sanitizeStringLiteral(ctx.STRING_LITERAL().getText());
        return ctx.IDENT().getText();
    }

    @Override
    public String visitCsymbol(KeYSolidityDLParser.CsymbolContext ctx) {
        return ctx.IDENT().getText();
    }


    @Override
    public String visitCstring(KeYSolidityDLParser.CstringContext ctx) {
        final var text = ctx.getText();
        return sanitizeStringLiteral(text);
    }

    private static @NonNull String sanitizeStringLiteral(String text) {
        return text.substring(1, text.length() - 1)
                .replace("\\\"", "\"")
                .replace("\\\\", "\\");
    }

    @Override
    public Long visitCintb(KeYSolidityDLParser.CintbContext ctx) {
        return Long.parseLong(ctx.getText(), 2);
    }

    @Override
    public Long visitCinth(KeYSolidityDLParser.CinthContext ctx) {
        return Long.parseLong(ctx.getText(), 16);
    }

    @Override
    public Long visitCintd(KeYSolidityDLParser.CintdContext ctx) {
        final var text = ctx.getText();
        if (text.endsWith("L") || text.endsWith("l")) {
            return Long.parseLong(text.substring(0, text.length() - 1), 10);
        } else {
            return Long.parseLong(text, 10);
        }
    }

    @Override
    public Double visitCfpf(KeYSolidityDLParser.CfpfContext ctx) {
        return Double.parseDouble(ctx.getText());
    }

    @Override
    public Double visitCfpd(KeYSolidityDLParser.CfpdContext ctx) {
        return Double.parseDouble(ctx.getText());
    }

    @Override
    public Double visitCfpr(KeYSolidityDLParser.CfprContext ctx) {
        return Double.parseDouble(ctx.getText());
    }

    @Override
    public Boolean visitCbool(KeYSolidityDLParser.CboolContext ctx) {
        return Boolean.parseBoolean(ctx.getText());
    }

    @Override
    public Configuration visitTable(KeYSolidityDLParser.TableContext ctx) {
        final var data = new LinkedHashMap<String, Object>();
        for (KeYSolidityDLParser.CkvContext context : ctx.ckv()) {
            var name = context.ckey().accept(this).toString();
            var val = context.cvalue().accept(this);
            data.put(name, val);
        }
        return new Configuration(data);
    }

    @Override
    public List<Object> visitList(KeYSolidityDLParser.ListContext ctx) {
        var seq = new ArrayList<>(ctx.children.size());
        for (KeYSolidityDLParser.CvalueContext context : ctx.cvalue()) {
            seq.add(context.accept(this));
        }
        return seq;
    }
}
