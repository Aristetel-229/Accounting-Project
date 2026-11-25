package tala.ui.components;

import tala.ui.theme.Hover;
import tala.ui.theme.Theme;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;

/**
 * Segmented
 * ------------------------------------------------------------
 * The light design's segmented control:
 *
 *   ( [All accounts 32]  Assets 16   Liabilities 5   Equity 2 ... )
 *
 * A white rounded bar with a lilac outline and a soft shadow. The selected
 * segment is a deep purple gradient with white text and a yellow count
 * bubble; the others are grey text with a lilac count bubble.
 *
 * Hover: a segment fades to a soft lilac and its text darkens.
 *
 *   Segmented.Bar bar = new Segmented.Bar();
 *   bar.add(new Segmented.Tab("All accounts"));    // a JButton: add listeners as usual
 */
public final class Segmented {

    private Segmented() { }

    /** The white rounded bar that holds the tabs */
    public static class Bar extends JPanel {
        public Bar() {
            this(false);
        }

        /** compact = tighter padding, for the small (30px) segments */
        public Bar(boolean compact) {
            super(new FlowLayout(FlowLayout.LEFT, compact ? 2 : 4, 0));
            setOpaque(false);
            setBorder(compact ? BorderFactory.createEmptyBorder(4, 4, 7, 4) : BorderFactory.createEmptyBorder(5, 5, 8, 5));
        }

        @Override
        public Dimension getMaximumSize() {
            return getPreferredSize();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth() - 3, h = getHeight() - 5;
            for (int i = 3; i >= 1; i--) {
                g2.setColor(new Color(46, 16, 101, 9));
                g2.fill(new RoundRectangle2D.Float(1 - i * 0.5f, 1 + i, w + i, h, 24 + i, 24 + i));
            }
            RoundRectangle2D box = new RoundRectangle2D.Float(1, 1, w, h, 24, 24);
            g2.setColor(Color.WHITE);
            g2.fill(box);
            g2.setColor(Theme.CARD_BORDER);
            g2.setStroke(new BasicStroke(1.5f));
            g2.draw(box);
            g2.dispose();
        }
    }

    /** One segment. count < 0 = no bubble. */
    public static class Tab extends JButton {
        private final String label;
        private int count = -1;
        private boolean on;
        private boolean compact;

        public Tab(String label) {
            this.label = label;
            setText(label);
            setFont(Theme.medium(13.5f));
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            getAccessibleContext().setAccessibleName(label);
            Hover.of(this);
        }

        /** Smaller segment (30px, 12px text), like the Chart of Accounts filter */
        public Tab setCompact(boolean compact) {
            this.compact = compact;
            revalidate();
            repaint();
            return this;
        }

        private float fs()  { return compact ? 12f : 13.5f; }
        private float cfs() { return compact ? 10.5f : 11.5f; }
        private int pad()   { return compact ? 10 : 14; }

        public void setCount(int count) {
            this.count = count;
            revalidate();
            repaint();
        }

        public int getCount() {
            return count;
        }

        public String getTabLabel() {
            return label;
        }

        @Override
        public void setSelected(boolean selected) {
            this.on = selected;
            getAccessibleContext().setAccessibleDescription(selected ? "selected" : null);
            revalidate();
            repaint();
        }

        @Override
        public boolean isSelected() {
            return on;
        }

        @Override
        public Dimension getPreferredSize() {
            FontMetrics fm = getFontMetrics(Theme.semibold(fs()));
            int w = pad() + fm.stringWidth(label) + pad();
            if (count >= 0) {
                w += 6 + getFontMetrics(Theme.semibold(cfs())).stringWidth(String.valueOf(count)) + (compact ? 10 : 14);
            }
            return new Dimension(w, compact ? 30 : 38);
        }

        @Override
        public Dimension getMinimumSize() {
            return getPreferredSize();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            float t = Hover.of(this).value();
            int w = getWidth(), h = getHeight();
            RoundRectangle2D shape = new RoundRectangle2D.Float(0, 0, w, h, 18, 18);
            if (on) {
                g2.setColor(new Color(91, 33, 182, 45));
                g2.fill(new RoundRectangle2D.Float(0, 2, w, h - 1, 18, 18));
                g2.setPaint(new GradientPaint(0, 0, Theme.mix(Theme.PURPLE_DARK, new Color(0x6a2ad0), t), w, h,
                        Theme.mix(Theme.VIOLET, new Color(0x8b4cf5), t)));
                g2.fill(shape);
            } else if (t > 0) {
                g2.setColor(Theme.alpha(new Color(0xf3eeff), Math.round(255 * t)));
                g2.fill(shape);
            }
            if (isFocusOwner()) {
                g2.setColor(on ? Theme.YELLOW : Theme.FOCUS);
                g2.setStroke(new BasicStroke(2f));
                g2.draw(new RoundRectangle2D.Float(1, 1, w - 2, h - 2, 18, 18));
            }

            g2.setFont(on ? Theme.semibold(fs()) : Theme.medium(fs()));
            FontMetrics fm = g2.getFontMetrics();
            int base = (h + fm.getAscent() - fm.getDescent()) / 2;
            g2.setColor(on ? Color.WHITE : Theme.mix(new Color(0x4a4266), Theme.TITLE, t));
            g2.drawString(label, pad(), base);

            if (count >= 0) {
                int x = pad() + fm.stringWidth(label) + 6;
                g2.setFont(Theme.semibold(cfs()));
                FontMetrics cfm = g2.getFontMetrics();
                String c = String.valueOf(count);
                int bh = compact ? 17 : 20;
                int bw = cfm.stringWidth(c) + (compact ? 10 : 14);
                g2.setColor(on ? Theme.YELLOW : Theme.mix(new Color(0xf4f1fa), new Color(0xe9e1fb), t));
                g2.fillRoundRect(x, (h - bh) / 2, bw, bh, bh, bh);
                g2.setColor(on ? Theme.PURPLE_DEEP : new Color(0x4a4266));
                g2.drawString(c, x + bw / 2f - cfm.stringWidth(c) / 2f, (h + cfm.getAscent() - cfm.getDescent()) / 2);
            }
            g2.dispose();
        }
    }

    /** Wraps any component so it sits at its preferred width on the left of a row */
    public static JComponent leftAligned(JComponent c) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        row.setOpaque(false);
        row.add(c);
        return row;
    }
}
