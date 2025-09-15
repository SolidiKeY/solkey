package org.key_project.solidity.program.ast.declarations.FunctionEnums;

public enum DataLocation {
    Memory("memory"), Storage("storage"), Calldata("calldata"), Default("default");

    private final String label;

    DataLocation(String label) {
        this.label = label;
    }
    public static DataLocation fromString(String text) {
        for (DataLocation level : DataLocation.values()) {
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
