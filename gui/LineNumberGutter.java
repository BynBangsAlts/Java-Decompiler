package com.reverse.gui;

import javax.swing.*;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Element;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.geom.Rectangle2D;

public class LineNumberGutter extends JComponent {
    private final JTextPane text;
    private final Font font = new Font(Font.MONOSPACED, Font.PLAIN, 12);

    public LineNumberGutter(JTextPane text) {
        this.text = text;
        setPreferredSize(new Dimension(48, 0));

        text.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { repaint(); }
            public void removeUpdate(DocumentEvent e) { repaint(); }
            public void changedUpdate(DocumentEvent e) { repaint(); }
        });
        text.addCaretListener(new CaretListener() {
            @Override public void caretUpdate(CaretEvent e) { repaint(); }
        });
        text.addPropertyChangeListener("font", evt -> repaint());
    }

    @Override protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setFont(font);

            Rectangle visible = getVisibleArea(text);
            if (visible.height <= 0) return;

            int startPos = safeViewToModel(text, new Point(0, visible.y));
            int endPos   = safeViewToModel(text, new Point(0, visible.y + visible.height));

            Element root = text.getDocument().getDefaultRootElement();
            int startLine = root.getElementIndex(startPos);
            int endLine   = root.getElementIndex(endPos);

            Color fg = UIManager.getColor("Label.disabledForeground");
            if (fg == null) fg = new Color(130,130,130);

            for (int i = startLine; i <= endLine; i++) {
                Element line = root.getElement(i);
                if (line == null) continue;

                Rectangle r = safeModelToViewBounds(text, line.getStartOffset());
                if (r == null) continue;

                String num = String.valueOf(i + 1);
                int baseline = r.y + r.height - 4;

                int w = getWidth();
                int strW = g2.getFontMetrics().stringWidth(num);

                g2.setColor(fg);
                g2.drawString(num, w - strW - 6, baseline);
            }
        } finally {
            g2.dispose();
        }
    }

    private static Rectangle getVisibleArea(JTextComponent c) {
        Rectangle vr = c.getVisibleRect();           // view coords
        if (vr == null) vr = new Rectangle(0,0,0,0);
        return vr;
    }

    private static int safeViewToModel(JTextComponent c, Point p) {
        try {
            return c.viewToModel2D(p);
        } catch (Exception ex) {
            return 0;
        }
    }

    private static Rectangle safeModelToViewBounds(JTextComponent c, int offs) {
        try {
            int len = Math.max(0, c.getDocument().getLength());
            int clamped = Math.max(0, Math.min(offs, Math.max(0, len - 1)));
            Rectangle2D r2 = c.modelToView2D(clamped);
            return r2 != null ? r2.getBounds() : null;
        } catch (Exception ex) {
            return null;
        }
    }
}
