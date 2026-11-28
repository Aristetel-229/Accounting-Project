package tala.ui.theme;

import tala.util.Money;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.border.Border;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GraphicsEnvironment;

/**
 * Theme
 * ------------------------------------------------------------
 * All colors and fonts of the app in one place.
 *
 * The look is the LIGHT TALA design (from the "TALA Screens (Light)"
 * design canvas): a soft lavender-to-cream page, a deep purple
 * "ledger book" sidebar, white cards with lilac outlines, purple text
 * accents and one bright yellow accent for the active page and the
 * main buttons. The font is Arial on every page (it comes with Windows,
 * so nothing has to be installed).
 *
 * Want a different look? Change the values here only.
 */
public final class Theme {

    private Theme() { }

    // ---------------- Brand colors ----------------
    public static final Color NAV_BG        = new Color(0x2e1065);  // sidebar (deep purple ledger book)
    public static final Color NAV_TOP       = new Color(0x26094f);  // sidebar gradient: top
    public static final Color NAV_BOTTOM    = new Color(0x3b1578);  // sidebar gradient: bottom
    public static final Color NAV_TEXT      = new Color(0xd6cbf2);  // sidebar links
    public static final Color NAV_MUTED     = new Color(0xc9b8f5);  // "Accounting System", section labels
    public static final Color YELLOW        = new Color(0xfddd44);  // active tab, highlights
    public static final Color YELLOW_SOFT   = new Color(0xfde68a);
    public static final Color VIOLET        = new Color(0x7c3aed);  // FILLS: switches, chips, focus
    public static final Color PURPLE        = new Color(0x6d28d9);  // TEXT and icons on white
    public static final Color PURPLE_DARK   = new Color(0x5b21b6);
    public static final Color PURPLE_DEEP   = new Color(0x2e1065);  // dark text on yellow
    public static final Color PURPLE_BRIGHT = new Color(0x8b5cf6);
    public static final Color PURPLE_INK    = new Color(0x4c1d95);  // table heads, quiet button text
    public static final Color PURPLE_TINT   = new Color(0xefe8ff);  // pills, icon boxes
    public static final Color PURPLE_HOVER  = new Color(0xf3eeff);  // hover on white
    public static final Color FOCUS         = new Color(0x7c3aed);
    public static final Color FOCUS_RING    = new Color(124, 58, 237, 38);   // rgba(124,58,237,.15)
    public static final Color MAGENTA       = new Color(0xd946ef);

    // ---------------- Surfaces ----------------
    public static final Color PAGE_BG       = new Color(0xf7f4fd);  // the soft lavender page
    public static final Color CARD_TOP      = Color.WHITE;
    public static final Color CARD_BOTTOM   = Color.WHITE;
    public static final Color CARD_BG       = Color.WHITE;
    public static final Color CARD_BORDER   = new Color(0xd9cff3);  // lilac outline (1.5px)
    public static final Color FIELD_BG      = Color.WHITE;          // text boxes and lists
    public static final Color READ_ONLY_BG  = new Color(0xf6f3fb);  // fields you cannot type in
    public static final Color DIVIDER       = new Color(0xebe5f6);
    public static final Color ROW_LINE      = new Color(0xf0edf6);
    public static final Color FIELD_BORDER  = new Color(0xd6cbf2);
    public static final Color FIELD_HOVER   = new Color(0xb9a3f3);
    public static final Color FOOTER_BG     = new Color(0xfaf8fd);
    public static final Color CREAM         = new Color(0xfff8dc);  // total rows
    public static final Color CREAM_BORDER  = new Color(0xf1d77a);
    public static final Color CREAM_LINE    = new Color(0xecd27a);
    public static final Color SELECTED_ROW  = new Color(0xede7fb);
    public static final Color NEW_ROW       = new Color(0xfff8dc);
    public static final Color GOLD_GLOW     = new Color(0xfff4d1);  // tip card, period box
    public static final Color HEADER_GLOW   = new Color(0xfffaeb);  // right end of card headers

    // ---------------- Text ----------------
    public static final Color TITLE         = new Color(0x231b3a);
    public static final Color TEXT          = new Color(0x2a2240);
    public static final Color TEXT_SOFT     = new Color(0x5c5577);
    public static final Color MUTED         = new Color(0x7a7392);
    public static final Color FAINT         = new Color(0x8a83a3);  // page description under the title
    public static final Color LABEL         = new Color(0x4a4266);  // small form labels
    public static final Color TABLE_HEAD    = new Color(0x3b1578);

