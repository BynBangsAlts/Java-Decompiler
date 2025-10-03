package com.reverse.ui;

import com.reverse.decompile.Decompilers.Type;

import javax.swing.text.*;
import java.awt.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SyntaxHighlighter {
    private SyntaxHighlighter() {}

    // --- Creds to GPT For the List ---
    private static final Color ORANGE = new Color(204, 120, 50);   // ASM instructions, descriptors, labels, etc.
    private static final Color GRAY   = new Color(128, 128, 128);  // comments & default
    private static final Color KW    = new Color(204, 120, 50);   // orange
    private static final Color TYPE  = new Color(104, 151, 187);  // blue
    private static final Color STR   = new Color(152, 118, 170);  // purple
    private static final Color NUM   = new Color(98, 151, 85);    // green
    private static final Color ANN   = new Color(200, 200, 50);   // yellow
    private static final Color CMT   = new Color(128, 128, 128);  // gray
    private static final Pattern J_KEY = Pattern.compile("\\b(abstract|assert|boolean|break|byte|case|catch|class|const|continue|default|do|double|else|enum|extends|final|finally|float|for|goto|if|implements|import|instanceof|int|interface|long|native|new|package|private|protected|public|return|short|static|strictfp|super|switch|synchronized|this|throw|throws|transient|try|void|volatile|while)\\b");
    private static final Pattern J_BOOL_NULL = Pattern.compile("\\b(true|false|null)\\b");
    private static final Pattern J_STR = Pattern.compile("\"([^\"\\\\]|\\\\.)*\"|'([^'\\\\]|\\\\.)*'");
    private static final Pattern J_NUM = Pattern.compile("\\b(0[xX][0-9a-fA-F_]+|0[bB][01_]+|\\d[\\d_]*(?:\\.\\d[\\d_]*)?(?:[eE][+-]?\\d[\\d_]*)?)[fFdDlL]?\\b");
    private static final Pattern J_ANN = Pattern.compile("@[A-Za-z_][A-Za-z0-9_]*(?:\\.[A-Za-z_][A-Za-z0-9_]*)*");
    private static final Pattern J_CMT = Pattern.compile("//[^\\n]*|/\\*[\\s\\S]*?\\*/");
    private static final Pattern J_TYPE = Pattern.compile("\\b(?:[A-Z][A-Za-z0-9_]*\\.)*[A-Z][A-Za-z0-9_]*\\b");
    private static final Pattern A_ANY  = Pattern.compile("\\S+");     // everything non-space → orange
    private static final Pattern A_LBL  = Pattern.compile("L\\d+:");   // labels → orange
    private static final Pattern A_CMT  = Pattern.compile(";.*$", Pattern.MULTILINE); // comments → gray

    public static void apply(StyledDocument doc, Type t) { applyJava(doc); }

    /** Java highlighting */
    public static void applyJava(StyledDocument doc) {
        clear(doc);
        paint(doc, J_KEY, KW);
        paint(doc, J_BOOL_NULL, NUM);
        paint(doc, J_STR, STR);
        paint(doc, J_NUM, NUM);
        paint(doc, J_ANN, ANN);
        paint(doc, J_CMT, CMT, Font.ITALIC);
        paint(doc, J_TYPE, TYPE);
    }

    public static void applyAsm(StyledDocument doc) {
        clear(doc);
        Style gray = doc.addStyle("asm-default", null);
        StyleConstants.setForeground(gray, GRAY);
        doc.setCharacterAttributes(0, doc.getLength(), gray, true);
        paint(doc, A_ANY, ORANGE, Font.PLAIN);
        paint(doc, A_LBL, ORANGE, Font.BOLD);
        paint(doc, A_CMT, GRAY, Font.ITALIC);
    }

    /* --- helpers --- */
    private static void clear(StyledDocument doc) {
        var def = new SimpleAttributeSet();
        doc.setCharacterAttributes(0, doc.getLength(), def, true);
    }

    private static void paint(StyledDocument doc, Pattern p, Color c) {
        paint(doc, p, c, Font.PLAIN);
    }

    private static void paint(StyledDocument doc, Pattern p, Color c, int style) {
        Style s = doc.getStyle(c.toString() + style);
        if (s == null) {
            s = doc.addStyle(c.toString() + style, null);
            StyleConstants.setForeground(s, c);
            StyleConstants.setItalic(s, (style & Font.ITALIC) != 0);
            StyleConstants.setBold(s, (style & Font.BOLD) != 0);
        }
        try {
            Matcher m = p.matcher(doc.getText(0, doc.getLength()));
            while (m.find()) {
                doc.setCharacterAttributes(m.start(), m.end() - m.start(), s, false);
            }
        } catch (BadLocationException ignored) {}
    }
}
