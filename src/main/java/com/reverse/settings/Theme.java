package com.reverse.settings;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.*;
import java.awt.*;
import java.util.prefs.Preferences;

public final class Theme {
    public enum Mode { LIGHT, DARK, SYSTEM }

    private static final Preferences P = Preferences.userNodeForPackage(Theme.class);
    private static final String K_MODE = "mode";
    private static final String K_FONT = "editor.font";
    private static final String K_SIZE = "editor.size";
    private static final String K_LINENO = "editor.linenumbers";
    private static final String K_ZEN = "ui.zen";

    private Theme() {}

    public static void init() {
        applyMode(getMode());
        applyGlobalStyle();
    }

    public static void applyGlobalStyle() {
        // breathe a bit
        UIManager.put("Component.focusWidth", 1);
        UIManager.put("TabbedPane.tabSeparatorsFullHeight", true);
        UIManager.put("TitlePane.unifiedBackground", true);
    }

    public static Mode getMode() {
        String raw = P.get(K_MODE, Mode.LIGHT.name());
        try { return Mode.valueOf(raw); } catch (Exception e) { return Mode.LIGHT; }
    }

    public static void setMode(Mode m) {
        P.put(K_MODE, m.name());
        applyMode(m);
        refreshAll();
    }

    private static void applyMode(Mode m) {
        try {
            if (m == Mode.SYSTEM) {
                UIManager.setLookAndFeel(new FlatLightLaf());
            } else if (m == Mode.DARK) {
                UIManager.setLookAndFeel(new FlatDarkLaf());
            } else {
                UIManager.setLookAndFeel(new FlatLightLaf());
            }
        } catch (Exception ignored) {}
    }

    public static boolean isDark() { return UIManager.getLookAndFeel() instanceof FlatDarkLaf; }

    public static void refreshAll() {
        for (Frame f : Frame.getFrames()) {
            SwingUtilities.updateComponentTreeUI(f);
            f.repaint();
        }
    }

    public static String editorFont() { return P.get(K_FONT, bestMono()); }
    public static int editorSize() { return Math.max(11, Math.min(22, P.getInt(K_SIZE, 14))); }
    public static boolean showLineNumbers() { return P.getBoolean(K_LINENO, true); }
    public static boolean zenMode() { return P.getBoolean(K_ZEN, false); }

    public static void setEditorFont(String family) { P.put(K_FONT, family); }
    public static void setEditorSize(int size) { P.putInt(K_SIZE, size); }
    public static void setShowLineNumbers(boolean on) { P.putBoolean(K_LINENO, on); }
    public static void setZenMode(boolean on) { P.putBoolean(K_ZEN, on); }

    private static String bestMono() {
        // pick something nice if available
        String[] favorites = {
                "JetBrains Mono", "Fira Code", "Cascadia Mono", "Consolas",
                "Menlo", "Monaco", "DejaVu Sans Mono", "Courier New", "Monospaced"
        };
        var env = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
        for (String f : favorites) for (String e : env) if (e.equalsIgnoreCase(f)) return e;
        return "Monospaced";
    }
}
