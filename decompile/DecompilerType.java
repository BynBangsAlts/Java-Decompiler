package com.reverse.decompile;

enum DecompilerType {
    VINEFLOWER("Vineflower"),
    CFR("CFR"),
    ASM("ASM");

    final String label;

    DecompilerType(String label) {
        this.label = label;
    }

    static DecompilerType fromLabel(String s) {
        if (s == null) return VINEFLOWER; // shrug default
        for (DecompilerType t : values()) if (t.label.equalsIgnoreCase(s)) return t;
        return VINEFLOWER;
    }
}