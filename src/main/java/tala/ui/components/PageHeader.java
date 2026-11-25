package tala.ui.components;

import tala.ui.theme.Theme;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;

/**
 * PageHeader
 * ------------------------------------------------------------
 * The top of every page in the light design - no banner picture, just:
 *
 *   Journal Entry                                  [ Refresh ] [ Add ... ]
 *   One-line explanation of the page (soft grey)
 *
 * Title: 24px bold, deep ink. Description: 13.5px, #8a83a3, at most ~560px wide.
 * The action buttons sit on the right and keep their own style
 * (white, yellow or red), exactly like the design.
 * (iconName is kept so the pages did not need to change.)
 */
public class PageHeader extends JPanel {

    private final WrapLabel description = new WrapLabel("");
    private final JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));

    public PageHeader(String iconName, String title, String descriptionText) {
        super(new BorderLayout(16, 0));
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(Theme.bold(24));
        titleLabel.setForeground(Theme.TITLE);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 2, 8));   // right: room for the last glyph

        description.setFont(Theme.regular(13.5f));
        description.setForeground(Theme.FAINT);
        setDescription(descriptionText);

        JPanel texts = new JPanel() {
            // the description stays short-lined (max-width: 72ch in the design)
            @Override
            public Dimension getMaximumSize() {
                return new Dimension(580, super.getMaximumSize().height);
            }
        };
        texts.setOpaque(false);
        texts.setLayout(new BoxLayout(texts, BoxLayout.Y_AXIS));
        for (JComponent c : new JComponent[] {titleLabel, description}) {
            c.setAlignmentX(Component.LEFT_ALIGNMENT);
            texts.add(c);
        }

        JPanel left = new JPanel(new BorderLayout()) {
            @Override
            public Dimension getPreferredSize() {
                Dimension d = super.getPreferredSize();
                return new Dimension(Math.min(580, d.width), d.height);
            }
        };
        left.setOpaque(false);
        left.add(texts, BorderLayout.CENTER);
        add(left, BorderLayout.CENTER);

        actions.setOpaque(false);
        JPanel right = new JPanel(new GridBagLayout());   // keeps the buttons vertically centered
        right.setOpaque(false);
        right.add(actions);
        JPanel rightTop = new JPanel(new BorderLayout());
        rightTop.setOpaque(false);
        rightTop.add(right, BorderLayout.CENTER);   // buttons centered on the whole title block
        add(rightTop, BorderLayout.EAST);
    }

    public void setDescription(String text) {
        description.setText(text);   // WrapLabel wraps long text onto more lines
    }

    public void addAction(JComponent button) {
        actions.add(button);
    }
}
