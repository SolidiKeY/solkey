package org.key_project.solidity.program.ast.declarations.FunctionEnums;

public enum Visibility {
    internal("internal"), external("external"), Private("private"), Public("public");

    private final String label;

    Visibility(String label) {
        this.label = label;
    }


    public static Visibility fromString(String text) {
        for (Visibility level : Visibility.values()) {
            if (level.label.equalsIgnoreCase(text)) {
                return level;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return label;
    }

}
