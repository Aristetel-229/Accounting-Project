package tala.ui.components;

import tala.ui.theme.Theme;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.text.JTextComponent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;

/**
 * Card
 * ------------------------------------------------------------
 * A rounded card with three parts (light design, "all cards purple"):
 *
 *   +-------------------------------------------+
 *   | o Title                      [right side] |   <- header: deep purple gradient
 *   |   subtitle                                |      (#2e0f6b -> #4c1d95 -> #6d28d9) with a
 *   |-------------------------------------------|      soft yellow glow, white title, yellow dot
 *   | body (a table, a form, ...)               |   <- body: soft lavender (#f4efff)
 *   |-------------------------------------------|
 *   | footer (optional totals)                  |   <- footer: a lighter lavender strip
 *   +-------------------------------------------+
 *
 * Things placed in the header (badges, labels, links, icon squares)
 * switch to their "on purple" colors automatically.
 * Cards glide to a violet outline and a deeper shadow under the mouse.
 */
public class Card extends RoundedPanel {

    /** Client property set on everything inside a purple header */
    public static final String ON_DARK = "tala.onDark";

    private final JLabel titleLabel = new JLabel();
    private final JLabel subtitleLabel = new JLabel();
    private final JPanel headerRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
    private final JPanel headerLeft = new JPanel(new BorderLayout(12, 0));
    private final JPanel header;
    private boolean darkHeader = true;

