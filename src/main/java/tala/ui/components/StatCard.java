package tala.ui.components;

import tala.ui.theme.Theme;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;

/**
 * StatCard
 * ------------------------------------------------------------
 * A summary tile (light design, "tinted tiles"):
 *
 *   [icon]  Total users
 *           6
 *
 * The tile fades from white to a soft tint of its color (purple, yellow,
 * green, blue or red) with an outline in the same color; the icon sits
 * in a white rounded square.
 *
 * Looks: NORMAL (tinted), HIGHLIGHT (deep purple gradient, yellow number)
 * and CREAM (yellow tint).
 *
 * Hover: the tile rises 2px, its shadow deepens and the outline glows;
 * a tooltip explains how the number was worked out.
 */
public class StatCard extends RoundedPanel {

    public enum Look { NORMAL, HIGHLIGHT, CREAM }

    private final JLabel valueLabel = new JLabel("-");
    private final JLabel noteLabel = new JLabel();

    public StatCard(String iconName, Color iconBg, Color iconColor, String title, String note, Look look) {
        super(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(RoundedPanel.TOP + 12, RoundedPanel.SIDE + 16,
                RoundedPanel.BOTTOM + 12, RoundedPanel.SIDE + 16));
        setRadius(14);

        Color[] tone = toneOf(iconColor, look);   // { tint, edge, ink }
        Color titleColor = Theme.TEXT_SOFT;
        Color valueColor = Theme.TITLE;
        Color noteColor = Theme.MUTED;
        Color boxBg = Color.WHITE;
        Color boxInk = tone[2];

        if (look == Look.HIGHLIGHT) {
            setFill(new Color(0x5b21b6)).setGradient(new Color(0x7c3aed), 0f);
            setBorderColor(new Color(0x4c1d95));
            titleColor = new Color(0xede6ff);
            valueColor = Theme.YELLOW;
            noteColor = new Color(0xd9ccfb);
            boxBg = new Color(255, 255, 255, 40);
            boxInk = Color.WHITE;
        } else {
            setFill(Color.WHITE).setGradient(tone[0], 0f);
            setBorderColor(tone[1]);
        }
        setHoverable(tone[1]);
        setLift(true);

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(Theme.medium(12.5f));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 6));   // room for the last glyph
        titleLabel.setForeground(titleColor);

        valueLabel.setFont(Theme.bold(19));
        valueLabel.setForeground(valueColor);
        valueLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 8));   // room for the last glyph

        noteLabel.setText(note);
        noteLabel.setFont(Theme.regular(12.5f));
        noteLabel.setForeground(noteColor);

        JPanel column = new JPanel();
        column.setOpaque(false);
        column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        valueLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        noteLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        column.add(titleLabel);
        column.add(valueLabel);
        column.add(noteLabel);
        noteLabel.setVisible(note != null && !note.isEmpty());

        JPanel iconHolder = new JPanel(new java.awt.GridBagLayout());
        iconHolder.setOpaque(false);
        IconBox box = new IconBox(iconName, 36, boxBg, boxInk);
        box.setOutline(look == Look.HIGHLIGHT ? null : new Color(46, 16, 101, 20));
        iconHolder.add(box);

        setLayout(new BorderLayout(12, 0));
        add(iconHolder, BorderLayout.WEST);
        add(column, BorderLayout.CENTER);
    }

    /** { tile tint, outline, icon color } for the color the page asked for */
    private static Color[] toneOf(Color c, Look look) {
        if (look == Look.CREAM || Theme.AMBER.equals(c) || Theme.YELLOW.equals(c) || Theme.CREDIT.equals(c)) {
            return new Color[] {Theme.TINT_YELLOW, Theme.EDGE_YELLOW, Theme.TONE_YELLOW};
        }
        if (Theme.GREEN.equals(c)) {
            return new Color[] {Theme.TINT_GREEN, Theme.EDGE_GREEN, Theme.TONE_GREEN};
        }
        if (Theme.BLUE.equals(c)) {
            return new Color[] {Theme.TINT_BLUE, Theme.EDGE_BLUE, Theme.TONE_BLUE};
        }
        if (Theme.RED.equals(c)) {
            return new Color[] {Theme.TINT_RED, Theme.EDGE_RED, Theme.TONE_RED};
        }
        return new Color[] {Theme.TINT_PURPLE, Theme.EDGE_PURPLE, Theme.TONE_PURPLE};
    }

    public void setValue(String value) {
        valueLabel.setText(value);
        // long values (e.g. "Jan. - Sep. 2026") get a slightly smaller font so they fit the tile
        valueLabel.setFont(Theme.bold(value != null && value.length() > 14 ? 16 : 19));
    }

    public void setNote(String note) {
        noteLabel.setText(note);
        noteLabel.setVisible(note != null && !note.isEmpty());
    }

    /** The explanation shown when the mouse rests on the tile (simple HTML allowed) */
    public void setTip(String html) {
        setToolTipText("<html>" + html + "</html>");
    }
}
