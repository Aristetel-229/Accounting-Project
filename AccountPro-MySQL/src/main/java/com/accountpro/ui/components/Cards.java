package com.accountpro.ui.components;

import com.accountpro.ui.Theme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/** Dashboard metric card with a colored accent bar and a hover effect. */
public final class Cards {
    private Cards() {}

    public static JPanel metric(String title, String hint, JLabel value, Color accent) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Color.WHITE);
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        card.setToolTipText(hint);

        JPanel bar = new JPanel();
        bar.setBackground(accent);
        bar.setPreferredSize(new Dimension(6, 0));

        JLabel t = new JLabel(title);
        t.setForeground(Theme.TEXT);
        t.setFont(Theme.BOLD);
        value.setFont(new Font("SansSerif", Font.BOLD, 26));
        value.setForeground(accent);
        JLabel h = new JLabel(hint);
        h.setForeground(Theme.MUTED);
        h.setFont(Theme.HINT);

        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        text.setBorder(new EmptyBorder(16, 18, 16, 18));
        for (JComponent c : new JComponent[]{t, value, h}) c.setAlignmentX(Component.LEFT_ALIGNMENT);
        text.add(t);
        text.add(Box.createVerticalStrut(8));
        text.add(value);
        text.add(Box.createVerticalStrut(6));
        text.add(h);

        card.add(bar, BorderLayout.WEST);
        card.add(text, BorderLayout.CENTER);

        setHover(card, accent, false);
        MouseAdapter hover = new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { setHover(card, accent, true); }
            @Override public void mouseExited(MouseEvent e) {
                // Moving onto a child label also fires mouseExited; only un-hover when truly outside.
                if (card.getMousePosition(true) == null) setHover(card, accent, false);
            }
        };
        addRecursively(card, hover);
        return card;
    }

    private static void setHover(JPanel card, Color accent, boolean hover) {
        card.setBorder(BorderFactory.createLineBorder(hover ? accent : Theme.BORDER, hover ? 2 : 1));
        card.setBackground(hover ? new Color(250, 252, 255) : Color.WHITE);
    }

    private static void addRecursively(Component c, MouseAdapter l) {
        c.addMouseListener(l);
        if (c instanceof Container container) {
            for (Component child : container.getComponents()) addRecursively(child, l);
        }
    }
}
