package com.reverse.model;

public record ClassEntry(String classPath, String simpleName, boolean b, boolean b1, boolean b2) {
    private static String renamed;

    @Override
    public String toString() {
        return getRenamed();
    }

    public String getRenamed() {
        return renamed != null ? renamed : simpleName;
    }
}
