package tala.ui.components;

import tala.ui.theme.Hover;
import tala.ui.theme.Theme;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LayoutManager;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.RoundRectangle2D;

/**
 * RoundedPanel
 * ------------------------------------------------------------
 * A JPanel with rounded corners, a lilac outline, a soft purple shadow
 * and a white background. Every "card" in the app is one of these.
 *
 *   setFill(color)          -> another plain color instead of white
 *   setGradient(end, start) -> fill (top-left) to end (bottom-right), like
 *                              the tinted tiles "white -> #f1ebff"
 *   setGlow(color)          -> a faint wash of that color over the fill
 *   setHoverable(color)     -> reacts to the mouse: the outline glides to
 *                              violet (#a78bfa) and the shadow grows
 *   setLift(true)           -> on hover the whole card also rises 2px
 *                              (for small tiles; big cards with tables don't lift)
 */
public class RoundedPanel extends JPanel {

    /** Space kept around the shape for the shadow and the lift */
    static final int SIDE = 3, TOP = 3, BOTTOM = 9;

    private int radius = 16;
    private Color fill = null;                 // null = white
    private Color glow = null;
    private boolean accent = false;            // kept for older code (no top line in the light design)
    private Color gradientEnd = null;          // null = plain color
    private float gradientStart = 0f;
    private Color borderColor = Theme.CARD_BORDER;
    private float borderWidth = 1.5f;
    private boolean shadow = true;

    private boolean hoverable = false;
    private boolean lift = false;
    private Color hoverBorder = Theme.CARD_HOVER_BORDER;

    public RoundedPanel(LayoutManager layout) {
        super(layout);
        setOpaque(false);   // we paint the background ourselves
        // keep the content inside the outline and above the shadow
        setBorder(BorderFactory.createEmptyBorder(TOP + 1, SIDE + 1, BOTTOM + 1, SIDE + 1));
    }

    public RoundedPanel setRadius(int radius)       { this.radius = radius; repaint(); return this; }
    public RoundedPanel setFill(Color fill)         { this.fill = fill; repaint(); return this; }
    public RoundedPanel setBorderColor(Color color)  { this.borderColor = color; repaint(); return this; }
    public RoundedPanel setBorderWidth(float width) { this.borderWidth = width; repaint(); return this; }
    public RoundedPanel setShadow(boolean shadow)   { this.shadow = shadow; repaint(); return this; }
    public RoundedPanel setGlow(Color glow)         { this.glow = glow; repaint(); return this; }
    public RoundedPanel setAccent(boolean accent)   { this.accent = accent; repaint(); return this; }
    public RoundedPanel setLift(boolean lift)       { this.lift = lift; setHoverable(null); return this; }

    public int getRadius() {
        return radius;
    }

    /** Gradient from fill (top-left) to end (bottom-right), starting at 'startFraction' of the width */
    public RoundedPanel setGradient(Color end, float startFraction) {
        this.gradientEnd = end;
        this.gradientStart = startFraction;
        repaint();
        return this;
    }

    /**
     * Turns on the hover effect. borderWhenHovered is the outline color
     * while the mouse is over the panel (null = the usual violet).
     */
    public RoundedPanel setHoverable(Color borderWhenHovered) {
        if (borderWhenHovered != null) {
            // a soft version of the given tone, so colored tiles keep their character
            this.hoverBorder = Theme.mix(borderWhenHovered, Theme.CARD_HOVER_BORDER, 0.5f);
        }
        if (!hoverable) {
            hoverable = true;
            Hover.of(this);
        }
        return this;
    }

    private float hover() {
        return hoverable ? Hover.of(this).value() : 0f;
    }

    /** How far the card is lifted right now (0..2 px) */
    protected float liftNow() {
        return lift ? hover() * 2f : 0f;
    }

    /** The card's rounded outline (inside the shadow space) */
    protected Shape cardShape() {
        float y = TOP;
        return new RoundRectangle2D.Float(SIDE, y, getWidth() - SIDE * 2 - 1, getHeight() - TOP - BOTTOM,
                radius * 2, radius * 2);
    }

    // ---------------------------------------------------------------- painting

    /** Lifting tiles: everything (also the labels inside) is drawn a little higher */
    @Override
    public void paint(Graphics g) {
        float up = liftNow();
        if (up > 0.01f) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.translate(0, -up);
            super.paint(g2);
            g2.dispose();
        } else {
            super.paint(g);
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        float t = hover();
        float up = liftNow();
        int x = SIDE, y = TOP;
        int w = getWidth() - SIDE * 2 - 1;
        int h = getHeight() - TOP - BOTTOM;
        int arc = radius * 2;
        Shape shape = new RoundRectangle2D.Float(x, y, w, h, arc, arc);

        // 1) soft purple shadow: "0 1px 2px .06, 0 6px 14px .08, 0 18px 36px -12px .18"
        //    it gets deeper while hovered (and the card has risen above it)
        if (shadow) {
            Graphics2D s = (Graphics2D) g2.create();
            s.translate(0, up);    // the shadow stays on the "table"
            int layers = 7;
            for (int i = layers; i >= 1; i--) {
                float k = i / (float) layers;
                int alpha = Math.round((5 + 6 * t) * (1f - k * 0.75f));
                s.setColor(new Color(46, 16, 101, alpha));
                float grow = k * (1.5f + t);
                float drop = 1 + k * (4 + 3 * t);
                s.fill(new RoundRectangle2D.Float(x - grow, y + drop - grow * 0.3f, w + grow * 2, h + grow,
                        arc + grow * 2, arc + grow * 2));
            }
            s.dispose();
        }

        // 2) background: white, a plain color, or a diagonal gradient
        Color base = fill == null ? Theme.CARD_BG : fill;
        if (gradientEnd != null) {
            float startX = getWidth() * gradientStart;
            g2.setPaint(new GradientPaint(startX, 0, base, getWidth(), getHeight(), gradientEnd));
        } else {
            g2.setColor(base);
        }
        g2.fill(shape);

        // 3) an optional faint wash of color
        if (glow != null) {
            g2.setColor(new Color(glow.getRed(), glow.getGreen(), glow.getBlue(), Math.min(22, glow.getAlpha())));
            g2.fill(shape);
        }

        // 4) outline (glides to violet while hovered)
        if (borderColor != null) {
            g2.setColor(t > 0 ? Theme.mix(borderColor, hoverBorder, t) : borderColor);
            g2.setStroke(new BasicStroke(borderWidth));
            g2.draw(shape);
        }
        g2.dispose();
        super.paintComponent(g);
    }
}
