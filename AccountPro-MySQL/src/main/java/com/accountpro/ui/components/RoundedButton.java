package com.accountpro.ui.components;

import com.accountpro.ui.Theme;

import javax.swing.*;
import java.awt.*;

/** Rounded button with hover and pressed colors and a hand cursor. */
public class RoundedButton extends JButton {
    public enum Kind { PRIMARY, SECONDARY, DANGER }

    private final Kind kind;

    public RoundedButton(String text) {
        this(text, Kind.PRIMARY);
    }

    public RoundedButton(String text, Kind kind) {
        super(text);
        this.kind = kind;
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setOpaque(false);
        setFont(Theme.BOLD);
        setForeground(kind == Kind.SECONDARY ? Theme.TEXT : Color.WHITE);
        setBorder(BorderFactory.createEmptyBorder(9, 18, 9, 18));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(currentColor());
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
        g2.dispose();
        super.paintComponent(g);
    }

    private Color currentColor() {
        ButtonModel m = getModel();
        if (!isEnabled()) return Theme.BORDER;
        boolean hover = m.isRollover();
        boolean pressed = m.isPressed();
        return switch (kind) {
            case PRIMARY -> pressed ? Theme.PRIMARY_PRESSED : hover ? Theme.PRIMARY_HOVER : Theme.PRIMARY;
            case DANGER -> pressed || hover ? Theme.DANGER_HOVER : Theme.DANGER;
            case SECONDARY -> pressed || hover ? Theme.SECONDARY_HOVER : Theme.SECONDARY;
        };
    }
}
