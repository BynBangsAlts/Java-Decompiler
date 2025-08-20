package com.reverse;

import com.formdev.flatlaf.FlatLightLaf;
import com.reverse.settings.Theme;

import javax.swing.*;

public class App {
    public static void main(String[] args) {
        try {
            FlatLightLaf.setup();
            UIManager.put("Component.arc", 14);   // rounded
            UIManager.put("Button.arc", 18);
            UIManager.put("TextComponent.arc", 12);
            UIManager.put("ScrollBar.thumbArc", 999);
            UIManager.put("ScrollBar.trackArc", 999);
        } catch (Exception ignored) {}

        SwingUtilities.invokeLater(() -> {
            Theme.init();
            new com.reverse.gui.DecompilerFrame().setVisible(true);
        });
    }
}
