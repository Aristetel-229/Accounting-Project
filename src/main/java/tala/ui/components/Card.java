package tala.ui.components;

import tala.ui.theme.Theme;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;

/**
 * Card
 * ------------------------------------------------------------
 * A white rounded card with three parts (light design):
 *
 *   +-------------------------------------------+
 *   | o Title                      [right side] |   <- header: lilac -> white -> cream strip,
 *   |   subtitle                                |      a small violet-to-yellow dot before the title
 *   |-------------------------------------------|
 *   | body (a table, a form, ...)               |   <- body
 *   |-------------------------------------------|
 *   | footer (optional totals)                  |   <- footer
 *   +-------------------------------------------+
 *
 * Cards glide to a violet outline and a deeper shadow under the mouse.
 */
public class Card extends RoundedPanel {

    private final JLabel titleLabel = new JLabel();
    private final JLabel subtitleLabel = new JLabel();
    private final JPanel headerRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
    private final JPanel headerLeft = new JPanel(new BorderLayout(12, 0));
    private final JPanel header;

    public Card(String title, String subtitle) {
        super(new BorderLayout());
        setHoverable(null);

        titleLabel.setText(title);
        titleLabel.setFont(Theme.semibold(16));
        titleLabel.setForeground(Theme.HEADING);
        titleLabel.setIcon(new DotIcon());
        titleLabel.setIconTextGap(10);
        subtitleLabel.setFont(Theme.regular(13));
        subtitleLabel.setForeground(Theme.MUTED);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 6));      // room for the last glyph
        subtitleLabel.setBorder(BorderFactory.createEmptyBorder(0, 18, 0, 6)); // lines up with the title text
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
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.DIVIDER),
                BorderFactory.createEmptyBorder(14, 20, 14, 20)));
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
        subtitleLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 6));
        headerLeft.add(iconBox, BorderLayout.WEST);
    }

    /** Puts an icon square to the left and keeps the small dot before the title */
    public void setHeaderIconWithDot(JComponent iconBox) {
        JPanel holder = new JPanel(new java.awt.GridBagLayout());
        holder.setOpaque(false);
        holder.add(iconBox);
        headerLeft.add(holder, BorderLayout.WEST);
    }

    /** A smaller title (14.5px), for compact side cards */
    public void setCompactTitle() {
        titleLabel.setFont(Theme.semibold(14.5f));
        subtitleLabel.setFont(Theme.regular(12));
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.DIVIDER),
                BorderFactory.createEmptyBorder(11, 14, 11, 14)));
    }

    /** Adds buttons, badges or search boxes to the right side of the header */
    public void addToHeader(JComponent component) {
        headerRight.add(component);
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
        strip.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.DIVIDER));
        add(strip, BorderLayout.SOUTH);
    }

    /** The card's outline in a child's coordinates (to clip the header / footer to the round corners) */
    private Shape shapeFor(Component child) {
        return AffineTransform.getTranslateInstance(-child.getX(), -child.getY()).createTransformedShape(cardShape());
    }

    /** Header background: linear-gradient(90deg, #f5f0ff 0%, #ffffff 60%, #fffaeb 100%) */
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
            int w = Math.max(1, getWidth());
            g2.setPaint(new LinearGradientPaint(0, 0, w, 0, new float[] {0f, 0.6f, 1f},
                    new Color[] {new Color(0xf5f0ff), Color.WHITE, Theme.HEADER_GLOW}));
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.dispose();
        }
    }

    /** Footer background: #faf8fd, clipped to the round bottom corners */
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
            g2.setColor(Theme.FOOTER_BG);
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.dispose();
        }
    }

    /** The 8px violet-to-yellow dot in front of card titles */
    static class DotIcon implements Icon {
        @Override public int getIconWidth()  { return 8; }
        @Override public int getIconHeight() { return 8; }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setPaint(new GradientPaint(x, y, Theme.VIOLET, x + 8, y + 8, Theme.YELLOW));
            g2.fillOval(x, y, 8, 8);
            g2.dispose();
        }
    }
}
