package com.tala.ui;

import java.awt.Color;
import java.awt.Font;

/** Shared UI colors and fonts. Change them here to re-skin the whole app. */
public final class Theme {
    private Theme() {}

    // Core green palette
    public static final Color GREEN_DARK = new Color(20, 83, 57);
    public static final Color GREEN_HOVER = new Color(31, 111, 76);
    public static final Color GREEN_ACTIVE = new Color(40, 130, 87);
    public static final Color GREEN_LIGHT = new Color(232, 244, 235);
    public static final Color BACKGROUND = new Color(243, 248, 244);
    public static final Color BORDER = new Color(218, 231, 220);
    public static final Color TEXT = new Color(37, 56, 43);
    public static final Color MUTED = new Color(103, 125, 108);
    public static final Color SIDEBAR_TEXT = new Color(204, 228, 211);

    // Actions
    public static final Color PRIMARY = new Color(34, 125, 79);
    public static final Color PRIMARY_HOVER = new Color(27, 101, 63);
    public static final Color PRIMARY_PRESSED = new Color(20, 83, 57);
    public static final Color SECONDARY = new Color(224, 237, 226);
    public static final Color SECONDARY_HOVER = new Color(204, 224, 207);
    public static final Color DANGER = new Color(185, 40, 40);
    public static final Color DANGER_HOVER = new Color(155, 28, 28);

    // Table colors
    public static final Color ROW_ALT = new Color(246, 250, 247);
    public static final Color ROW_HOVER = new Color(230, 242, 232);
    public static final Color ROW_SELECTED = new Color(198, 226, 203);
    public static final Color ROW_TOTAL = new Color(214, 234, 219);

    // Fonts
    public static final Font BASE = new Font("SansSerif", Font.PLAIN, 14);
    public static final Font BOLD = new Font("SansSerif", Font.BOLD, 14);
    public static final Font TITLE = new Font("SansSerif", Font.BOLD, 24);
    public static final Font HINT = new Font("SansSerif", Font.PLAIN, 13);
}
