package com.accountpro.ui.components;

import com.accountpro.ui.Theme;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

/** Text fields that highlight their border when focused. */
public final class Fields {
    private Fields() {}

    public static JTextField text() {
        return text("");
    }

    public static JTextField text(String initial) {
        JTextField field = new JTextField(initial);
        field.setFont(Theme.BASE);
        field.setBorder(border(Theme.BORDER));
        field.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) { field.setBorder(border(Theme.PRIMARY)); }
            @Override public void focusLost(FocusEvent e) { field.setBorder(border(Theme.BORDER)); }
        });
        return field;
    }

    private static Border border(java.awt.Color color) {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(color, 1),
                BorderFactory.createEmptyBorder(6, 8, 6, 8));
    }
}
