package com.accountpro.ui;

import java.awt.Color;
import java.awt.Font;

/** Shared UI colors and fonts. Change them here to re-skin the whole app. */
public final class Theme {
    private Theme() {}

    // Core palette
    public static final Color NAVY = new Color(24, 43, 75);
    public static final Color NAVY_HOVER = new Color(40, 66, 110);
    public static final Color NAVY_ACTIVE = new Color(59, 100, 168);
    public static final Color BACKGROUND = new Color(245, 247, 251);
    public static final Color BORDER = new Color(225, 230, 238);
    public static final Color TEXT = new Color(45, 55, 72);
    public static final Color MUTED = new Color(110, 122, 142);

    // Actions
    public static final Color PRIMARY = new Color(37, 99, 235);
    public static final Color PRIMARY_HOVER = new Color(29, 78, 216);
    public static final Color PRIMARY_PRESSED = new Color(30, 64, 175);
    public static final Color SECONDARY = new Color(226, 232, 244);
    public static final Color SECONDARY_HOVER = new Color(205, 215, 235);
    public static final Color DANGER = new Color(185, 40, 40);
    public static final Color DANGER_HOVER = new Color(155, 28, 28);

    // Table colors
    public static final Color ROW_ALT = new Color(248, 250, 253);
    public static final Color ROW_HOVER = new Color(232, 240, 254);
    public static final Color ROW_SELECTED = new Color(200, 218, 252);

    // Dashboard accents
    public static final Color ASSET = new Color(37, 99, 235);
    public static final Color LIABILITY = new Color(234, 138, 20);
    public static final Color REVENUE = new Color(22, 163, 74);
    public static final Color EXPENSE = new Color(220, 38, 38);

    // Fonts
    public static final Font BASE = new Font("SansSerif", Font.PLAIN, 14);
    public static final Font BOLD = new Font("SansSerif", Font.BOLD, 14);
    public static final Font TITLE = new Font("SansSerif", Font.BOLD, 24);
    public static final Font HINT = new Font("SansSerif", Font.PLAIN, 13);
}
