package tala.ui.theme;

import javax.swing.plaf.basic.BasicPasswordFieldUI;
import javax.swing.plaf.basic.BasicTextFieldUI;
import javax.swing.text.JTextComponent;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/**
 * FieldUI
 * ------------------------------------------------------------
 * Text boxes with a ROUNDED white background (a normal text box paints a
 * square background, which would show past the rounded border).
 * Theme.styleField(...) puts this on every text box.
 */
public final class FieldUI {

    private FieldUI() { }

    public static BasicTextFieldUI text() {
        return new BasicTextFieldUI() {
            @Override
            protected void paintSafely(Graphics g) {
                paintRounded(getComponent(), g);
                super.paintSafely(g);
                paintExtras(getComponent(), g);
            }
        };
    }

    public static BasicPasswordFieldUI password() {
        return new BasicPasswordFieldUI() {
            @Override
            protected void paintSafely(Graphics g) {
                paintRounded(getComponent(), g);
                super.paintSafely(g);
            }
        };
    }

    /** The grey placeholder text (while empty) and the magnifier of search boxes */
    static void paintExtras(JTextComponent field, Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        Object hint = field.getClientProperty(Theme.PLACEHOLDER);
        if (hint != null && field.getDocument().getLength() == 0) {
            g2.setFont(field.getFont());
            g2.setColor(new java.awt.Color(0xa39cb8));
            java.awt.Insets in = field.getInsets();
            java.awt.FontMetrics fm = g2.getFontMetrics();
            int y = (field.getHeight() + fm.getAscent() - fm.getDescent()) / 2;
            g2.drawString(hint.toString(), in.left, y);
        }
        g2.dispose();
    }

    /** Fills the rounded shape inside the focus-ring space; the field itself is see-through */
    static void paintRounded(JTextComponent field, Graphics g) {
        field.setOpaque(false);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(field.isEditable() && field.isEnabled() ? field.getBackground() : Theme.READ_ONLY_BG);
        int r = FieldBorder.RING;
        g2.fillRoundRect(r, r, field.getWidth() - r * 2 - 1, field.getHeight() - r * 2 - 1, 20, 20);
        g2.dispose();
    }
}
