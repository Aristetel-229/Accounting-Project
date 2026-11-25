package tala.ui.theme;

import tala.ui.components.Icons;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.plaf.basic.BasicComboPopup;
import javax.swing.plaf.basic.ComboPopup;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;

/**
 * ComboUI
 * ------------------------------------------------------------
 * A drop-down list in the light style: rounded white box, a small purple
 * chevron instead of the grey Windows button (it dips a little on hover),
 * and a white pop-up list whose rows light up lilac under the mouse.
 */
public class ComboUI extends BasicComboBoxUI {

    @Override
    protected JButton createArrowButton() {
        JButton arrow = new JButton(new javax.swing.Icon() {
            private final javax.swing.Icon chevron = Icons.get(Icons.CHEVRON, 16, Theme.PURPLE);
            @Override public int getIconWidth()  { return 16; }
            @Override public int getIconHeight() { return 16; }
            @Override public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                float drop = comboBox == null ? 0 : Hover.of(comboBox).value() * 1.5f;
                g2.translate(0, drop);
                g2.rotate(Math.PI / 2, x + 8, y + 8);   // the chevron points right; turn it down
                chevron.paintIcon(c, g2, x, y);
                g2.dispose();
            }
        });
        arrow.setContentAreaFilled(false);
        arrow.setBorderPainted(false);
        arrow.setFocusPainted(false);
        arrow.setOpaque(false);
        arrow.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 4));
        return arrow;
    }

    @Override
    public void installUI(JComponent c) {
        super.installUI(c);
        c.setOpaque(false);
        c.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
    }

    @Override
    public void paint(Graphics g, JComponent c) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Color fill = comboBox.isEnabled()
                ? Theme.mix(comboBox.getBackground(), new Color(0xfbf9ff), Hover.of(comboBox).value())
                : Theme.READ_ONLY_BG;
        g2.setColor(fill);
        int r = FieldBorder.RING;
        g2.fillRoundRect(r, r, c.getWidth() - r * 2 - 1, c.getHeight() - r * 2 - 1, 20, 20);
        g2.dispose();
        super.paint(g, c);
    }

    /** No square background behind the chosen value */
    @Override
    public void paintCurrentValueBackground(Graphics g, Rectangle bounds, boolean hasFocus) {
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void paintCurrentValue(Graphics g, Rectangle bounds, boolean hasFocus) {
        ListCellRenderer renderer = comboBox.getRenderer();
        Component shown = renderer.getListCellRendererComponent(listBox, comboBox.getSelectedItem(), -1, false, false);
        shown.setFont(comboBox.getFont());
        shown.setForeground(comboBox.isEnabled() ? comboBox.getForeground() : Theme.MUTED);
        if (shown instanceof JComponent) {
            ((JComponent) shown).setOpaque(false);
        }
        currentValuePane.paintComponent(g, shown, comboBox, bounds.x, bounds.y, bounds.width, bounds.height, false);
        if (shown instanceof JComponent) {
            ((JComponent) shown).setOpaque(true);   // the pop-up list needs it opaque again
        }
    }

    @Override
    protected ComboPopup createPopup() {
        BasicComboPopup popup = new BasicComboPopup(comboBox) {
            @Override
            protected void configureList() {
                super.configureList();
                list.setBackground(Color.WHITE);
                list.setForeground(Theme.TITLE);
                list.setSelectionBackground(Theme.PURPLE_TINT);
                list.setSelectionForeground(Theme.PURPLE_DEEP);
                list.setFont(comboBox.getFont());
            }
        };
        popup.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.FIELD_BORDER),
                BorderFactory.createEmptyBorder(4, 4, 4, 4)));
        popup.setBackground(Color.WHITE);
        return popup;
    }

    @Override
    protected ListCellRenderer<Object> createRenderer() {
        return new javax.swing.plaf.basic.BasicComboBoxRenderer.UIResource() {
            @Override
            @SuppressWarnings("rawtypes")
            public Component getListCellRendererComponent(JList list, Object value, int index,
                                                          boolean selected, boolean focused) {
                super.getListCellRendererComponent(list, value, index, selected, focused);
                setBorder(BorderFactory.createEmptyBorder(7, 8, 7, 8));
                return this;
            }
        };
    }
}
