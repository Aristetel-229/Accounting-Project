package tala.ui.components;

import tala.ui.theme.Theme;

import javax.swing.JLabel;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/**
 * Badge
 * ------------------------------------------------------------
 * A small rounded "pill" label, e.g.  [ Balanced ]  or  [ Asset ].
 */
public class Badge extends JLabel {

    private Color fill;
    private boolean onDark = false;

    public Badge(String text, Color fill, Color textColor) {
        super(text);
        setFont(Theme.semibold(12.5f));
        setBorder(javax.swing.BorderFactory.createEmptyBorder(3, 11, 3, 11));
        setColors(fill, textColor);
        setOpaque(false);
    }

    public void setColors(Color fill, Color textColor) {
        this.fill = fill;
        setForeground(textColor);
        repaint();
    }

    // ----- ready-made colors -----
    public static Badge ok(String text)      { return new Badge(text, Theme.GREEN_BG, Theme.GREEN); }
    public static Badge warn(String text)    { return new Badge(text, Theme.AMBER_BG, Theme.AMBER); }
    public static Badge bad(String text)     { return new Badge(text, Theme.RED_BG, Theme.RED); }
    public static Badge info(String text)    { return new Badge(text, Theme.PURPLE_TINT, Theme.PURPLE); }

    public void showOk(String text)   { setText(text); setColors(Theme.GREEN_BG, Theme.GREEN); }
    public void showWarn(String text) { setText(text); setColors(Theme.AMBER_BG, Theme.AMBER); }
    public void showBad(String text)  { setText(text); setColors(Theme.RED_BG, Theme.RED); }

    /** On a purple card header: see-through pills (mint for OK, rose for problems, white otherwise) */
    public void setOnDark(boolean onDark) {
        this.onDark = onDark;
        repaint();
    }

    private int darkTone() {
        if (Theme.GREEN_BG.equals(fill)) return 1;
        if (Theme.RED_BG.equals(fill)) return 2;
        return 0;
    }

    @Override
    public Color getForeground() {
        if (onDark && fill != null) {
            switch (darkTone()) {
                case 1:  return new Color(0xa7f3d0);
                case 2:  return new Color(0xfecdd3);
                default: return Color.WHITE;
            }
        }
        return super.getForeground();
    }

    @Override
    public Dimension getMaximumSize() {
        return getPreferredSize();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int h = getHeight();
        if (onDark) {
            int tone = darkTone();
            Color bg = tone == 1 ? new Color(110, 231, 183, 46) : tone == 2 ? new Color(253, 164, 175, 46) : new Color(255, 255, 255, 36);
            Color edge = tone == 1 ? new Color(167, 243, 208, 90) : tone == 2 ? new Color(254, 205, 211, 90) : new Color(255, 255, 255, 64);
            g2.setColor(bg);
            g2.fillRoundRect(0, 0, getWidth(), h, h, h);
            g2.setColor(edge);
            g2.drawRoundRect(0, 0, getWidth() - 1, h - 1, h, h);
        } else {
            g2.setColor(fill);
            g2.fillRoundRect(0, 0, getWidth(), h, h, h);
        }
        g2.dispose();
        super.paintComponent(g);
    }
}
