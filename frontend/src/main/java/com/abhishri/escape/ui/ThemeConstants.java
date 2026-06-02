package com.abhishri.escape.ui;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import java.awt.Color;
import java.awt.Font;

public final class ThemeConstants {

    private ThemeConstants() {}

    // --- Color palette (design_ui_upgrade.md §3.2) ---

    public static final Color NIGHT_BLACK    = new Color(0x0D, 0x0B, 0x07);
    public static final Color DARK_WOOD      = new Color(0x1A, 0x14, 0x08);
    public static final Color AGED_BRASS     = new Color(0x8B, 0x69, 0x14);
    public static final Color BRASS_GLOW     = new Color(0xC8, 0xA8, 0x4B);
    public static final Color PARCHMENT      = new Color(0xF0, 0xDF, 0xA0);
    public static final Color PARCHMENT_TEXT = new Color(0x2A, 0x1F, 0x0A);
    public static final Color CANDLE_TEXT    = new Color(0xE8, 0xD5, 0xA3);
    public static final Color DIM_TEXT       = new Color(0x70, 0x60, 0x40);
    public static final Color SOLVED_OVERLAY = new Color(0x1A, 0x3A, 0x1A);
    public static final Color EXIT_OVERLAY   = new Color(0x0A, 0x1A, 0x2A);
    public static final Color SELECTION_GLOW = new Color(0xC8, 0xA8, 0x4B);
    public static final Color ERROR_TEXT     = new Color(0xC8, 0x40, 0x40);

    // --- Font palette (design_ui_upgrade.md §3.3) ---

    public static final Font FONT_TITLE     = new Font("Georgia",   Font.BOLD,  15);
    public static final Font FONT_BODY      = new Font("Georgia",   Font.PLAIN, 13);
    public static final Font FONT_LABEL     = new Font("SansSerif", Font.BOLD,  11);
    public static final Font FONT_BUTTON    = new Font("Georgia",   Font.BOLD,  12);
    public static final Font FONT_INVENTORY = new Font("SansSerif", Font.PLAIN, 12);
    public static final Font FONT_SMALL     = new Font("SansSerif", Font.PLAIN, 10);

    // --- Utility ---

    public static void applyDarkButton(JButton b) {
        b.setFont(FONT_BUTTON);
        b.setBackground(DARK_WOOD);
        b.setForeground(CANDLE_TEXT);
        b.setBorder(BorderFactory.createLineBorder(AGED_BRASS, 1));
        b.setFocusPainted(false);
        b.setOpaque(true);
    }
}
