package tala.ui.theme;

import javax.swing.BorderFactory;
import javax.swing.ToolTipManager;
import javax.swing.UIManager;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.metal.DefaultMetalTheme;
import javax.swing.plaf.metal.MetalLookAndFeel;
import java.awt.Color;

/**
 * TalaLook
 * ------------------------------------------------------------
 * Gives Java's standard parts (dialogs, message boxes, drop-down lists,
 * check boxes, scroll bars, tooltips, the file chooser) the light TALA
 * colors too. Call TalaLook.install() once, before any window opens.
 *
 * It uses Java's built-in "Metal" look with our own colors.
 */
public final class TalaLook {

    private TalaLook() { }

    public static void install() {
        MetalLookAndFeel.setCurrentTheme(new TalaMetalTheme());
        try {
            UIManager.setLookAndFeel(new MetalLookAndFeel());
        } catch (Exception ignored) {
            // the default look is fine too
        }

        Color bg = Color.WHITE;
        put("Panel.background", bg);
        put("OptionPane.background", bg);
        put("OptionPane.messageForeground", Theme.TEXT);
        put("Label.foreground", Theme.TEXT);
        put("CheckBox.foreground", Theme.TEXT);
        put("CheckBox.background", bg);
        put("Button.background", new Color(0xf6f3fb));
        put("Button.foreground", Theme.PURPLE_INK);
        put("Button.select", new Color(0xe4dafa));
        put("Button.focus", Theme.FOCUS);
        put("Button.disabledText", new Color(0xa39cb8));
        put("ToggleButton.disabledText", new Color(0xa39cb8));
        put("Label.disabledForeground", new Color(0xa39cb8));
        put("CheckBox.disabledText", new Color(0xa39cb8));
        UIManager.put("Button.gradient", null);
        put("TextField.background", Theme.FIELD_BG);
        put("TextField.foreground", Theme.TITLE);
        put("TextField.caretForeground", Theme.PURPLE);
        put("PasswordField.background", Theme.FIELD_BG);
        put("PasswordField.foreground", Theme.TITLE);
        put("PasswordField.caretForeground", Theme.PURPLE);
        put("ComboBox.background", Theme.FIELD_BG);
        put("ComboBox.foreground", Theme.TITLE);
        put("ComboBox.selectionBackground", Theme.PURPLE_TINT);
        put("ComboBox.selectionForeground", Theme.PURPLE_DEEP);
        put("List.background", Theme.FIELD_BG);
        put("List.foreground", Theme.TITLE);
        put("List.selectionBackground", Theme.PURPLE_TINT);
        put("List.selectionForeground", Theme.PURPLE_DEEP);
        put("Table.background", Theme.FIELD_BG);
        put("Table.foreground", Theme.TEXT);
        put("ScrollPane.background", bg);
        put("Viewport.background", bg);
        put("ToolTip.background", new Color(0x2e1065));
        put("ToolTip.foreground", Color.WHITE);
        UIManager.put("ToolTip.border", BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0x4c1d95)),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)));
        UIManager.put("ScrollBarUI", TalaScrollBarUI.class.getName());

        // message-box icons: round badges in the TALA colors instead of Metal's old pictures
        UIManager.put("OptionPane.questionIcon", new BadgeIcon("?", Theme.VIOLET, Color.WHITE));
        UIManager.put("OptionPane.informationIcon", new BadgeIcon("i", Theme.VIOLET, Color.WHITE));
        UIManager.put("OptionPane.warningIcon", new BadgeIcon("!", Theme.YELLOW, Theme.PURPLE_DEEP));
        UIManager.put("OptionPane.errorIcon", new BadgeIcon("!", new Color(0xe11d48), Color.WHITE));

        FontUIResource text = new FontUIResource(Theme.regular(14.5f));
        FontUIResource strong = new FontUIResource(Theme.semibold(14));
        for (String key : new String[] {"Label.font", "OptionPane.messageFont", "TextField.font",
                "PasswordField.font", "ComboBox.font", "List.font", "Table.font", "CheckBox.font",
                "ToolTip.font", "TitledBorder.font", "FileChooser.listFont"}) {
            UIManager.put(key, text);
        }
        UIManager.put("Button.font", strong);
        UIManager.put("OptionPane.buttonFont", strong);
        UIManager.put("ToggleButton.font", strong);

        ToolTipManager.sharedInstance().setInitialDelay(250);   // show tips quickly
        ToolTipManager.sharedInstance().setDismissDelay(15000);
    }

    private static void put(String key, Color color) {
        UIManager.put(key, new ColorUIResource(color));
    }

    /** The colors Metal uses for its borders, buttons and selections */
    private static class TalaMetalTheme extends DefaultMetalTheme {
        private static final ColorUIResource P1 = new ColorUIResource(0x7c3aed);   // focus / active borders
        private static final ColorUIResource P2 = new ColorUIResource(0xc9b8f5);   // selection
        private static final ColorUIResource P3 = new ColorUIResource(0xefe8ff);
        private static final ColorUIResource S1 = new ColorUIResource(0xc9b8f5);   // borders
        private static final ColorUIResource S2 = new ColorUIResource(0xe4dafa);   // buttons
        private static final ColorUIResource S3 = new ColorUIResource(0xf7f4fd);   // window background

        @Override public String getName() { return "TALA light"; }
        @Override protected ColorUIResource getPrimary1()   { return P1; }
        @Override protected ColorUIResource getPrimary2()   { return P2; }
        @Override protected ColorUIResource getPrimary3()   { return P3; }
        @Override protected ColorUIResource getSecondary1() { return S1; }
        @Override protected ColorUIResource getSecondary2() { return S2; }
        @Override protected ColorUIResource getSecondary3() { return S3; }
        @Override protected ColorUIResource getWhite()      { return new ColorUIResource(Color.WHITE); }
        @Override protected ColorUIResource getBlack()      { return new ColorUIResource(Theme.TITLE); }
        @Override public ColorUIResource getControlTextColor() { return new ColorUIResource(Theme.TEXT); }
        @Override public ColorUIResource getSystemTextColor()  { return new ColorUIResource(Theme.TEXT); }
        @Override public ColorUIResource getUserTextColor()    { return new ColorUIResource(Theme.TITLE); }
        @Override public ColorUIResource getInactiveControlTextColor() { return new ColorUIResource(Theme.MUTED); }
        @Override public ColorUIResource getMenuForeground()   { return new ColorUIResource(Theme.TEXT); }
        @Override public ColorUIResource getHighlightedTextColor() { return new ColorUIResource(Theme.PURPLE_DEEP); }
        @Override public FontUIResource getControlTextFont()   { return new FontUIResource(Theme.semibold(14)); }
        @Override public FontUIResource getSystemTextFont()    { return new FontUIResource(Theme.regular(14.5f)); }
        @Override public FontUIResource getUserTextFont()      { return new FontUIResource(Theme.regular(14.5f)); }
        @Override public FontUIResource getMenuTextFont()      { return new FontUIResource(Theme.regular(14.5f)); }
        @Override public FontUIResource getWindowTitleFont()   { return new FontUIResource(Theme.bold(14)); }
        @Override public FontUIResource getSubTextFont()       { return new FontUIResource(Theme.regular(12.5f)); }
    }

    /** A round colored badge with one character, used as a message-box icon */
    private static class BadgeIcon implements javax.swing.Icon {
        private final String text;
        private final Color fill, ink;

        BadgeIcon(String text, Color fill, Color ink) {
            this.text = text;
            this.fill = fill;
            this.ink = ink;
        }

        @Override public int getIconWidth()  { return 40; }
        @Override public int getIconHeight() { return 40; }

        @Override
        public void paintIcon(java.awt.Component c, java.awt.Graphics g, int x, int y) {
            java.awt.Graphics2D g2 = (java.awt.Graphics2D) g.create();
            g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(java.awt.RenderingHints.KEY_TEXT_ANTIALIASING, java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setColor(fill);
            g2.fillOval(x, y, 40, 40);
            g2.setColor(ink);
            g2.setFont(Theme.bold(20));
            java.awt.FontMetrics fm = g2.getFontMetrics();
            g2.drawString(text, x + (40 - fm.stringWidth(text)) / 2, y + (40 + fm.getAscent() - fm.getDescent()) / 2);
            g2.dispose();
        }
    }
}
