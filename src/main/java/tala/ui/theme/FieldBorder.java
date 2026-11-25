package tala.ui.theme;

import javax.swing.JComponent;
import javax.swing.border.AbstractBorder;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

/**
 * FieldBorder
 * ------------------------------------------------------------
 * The border of every text box and drop-down list (light design):
 *
 *   normal  : 1.5px lilac line (#d6cbf2), 10px corners
 *   hover   : the line fades to a darker lilac (#b9a3f3)
 *   typing  : 2px violet line (#7c3aed) with a soft 3px violet ring around it
 *
 * The outer 3px are kept free for the ring, so nothing jumps on focus.
 */
public class FieldBorder extends AbstractBorder {

    static final int RING = 3;
    private static final int ARC = 20;

    /** Makes a field repaint when it gains / loses focus and when the mouse comes in */
    public static void watch(final JComponent field) {
        Hover.of(field);
        field.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) { field.repaint(); }
            @Override public void focusLost(FocusEvent e)   { field.repaint(); }
        });
    }

    private static boolean focused(Component c) {
        if (c.hasFocus()) {
            return true;
        }
        // a drop-down list: its editor or arrow button may hold the focus
        Component owner = java.awt.KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
        return owner != null && c instanceof java.awt.Container
                && javax.swing.SwingUtilities.isDescendingFrom(owner, c);
    }

    @Override
    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        boolean focus = focused(c) && c.isEnabled();
        float hover = c instanceof JComponent ? Hover.of((JComponent) c).value() : 0f;
        int bx = x + RING, by = y + RING, bw = width - RING * 2 - 1, bh = height - RING * 2 - 1;
        if (focus) {
            g2.setColor(Theme.FOCUS_RING);
            g2.setStroke(new BasicStroke(RING * 2f));
            g2.drawRoundRect(bx, by, bw, bh, ARC, ARC);
            g2.setColor(Theme.FOCUS);
            g2.setStroke(new BasicStroke(2f));
        } else {
            Color line = c.isEnabled() ? Theme.mix(Theme.FIELD_BORDER, Theme.FIELD_HOVER, hover) : Theme.DIVIDER;
            g2.setColor(line);
            g2.setStroke(new BasicStroke(1.5f));
        }
        g2.drawRoundRect(bx, by, bw, bh, ARC, ARC);
        // search boxes: a magnifier on the left
        if (c instanceof JComponent && Boolean.TRUE.equals(((JComponent) c).getClientProperty(Theme.SEARCH_ICON))) {
            int cy = y + height / 2;
            g2.setColor(focus ? Theme.FOCUS : new Color(0x8f88a8));
            g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawOval(x + RING + 12, cy - 7, 11, 11);
            g2.drawLine(x + RING + 21, cy + 3, x + RING + 25, cy + 7);
        }
        g2.dispose();
    }

    @Override
    public Insets getBorderInsets(Component c) {
        boolean search = c instanceof JComponent
                && Boolean.TRUE.equals(((JComponent) c).getClientProperty(Theme.SEARCH_ICON));
        return new Insets(RING + 6, RING + (search ? 34 : 12), RING + 6, RING + 12);
    }

    @Override
    public Insets getBorderInsets(Component c, Insets insets) {
        Insets i = getBorderInsets(c);
        insets.set(i.top, i.left, i.bottom, i.right);
        return insets;
    }
}
