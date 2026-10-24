package com.accountpro.ui.components;

import com.accountpro.ui.Theme;

import javax.swing.*;
import java.awt.*;

/** Sidebar menu button: highlights on hover and shows an accent bar when it is the active page. */
public class NavButton extends JButton {
    private boolean active;

    public NavButton(String text) {
        super(text);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setOpaque(false);
        setFont(Theme.BASE);
        setForeground(Color.WHITE);
        setHorizontalAlignment(SwingConstants.LEFT);
        setAlignmentX(Component.LEFT_ALIGNMENT);
        setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 8));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    public void setActive(boolean active) {
        this.active = active;
        setFont(active ? Theme.BOLD : Theme.BASE);
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        if (active || getModel().isRollover()) {
            g2.setColor(active ? Theme.NAVY_ACTIVE : Theme.NAVY_HOVER);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
        }
        if (active) {
            g2.setColor(Color.WHITE);
            g2.fillRoundRect(0, 8, 4, getHeight() - 16, 4, 4);
        }
        g2.dispose();
        super.paintComponent(g);
    }
}
