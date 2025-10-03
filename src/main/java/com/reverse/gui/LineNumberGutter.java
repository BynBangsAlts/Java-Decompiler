package com.reverse.ui;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.Element;
import java.awt.*;

/**
 * Still Buggy Ill Fix It Later
 */
public class LineNumberGutter extends JComponent {
    private final JTextPane text;

    public LineNumberGutter(JTextPane text) {
        this.text = text;
        setPreferredSize(new Dimension(48, 0));
        text.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { repaint(); }
            public void removeUpdate(DocumentEvent e) { repaint(); }
            public void changedUpdate(DocumentEvent e) { repaint(); }
        });
        text.addCaretListener(e -> repaint());
        text.addPropertyChangeListener("font", evt -> repaint());
        SwingUtilities.invokeLater(() -> {
            JScrollPane scroll = (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, text);
            if (scroll != null) {
                scroll.getVerticalScrollBar().getModel().addChangeListener(e -> repaint());
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setFont(text.getFont());
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            Rectangle clip = g.getClipBounds();
            Element root = text.getDocument().getDefaultRootElement();
            int start = text.viewToModel2D(new Point(0, clip.y));
            int end   = text.viewToModel2D(new Point(0, clip.y + clip.height));
            int startLine = root.getElementIndex(start);
            int endLine   = root.getElementIndex(end);
            FontMetrics fm = g2.getFontMetrics();
            Color fg = UIManager.getColor("Label.disabledForeground");
            if (fg == null) fg = Color.GRAY;
            for (int i = startLine; i <= endLine; i++) {
                Element line = root.getElement(i);
                try {
                    Rectangle r = text.modelToView2D(line.getStartOffset()).getBounds();
                    String num = String.valueOf(i + 1);
                    int baseline = r.y + fm.getAscent();
                    int strW = fm.stringWidth(num);
                    g2.setColor(fg);
                    g2.drawString(num, getWidth() - strW - 6, baseline);
                } catch (Exception ignored) {}
            }
        } finally {
            g2.dispose();
        }
    }
}
