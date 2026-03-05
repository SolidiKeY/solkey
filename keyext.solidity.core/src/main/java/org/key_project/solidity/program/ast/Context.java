package org.key_project.solidity.program.ast;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.key_project.logic.Namespace;
import org.key_project.solidity.logic.op.ProgramVariable;

import java.nio.file.Path;
import java.util.Objects;
import java.util.stream.Collectors;

public class Context {
    public static final String TMP_FN_NAME = "__SOLIDITY_KEY_CTX_FN_NAME__";
    private final Namespace<@NonNull ProgramVariable> varNS;
    private final @Nullable Path solidityPath;

    public Context(Namespace<@NonNull ProgramVariable> varNS) {
        this(varNS, null);
    }

    public Context(Namespace<@NonNull ProgramVariable> varNS, @Nullable Path solidityPath) {
        this.varNS = varNS;
        this.solidityPath = solidityPath;
    }

    public String buildFunction(String block) {
        var sb = new StringBuilder();
        sb.append("function ").append(TMP_FN_NAME).append("(");
        sb.append(varNS.allElements().stream().map(pv ->
                getType(pv) + " " + pv.name()).collect(Collectors.joining(", ")));
        sb.append(") public\n");
        sb.append(block);
        sb.append("\n");
        return sb.toString();
    }

    private String getType(ProgramVariable pv) {
        return Objects.requireNonNull(pv.getKeYSolidityType()).toString();
    }

    public @Nullable Path getSolidityPath() {
        return solidityPath;
    }

    public Namespace<@NonNull ProgramVariable> getVarNS() {
        return varNS;
    }

}
