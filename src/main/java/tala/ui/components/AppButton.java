package tala.ui.components;

import tala.ui.theme.Hover;
import tala.ui.theme.Theme;

import javax.swing.Icon;
import javax.swing.JButton;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;

/**
 * AppButton
 * ------------------------------------------------------------
 * A rounded button (10px corners, 40px tall) in the light design:
 *   PRIMARY   - yellow gradient (#fddd44 -> #f7af1a), dark purple text, golden glow
 *   SECONDARY - white with a lilac outline and purple text
 *   DANGER    - white with a soft red outline and red text (delete / reset)
 *   PURPLE    - deep purple gradient (#3b0f86 -> #6d28d9) with white text
 *
 * Hover effect: the button rises 1px, its shadow grows and its color
 * brightens (white buttons turn lilac). Pressing pushes it back down.
 * setTone(from, to) gives any button its own gradient (Admin cards).
 */
public class AppButton extends JButton {

    public enum Style { PRIMARY, SECONDARY, DANGER, PURPLE }

    private Style style;
    private Color toneFrom, toneTo;     // custom gradient (white text)
    private int radius = 10;
    private boolean compact = false;

    public AppButton(String text, Style style) {
        this(text, null, style);
    }

    public AppButton(String text, Icon icon, Style style) {
        super(text, icon);
        this.style = style;
        setFont(Theme.semibold(14));
        setIconTextGap(8);
        setFocusPainted(false);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setOpaque(false);
        setMargin(new Insets(0, 16, 0, 16));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setForeground(defaultForeground());
        Hover.of(this);
        getModel().addChangeListener(e -> repaint());
    }

    private Color defaultForeground() {
        if (toneFrom != null || style == Style.PURPLE) {
            return Color.WHITE;
        }
        switch (style) {
            case PRIMARY: return Theme.ON_YELLOW;
            case DANGER:  return Theme.RED;
            default:      return Theme.PURPLE_INK;
        }
    }

    public Style getStyle() {
        return style;
    }

    public void setStyle(Style style) {
        this.style = style;
        setForeground(defaultForeground());
        repaint();
    }

    /** A custom gradient with white text (used by the colored Admin cards) */
    public AppButton setTone(Color from, Color to) {
        this.toneFrom = from;
        this.toneTo = to;
        setForeground(Color.WHITE);
        repaint();
        return this;
    }

    /** Smaller button (34px) for card headers and table actions */
    public AppButton setCompact(boolean compact) {
        this.compact = compact;
        setFont(Theme.semibold(compact ? 13 : 14));
        setMargin(new Insets(0, compact ? 12 : 16, 0, compact ? 12 : 16));
        revalidate();
        return this;
    }

    /** Kept for older code: banner buttons now keep their own style */
    public void setBannerRole(int role) {
        // the light design shows each button in its own style (no forced yellow first button)
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension size = super.getPreferredSize();
        int h = compact ? 40 : 46;      // includes 2px lift space on top and 4px shadow below
        return new Dimension(size.width + 14, Math.max(h, size.height));
    }

    /** Never squeezed shorter than its normal height (BoxLayout would otherwise flatten it) */
    @Override
    public Dimension getMinimumSize() {
        return new Dimension(super.getMinimumSize().width, getPreferredSize().height);
    }

    private int topSpace()    { return 2; }
    private int bottomSpace() { return 4; }

    @Override
    public void paint(Graphics g) {
        // lift the whole button (background, icon and text) on hover
        float t = isEnabled() ? Hover.of(this).value() : 0f;
        boolean pressed = getModel().isPressed() && getModel().isArmed();
        float up = pressed ? -0.5f : t * 1.5f;
        Graphics2D g2 = (Graphics2D) g.create();
        g2.translate(0, -up);
        super.paint(g2);
        g2.dispose();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        float t = isEnabled() ? Hover.of(this).value() : 0f;
        boolean pressed = getModel().isPressed() && getModel().isArmed();
        float x = 1, y = topSpace();
        float w = getWidth() - 3, h = getHeight() - topSpace() - bottomSpace();
        float arc = radius * 2;
        RoundRectangle2D body = new RoundRectangle2D.Float(x, y, w, h, arc, arc);

        java.awt.Paint fill;
        Color border;
        Color glow;   // shadow color
        if (toneFrom != null) {
            fill = new GradientPaint(0, 0, Theme.mix(toneFrom, Color.WHITE, t * 0.12f), w, h,
                    Theme.mix(toneTo, Color.WHITE, t * 0.12f));
            border = null;
            glow = Theme.alpha(toneTo, 70);
        } else if (style == Style.PRIMARY) {      // yellow gradient, 135deg
            fill = new GradientPaint(0, 0, Theme.mix(Theme.YELLOW, new Color(0xfee26a), t), w, h,
                    Theme.mix(Theme.GOLD_DARK, new Color(0xf9bb2e), t));
            border = Theme.GOLD_EDGE;
            glow = new Color(247, 175, 26, 90);
        } else if (style == Style.PURPLE) {
            fill = new GradientPaint(0, 0, Theme.mix(new Color(0x3b0f86), new Color(0x4a16a3), t), w, h,
                    Theme.mix(new Color(0x6d28d9), new Color(0x7e3df0), t));
            border = null;
            glow = new Color(46, 16, 101, 80);
        } else if (style == Style.DANGER) {
            fill = Theme.mix(Color.WHITE, Theme.RED_BG, t);
            border = Theme.mix(Theme.RED_BORDER, new Color(0xe79aab), t);
            glow = new Color(180, 35, 60, 30);
        } else {
            fill = Theme.mix(Color.WHITE, Theme.PURPLE_HOVER, t);
            border = Theme.mix(Theme.QUIET_BORDER, Theme.CARD_HOVER_BORDER, t);
            glow = new Color(46, 16, 101, 32);
        }
        if (!isEnabled()) {
            fill = new Color(0xf4f2f8);
            border = Theme.DIVIDER;
            glow = null;
        }

        // shadow: "0 1px 2px, 0 4px 10px" growing to "0 2px 4px, 0 8px 18px" on hover
        if (glow != null && !pressed) {
            int layers = 4;
            for (int i = layers; i >= 1; i--) {
                float k = i / (float) layers;
                int a = Math.round(glow.getAlpha() * (0.18f + 0.22f * t) * (1f - k * 0.6f));
                g2.setColor(Theme.alpha(glow, a));
                float grow = k * (1f + t);
                g2.fill(new RoundRectangle2D.Float(x - grow, y + 1 + k * (2 + 2 * t), w + grow * 2, h,
                        arc + grow, arc + grow));
            }
        }

        g2.setPaint(fill);
        g2.fill(body);
        if (border != null) {
            g2.setColor(border);
            g2.setStroke(new BasicStroke(1.5f));
            g2.draw(body);
        }

        // keyboard focus ring
        if (isFocusOwner()) {
            g2.setColor(Theme.FOCUS);
            g2.setStroke(new BasicStroke(2f));
            g2.draw(new RoundRectangle2D.Float(x + 1, y + 1, w - 2, h - 2, arc, arc));
        }
        g2.dispose();

        // the text is centered inside the body (not the whole component)
        Graphics2D text = (Graphics2D) g.create();
        text.translate(0, (topSpace() - bottomSpace()) / 2.0);
        super.paintComponent(text);
        text.dispose();
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        setForeground(enabled ? (toneFrom != null ? Color.WHITE : defaultForeground()) : new Color(0xa39cb8));
    }
}