    // ---------------- Status colors (soft background, dark text) ----------------
    public static final Color GREEN_BG = new Color(0xe8f6ee), GREEN = new Color(0x1f7a45);
    public static final Color AMBER_BG = new Color(0xfdf4d6), AMBER = new Color(0x8a6200);
    public static final Color RED_BG   = new Color(0xfdecef), RED   = new Color(0xb4233c);
    public static final Color BLUE_BG  = new Color(0xe9effd), BLUE  = new Color(0x2f56c4);
    public static final Color RED_BORDER = new Color(0xf1c3cc);
    public static final Color GREEN_BAR  = new Color(0x22a06b);
    public static final Color PURPLE_BAR = new Color(0x7c3aed);

    // ---------------- Light-design extras ----------------
    public static final Color HEAD_BG       = new Color(0xe6dbff);  // table headers (--head)
    public static final Color SUB_HEAD_BG   = new Color(0xf6f2ff);  // second header row (worksheet)
    public static final Color HEAD_LINE     = new Color(0xd3c3f3);  // line under table headers
    public static final Color GRAND_BG      = new Color(0xfbf7e6);  // grand-total rows
    public static final Color ROW_HOVER     = new Color(0xe7dbfb);  // rgba(109,40,217,.10) on the card tint

    // ---------------- Purple card look (design: "all cards purple") ----------------
    public static final Color CARD_TINT     = new Color(0xf4efff);  // card body (--body)
    public static final Color CARD_EDGE     = new Color(0xd4c4f0);  // card outline on the tint
    public static final Color ZEBRA_ROW     = new Color(0xece4fd);  // every 2nd table row (--row)
    public static final Color FOOT_TINT     = new Color(0xf7f3fe);  // card footer strip
    public static final Color FOOT_LINE     = new Color(0xe1d4f7);  // line above the footer
    public static final Color HEAD_DARK_1   = new Color(0x2e0f6b);  // card header gradient: start
    public static final Color HEAD_DARK_2   = new Color(0x4c1d95);  //                        middle
    public static final Color HEAD_DARK_3   = new Color(0x6d28d9);  //                        end
    public static final Color ON_DARK_SUB   = new Color(0xd6c8fa);  // subtitles on the purple header
    public static final Color ON_DARK_LABEL = new Color(0xe3d6ff);  // small labels on the purple header
    public static final Color FOOT_TEXT     = new Color(0x6b6280);  // footer text (numbers are bold #2e1065)
    public static final Color CARD_HOVER_BORDER = new Color(0xa78bfa);
    public static final Color ICON_BG       = new Color(0xf4f1fa);  // icon squares on tiles
    public static final Color CRUMB         = new Color(0x6d4fc9);
    public static final Color HEADING       = new Color(0x231b3a);  // card titles
    public static final Color DEBIT         = new Color(0x6d4fc9);  // debit amounts
    public static final Color CREDIT        = new Color(0xa8740c);  // credit amounts
    public static final Color NIL           = new Color(0xc9c3d8);  // empty worksheet cells
    public static final Color GOLD_DARK     = new Color(0xf7af1a);  // end of the yellow button gradient
    public static final Color GOLD_EDGE     = new Color(0xf2c230);  // border of yellow buttons
    public static final Color ON_YELLOW     = new Color(0x2e1065);  // text on yellow buttons
    public static final Color QUIET_BORDER  = new Color(0xc9b8f5);  // border of white buttons
    // tile icon tints
    public static final Color TONE_PURPLE = new Color(0x6d4fc9), TONE_YELLOW = new Color(0xa8740c),
            TONE_GREEN = new Color(0x1f8a4c), TONE_RED = new Color(0xc2334d), TONE_BLUE = new Color(0x3b5fd0);
    // tile backgrounds (white -> tint) and their outlines
    public static final Color TINT_PURPLE = new Color(0xf1ebff), EDGE_PURPLE = new Color(0xc9b8f5);
    public static final Color TINT_YELLOW = new Color(0xfff4d1), EDGE_YELLOW = new Color(0xecd27a);
    public static final Color TINT_GREEN  = new Color(0xe6f7ec), EDGE_GREEN  = new Color(0xa9dcbc);
    public static final Color TINT_RED    = new Color(0xfdebee), EDGE_RED    = new Color(0xefb9c4);
    public static final Color TINT_BLUE   = new Color(0xe9effd), EDGE_BLUE   = new Color(0xb4c9f5);

    // ---------------- Font: Arial ----------------
    /** The app's font. Arial comes with Windows; if it is missing, a normal sans-serif font is used. */
    public static final String FONT_FAMILY = pickFamily();

    private static String pickFamily() {
        for (String name : GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()) {
            if (name.equalsIgnoreCase("Arial")) {
                return name;
            }
        }
        return Font.SANS_SERIF;
    }

