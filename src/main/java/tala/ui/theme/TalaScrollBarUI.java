package tala.ui.theme;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;

/**
 * TalaScrollBarUI
 * ------------------------------------------------------------
 * A slim lilac pill scroll bar with no arrow buttons (design: thumb
 * #cfc5ea; it turns violet and a little wider under the mouse).
 * Installed for every scroll bar by TalaLook.install().
 */
public class TalaScrollBarUI extends BasicScrollBarUI {

    public static ComponentUI createUI(JComponent c) {
        return new TalaScrollBarUI();
    }

    @Override
    protected void configureScrollBarColors() {
        thumbColor = new Color(0xcfc5ea);
        trackColor = new Color(0, 0, 0, 0);
    }

    @Override
    protected void installDefaults() {
        super.installDefaults();
        scrollbar.setOpaque(false);
    }

    @Override
    public Dimension getPreferredSize(JComponent c) {
        return scrollbar.getOrientation() == VERTICAL ? new Dimension(12, 48) : new Dimension(48, 12);
    }

    @Override protected JButton createDecreaseButton(int orientation) { return noButton(); }
    @Override protected JButton createIncreaseButton(int orientation) { return noButton(); }

    private static JButton noButton() {
        JButton b = new JButton();
        b.setPreferredSize(new Dimension(0, 0));
        b.setMinimumSize(new Dimension(0, 0));
        b.setMaximumSize(new Dimension(0, 0));
        return b;
    }

    @Override
    protected void paintTrack(Graphics g, JComponent c, Rectangle r) {
        // see-through track: the page shows behind it
    }

    @Override
    protected void paintThumb(Graphics g, JComponent c, Rectangle r) {
        if (r.isEmpty()) {
            return;
        }
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        boolean active = isDragging || isThumbRollover();
        g2.setColor(active ? new Color(0xa78bfa) : thumbColor);
        int pad = active ? 2 : 3;
        int arc = Math.min(r.width, r.height) - pad * 2;
        g2.fillRoundRect(r.x + pad, r.y + pad, r.width - pad * 2, r.height - pad * 2, arc, arc);
        g2.dispose();
    }

    @Override
    protected void setThumbRollover(boolean rollover) {
        super.setThumbRollover(rollover);
        scrollbar.repaint();
    }
}