    public Card(String title, String subtitle) {
        super(new BorderLayout());
        setHoverable(null);
        setRadius(14);
        setFill(Theme.CARD_TINT);
        setBorderColor(Theme.CARD_EDGE);
        setBorderWidth(1.2f);

        titleLabel.setText(title);
        titleLabel.setFont(Theme.semibold(15.5f));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setIcon(new DotIcon(this));
        titleLabel.setIconTextGap(10);
        subtitleLabel.setFont(Theme.regular(12.5f));
        subtitleLabel.setForeground(Theme.ON_DARK_SUB);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 6));      // room for the last glyph
        subtitleLabel.setBorder(BorderFactory.createEmptyBorder(1, 18, 0, 6)); // lines up with the title text
        setSubtitle(subtitle);

        JPanel titles = new JPanel();
        titles.setLayout(new BoxLayout(titles, BoxLayout.Y_AXIS));
        titles.setOpaque(false);
        titles.add(titleLabel);
        titles.add(subtitleLabel);

        headerLeft.setOpaque(false);
        headerLeft.add(titles, BorderLayout.CENTER);
        headerRight.setOpaque(false);

        header = new HeaderStrip(new BorderLayout(12, 0));
        header.setBorder(BorderFactory.createEmptyBorder(14, 20, 14, 20));
        header.add(headerLeft, BorderLayout.CENTER);
        header.add(headerRight, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);
    }

    /** A card without a header (for small info cards) */
    public static Card plain() {
        Card card = new Card("", null);
        card.remove(card.header);
        return card;
    }

    /**
     * The older light header (white -> lilac, dark title) and a white body,
     * for cards the design keeps light (e.g. the entry preview).
     */
    public Card setLightHeader() {
        darkHeader = false;
        setFill(Color.WHITE);
        setBorderColor(Theme.CARD_BORDER);
        titleLabel.setForeground(Theme.HEADING);
        subtitleLabel.setForeground(Theme.MUTED);
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.DIVIDER),
                BorderFactory.createEmptyBorder(14, 20, 14, 20)));
        repaint();
        return this;
    }

    public boolean hasDarkHeader() {
        return darkHeader;
    }

    public void setTitle(String title) {
        titleLabel.setText(title);
    }

    public void setSubtitle(String subtitle) {
        subtitleLabel.setText(subtitle == null ? "" : subtitle);
        subtitleLabel.setVisible(subtitle != null && !subtitle.isEmpty());
    }

    /** Puts an icon square to the left of the title (it replaces the small dot) */
    public void setHeaderIcon(JComponent iconBox) {
        titleLabel.setIcon(null);
        subtitleLabel.setBorder(BorderFactory.createEmptyBorder(1, 0, 0, 6));
        headerLeft.add(iconBox, BorderLayout.WEST);
        adapt(iconBox);
    }

    /** Puts an icon square to the left and keeps the small dot before the title */
    public void setHeaderIconWithDot(JComponent iconBox) {
        JPanel holder = new JPanel(new java.awt.GridBagLayout());
        holder.setOpaque(false);
        holder.add(iconBox);
        headerLeft.add(holder, BorderLayout.WEST);
        adapt(iconBox);
    }

    /** A smaller title (14.5px), for compact side cards */
    public void setCompactTitle() {
        titleLabel.setFont(Theme.semibold(14.5f));
        subtitleLabel.setFont(Theme.regular(12));
        header.setBorder(darkHeader
                ? BorderFactory.createEmptyBorder(11, 14, 11, 14)
                : BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.DIVIDER),
                        BorderFactory.createEmptyBorder(11, 14, 11, 14)));
    }

    /** Adds buttons, badges or search boxes to the right side of the header */
    public void addToHeader(JComponent component) {
        headerRight.add(component);
        adapt(component);
    }

    /** Adds something right after the title text (e.g. a count badge) */
    public void addNextToTitle(JComponent component) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        row.setOpaque(false);
        JPanel titles = (JPanel) titleLabel.getParent();
        titles.remove(titleLabel);
        row.add(titleLabel);
        row.add(Box.createHorizontalStrut(10));
        row.add(component);
        row.setAlignmentX(LEFT_ALIGNMENT);
        subtitleLabel.setAlignmentX(LEFT_ALIGNMENT);
        titles.add(row, 0);
        adapt(component);
    }

    /** Switches badges, labels, links and icon squares to their "on purple" colors */
    private void adapt(Component c) {
        if (darkHeader) {
            onDark(c);
        }
    }

    /** Marks a component (and what is inside it) as sitting on a purple header */
    public static void onDark(Component c) {
        if (c instanceof JComponent) {
            ((JComponent) c).putClientProperty(ON_DARK, Boolean.TRUE);
        }
        if (c instanceof Badge) {
            ((Badge) c).setOnDark(true);
            return;
        }
        if (c instanceof IconBox) {
            ((IconBox) c).setOnDark();
            return;
        }
        if (c instanceof FormField) {
            ((FormField) c).setLabelColor(Theme.ON_DARK_LABEL);
            return;
        }
        if (c instanceof JTextComponent || c instanceof JComboBox || c instanceof AppButton) {
            return;   // inputs and buttons keep their own white look
        }
        if (c instanceof JLabel) {
            ((JLabel) c).setForeground(Theme.ON_DARK_LABEL);
        }
        if (c instanceof Container) {
            for (Component child : ((Container) c).getComponents()) {
                onDark(child);
            }
        }
    }

    public void setBody(JComponent body) {
        add(body, BorderLayout.CENTER);
    }

    /** A soft lavender strip at the bottom of the card (for totals and status badges) */
    public void setFooter(JComponent footer) {
        JPanel strip = new FooterStrip(new BorderLayout());
        footer.setOpaque(false);
        footer.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        strip.add(footer, BorderLayout.CENTER);
        strip.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, darkHeader ? Theme.FOOT_LINE : Theme.DIVIDER));
        add(strip, BorderLayout.SOUTH);
    }

    /** The card's outline in a child's coordinates (to clip the header / footer to the round corners) */
    private Shape shapeFor(Component child) {
        return AffineTransform.getTranslateInstance(-child.getX(), -child.getY()).createTransformedShape(cardShape());
    }

    /**
     * The purple header background (also used by other cards with the same header):
     * linear-gradient(120deg, #2e0f6b 0%, #4c1d95 55%, #6d28d9 100%)
     * plus radial-gradient(260px 120px at 100% 0%, rgba(253,221,68,.18), transparent 70%)
     */
    public static void paintPurpleHeader(Graphics2D g2, int x, int y, int w, int h) {
        w = Math.max(1, w);
        h = Math.max(1, h);
        g2.setPaint(new LinearGradientPaint(x, y, x + w, y + Math.max(1, h * 1.4f), new float[] {0f, 0.55f, 1f},
                new Color[] {Theme.HEAD_DARK_1, Theme.HEAD_DARK_2, Theme.HEAD_DARK_3}));
        g2.fillRect(x, y, w, h);
        // the soft yellow glow in the top-right corner (an ellipse 260 x 120)
        Graphics2D glow = (Graphics2D) g2.create();
        glow.translate(x + w, y);
        glow.scale(1.0, 120.0 / 260.0);
        glow.setPaint(new RadialGradientPaint(new Point2D.Float(0, 0), 260f, new float[] {0f, 0.7f, 1f},
                new Color[] {new Color(253, 221, 68, 46), new Color(253, 221, 68, 0), new Color(253, 221, 68, 0)}));
        glow.fillRect(-260, 0, 260, 260);
        glow.dispose();
    }

    /** Header background: purple (or the older light strip) */
    private class HeaderStrip extends JPanel {
        HeaderStrip(java.awt.LayoutManager layout) {
            super(layout);
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.clip(shapeFor(this));
            if (darkHeader) {
                paintPurpleHeader(g2, 0, 0, getWidth(), getHeight());
            } else {
                int w = Math.max(1, getWidth());
                g2.setPaint(new LinearGradientPaint(0, 0, w, 0, new float[] {0f, 0.6f, 1f},
                        new Color[] {new Color(0xf5f0ff), Color.WHITE, Theme.HEADER_GLOW}));
                g2.fillRect(0, 0, getWidth(), getHeight());
            }
            g2.dispose();
        }
    }

    /** Footer background, clipped to the round bottom corners */
    private class FooterStrip extends JPanel {
        FooterStrip(java.awt.LayoutManager layout) {
            super(layout);
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.clip(shapeFor(this));
            g2.setColor(darkHeader ? Theme.FOOT_TINT : Theme.FOOTER_BG);
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.dispose();
        }
    }

    /** The 8px dot in front of card titles: yellow on purple, violet-to-yellow on the light header */
    static class DotIcon implements Icon {
        private final Card card;

        DotIcon(Card card) {
            this.card = card;
        }

        @Override public int getIconWidth()  { return 8; }
        @Override public int getIconHeight() { return 8; }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (card == null || card.darkHeader) {
                g2.setColor(Theme.YELLOW);
            } else {
                g2.setPaint(new java.awt.GradientPaint(x, y, Theme.VIOLET, x + 8, y + 8, Theme.YELLOW));
            }
            g2.fillOval(x, y, 8, 8);
            g2.dispose();
        }
    }
}
