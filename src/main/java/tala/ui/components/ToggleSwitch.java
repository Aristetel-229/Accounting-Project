package tala.ui.components;

import tala.ui.theme.Hover;
import tala.ui.theme.Theme;

import javax.swing.JToggleButton;
import javax.swing.Timer;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/**
 * ToggleSwitch
 * ------------------------------------------------------------
 * An on/off switch:  ( o    )  off (soft grey)   (    o )  on (violet).
 * The knob slides smoothly between the two ends, and the track darkens
 * a little under the mouse.
 * It is a JToggleButton, so the Space key and screen readers work too.
 */
public class ToggleSwitch extends JToggleButton {

    private float position;          // 0 = off ... 1 = on (animated)
    private final Timer slide;

    public ToggleSwitch(String accessibleName) {
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        getAccessibleContext().setAccessibleName(accessibleName);
        setToolTipText(accessibleName);
        Hover.of(this);
        slide = new Timer(15, e -> {
            float target = isSelected() ? 1f : 0f;
            position += (target - position) * 0.3f;
            if (Math.abs(target - position) < 0.02f) {
                position = target;
                ((Timer) e.getSource()).stop();
            }
            repaint();
        });
        getModel().addItemListener(e -> slide.start());
    }

    @Override
    public void setSelected(boolean b) {
        super.setSelected(b);
        if (!isShowing()) {
            position = b ? 1f : 0f;   // no animation while the page is being filled
        }
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(54, 44);   // the track is 46 x 26; the rest is click space
    }

    @Override
    public Dimension getMaximumSize() {
        return getPreferredSize();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int w = 46, h = 26;
        int x = (getWidth() - w) / 2, y = (getHeight() - h) / 2;
        float hover = Hover.of(this).value();

        Color off = Theme.mix(new Color(0xd9d4e6), new Color(0xc9c2dc), hover);
        Color on = Theme.mix(new Color(0x7c5fd6), new Color(0x6d28d9), hover);
        g2.setColor(!isEnabled() ? new Color(0xeeebf4) : Theme.mix(off, on, position));
        g2.fillRoundRect(x, y, w, h, h, h);

        int knob = h - 6;
        float kx = x + 3 + (w - knob - 6) * position;
        g2.setColor(new Color(46, 16, 101, 40));
        g2.fillOval(Math.round(kx), y + 4, knob, knob);         // small knob shadow
        g2.setColor(position > 0.5f ? Theme.YELLOW : Color.WHITE);
        g2.fillOval(Math.round(kx), y + 3, knob, knob);

        if (isFocusOwner()) {
            g2.setColor(Theme.FOCUS);
            g2.setStroke(new BasicStroke(2f));
            g2.drawRoundRect(x - 3, y - 3, w + 6, h + 6, h + 6, h + 6);
        }
        g2.dispose();
    }
}
