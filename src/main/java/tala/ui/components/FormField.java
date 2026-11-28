package tala.ui.components;

import tala.ui.theme.Theme;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Component;
import java.awt.Dimension;

/**
 * FormField
 * ------------------------------------------------------------
 * A label on top, the input under it, and an optional small hint:
 *
 *   Amount (P)
 *   [ 0.00            ]
 *   Must be above zero
 */
public class FormField extends JPanel {

    private final WrapLabel hintLabel = new WrapLabel("");
    private final JLabel label;

    public FormField(String labelText, JComponent input, String hint) {
        setOpaque(false);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        // small sentence-case label (12.5px, semibold, ink #4a4266), like the light design
        label = new JLabel(labelText);
        label.setFont(Theme.semibold(12.5f));
        label.setForeground(Theme.LABEL);
        label.setLabelFor(input);              // screen readers link the label to the input
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        label.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 3, 0, 0));   // lines up with the box

        input.setAlignmentX(Component.LEFT_ALIGNMENT);
        input.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));

        hintLabel.setFont(Theme.regular(12f));
        hintLabel.setForeground(Theme.MUTED);
        hintLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        hintLabel.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 3, 0, 0));
        setHint(hint);

        add(label);
        add(Box.createVerticalStrut(2));
        add(input);
        add(Box.createVerticalStrut(1));
        add(hintLabel);
    }

    /** The label color (light on a purple card header) */
    public void setLabelColor(java.awt.Color color) {
        label.setForeground(color);
    }

    public void setHint(String hint) {
        hintLabel.setText(hint);
        hintLabel.setVisible(hint != null);   // no hint = no empty line
    }
}
