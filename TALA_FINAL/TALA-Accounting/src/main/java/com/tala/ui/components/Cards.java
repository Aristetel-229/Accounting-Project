package com.tala.ui.components;

import com.tala.ui.Theme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/** Dashboard metric card with a colored accent bar. */
public final class Cards {
    private Cards() {}

    public static JPanel metric(String title, String hint, JLabel value, Color accent) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createLineBorder(Theme.BORDER, 1));

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
        text.setBorder(new EmptyBorder(14, 18, 14, 18));
        for (JComponent c : new JComponent[]{t, value, h}) c.setAlignmentX(Component.LEFT_ALIGNMENT);
        text.add(t);
        text.add(Box.createVerticalStrut(6));
        text.add(value);
        text.add(Box.createVerticalStrut(4));
        text.add(h);

        card.add(bar, BorderLayout.WEST);
        card.add(text, BorderLayout.CENTER);
        return card;
    }
}