    /**
     * Ready-made fonts by weight and size. Making a font is slow, and many
     * parts ask for one every time they are painted, so each is made once.
     */
    private static final java.util.Map<String, Font> FONTS = new java.util.concurrent.ConcurrentHashMap<>();

    private static Font cached(String key, java.util.function.Supplier<Font> maker) {
        Font f = FONTS.get(key);
        if (f == null) {
            f = maker.get();
            FONTS.put(key, f);
        }
        return f;
    }

    public static Font font(int style, float size) {
        return cached((style == Font.BOLD ? "b" : "r") + size,
                () -> new Font(FONT_FAMILY, style, 1).deriveFont(style, size));
    }

    public static Font regular(float size)  { return font(Font.PLAIN, size); }
    public static Font bold(float size)     { return font(Font.BOLD, size); }

    /** Medium weight (tabs, labels, table text). Arial has no medium, so this is regular Arial. */
    public static Font medium(float size) {
        return regular(size);
    }

    /** Semibold weight (small labels, headings). Arial has no semibold, so this is bold Arial. */
    public static Font semibold(float size) {
        return bold(size);
    }

    /** A monospaced font, for reference numbers like JE-7322 */
    public static Font mono(float size) {
        return cached("mono" + size, () -> new Font(Font.MONOSPACED, Font.BOLD, 1).deriveFont(Font.BOLD, size));
    }

    /**
     * Small UPPERCASE labels. With Poppins these had a little extra space
     * between letters; Arial is snapped to whole pixels, and that extra space
     * rounded into uneven gaps ("PO STED"), so Arial is used as it is.
     */
    public static Font spaced(Font font, float tracking) {
        return font;
    }

    /** Call once at start-up: uses "PHP " if the font has no peso sign. */
    public static void checkPesoSign() {
        if (!regular(15).canDisplay('₱')) {
            Money.PESO_SIGN = "PHP ";
        }
    }

    /** The same color with a different alpha (0-255) */
    public static Color alpha(Color c, int alpha) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), Math.max(0, Math.min(255, alpha)));
    }

    /** Mixes two colors: t = 0 gives a, t = 1 gives b (used by the hover animations) */
    public static Color mix(Color a, Color b, float t) {
        t = Math.max(0f, Math.min(1f, t));
        return new Color(
                Math.round(a.getRed() + (b.getRed() - a.getRed()) * t),
                Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * t),
                Math.round(a.getAlpha() + (b.getAlpha() - a.getAlpha()) * t));
    }

    // ---------------- Form field styling ----------------

    /**
     * Rounded border with inner padding for text fields. The border also
     * reacts to the mouse and the keyboard (see FieldBorder): lilac normally,
     * darker lilac on hover, violet with a soft ring while typing.
     */
    public static Border fieldBorder(Color color) {
        return new FieldBorder();
    }

    /** Gives a text field the TALA look (and the hover / focus effects) */
    public static void styleField(JTextField field) {
        field.setUI(field instanceof JPasswordField ? FieldUI.password() : FieldUI.text());
        field.setFont(regular(14f));
        field.setForeground(TITLE);
        field.setBackground(FIELD_BG);
        field.setDisabledTextColor(MUTED);
        field.setCaretColor(PURPLE);
        field.setSelectionColor(new Color(0xddd0fb));
        field.setSelectedTextColor(TITLE);
        field.setBorder(new FieldBorder());
        field.setPreferredSize(new Dimension(160, 44));
        FieldBorder.watch(field);
    }

    public static void styleCombo(JComboBox<?> combo) {
        combo.setUI(new ComboUI());
        combo.setFont(regular(14f));
        combo.setForeground(TITLE);
        combo.setBackground(FIELD_BG);
        combo.setBorder(new FieldBorder());
        combo.setPreferredSize(new Dimension(160, 44));
        FieldBorder.watch(combo);
    }

    /** Client-property keys: grey text shown while a field is empty, and a magnifier for search boxes */
    public static final String PLACEHOLDER = "tala.placeholder", SEARCH_ICON = "tala.searchIcon";

    /** Grey text shown inside an empty text box, e.g. "e.g. 1340" */
    public static void setPlaceholder(JTextField field, String text) {
        field.putClientProperty(PLACEHOLDER, text);
        field.repaint();
    }

    /** A search box: magnifier on the left and a placeholder */
    public static void makeSearch(JTextField field, String placeholder) {
        field.putClientProperty(SEARCH_ICON, Boolean.TRUE);
        setPlaceholder(field, placeholder);
        field.revalidate();
    }

    /** Kept for older code: fields now repaint their own focus color */
    static void repaintLater(final JComponent field) {
        field.repaint();
    }
}
