package tala.ui.components;

import javax.swing.Icon;
import javax.swing.JComponent;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/**
 * IconBox
 * ------------------------------------------------------------
 * A colored rounded square with a line icon in the middle
 * (the little squares next to card titles and on the stat tiles).
 */
public class IconBox extends JComponent {

    private final int boxSize;
    private Color background;
    private Icon icon;
    private final String iconName;
    private Color outline = null;

    public IconBox(String iconName, int boxSize, Color background, Color iconColor) {
        this.boxSize = boxSize;
        this.background = background;
        this.iconName = iconName;
        this.icon = Icons.get(iconName, iconSize(), iconColor);
        Dimension size = new Dimension(boxSize, boxSize);
        setPreferredSize(size);
        setMinimumSize(size);
        setMaximumSize(size);
    }

    private int iconSize() {
        return boxSize <= 30 ? 14 : boxSize <= 40 ? 17 : 26;
    }

    /** On a purple card header: a see-through white square with a yellow icon */
    public IconBox setOnDark() {
        background = new Color(255, 255, 255, 36);
        icon = Icons.get(iconName, iconSize(), tala.ui.theme.Theme.YELLOW);
        outline = new Color(255, 255, 255, 40);
        repaint();
        return this;
    }

    /** A hairline around the square, e.g. "inset 0 0 0 1px rgba(46,16,101,.08)" (null = none) */
    public IconBox setOutline(Color outline) {
        this.outline = outline;
        repaint();
        return this;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(background);
        int arc = boxSize <= 30 ? 14 : boxSize <= 40 ? 20 : 28;   // ~7-14px corner radius, like the design
        g2.fillRoundRect(0, 0, boxSize, boxSize, arc, arc);
        if (outline != null) {
            g2.setColor(outline);
            g2.setStroke(new BasicStroke(1f));
            g2.drawRoundRect(0, 0, boxSize - 1, boxSize - 1, arc, arc);
        }
        icon.paintIcon(this, g2, (boxSize - icon.getIconWidth()) / 2, (boxSize - icon.getIconHeight()) / 2);
        g2.dispose();
    }
}
