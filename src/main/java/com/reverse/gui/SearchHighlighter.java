package com.reverse.gui;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SearchHighlighter {
    private SearchHighlighter() {}

    public static int highlight(JTextPane pane, String query) {
        Highlighter hl = pane.getHighlighter();
        hl.removeAllHighlights();
        if (query == null || query.isBlank()) return 0;
        try {
            String text = pane.getDocument().getText(0, pane.getDocument().getLength());
            Matcher m = Pattern.compile(Pattern.quote(query), Pattern.CASE_INSENSITIVE).matcher(text);
            var painter = new DefaultHighlighter.DefaultHighlightPainter(new Color(255, 215, 115));
            int count = 0;
            while (m.find()) {
                hl.addHighlight(m.start(), m.end(), painter);
                count++;
            }
            return count;
        } catch (Exception ignored) { return 0; }
    }
}
