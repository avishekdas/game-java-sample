package com.abhishri.escape.ui;

import org.junit.jupiter.api.Test;

import javax.swing.JButton;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ThemeConstantsTest {

    @Test
    void allColorConstantsAreNonNull() {
        assertNotNull(ThemeConstants.NIGHT_BLACK,    "NIGHT_BLACK");
        assertNotNull(ThemeConstants.DARK_WOOD,      "DARK_WOOD");
        assertNotNull(ThemeConstants.AGED_BRASS,     "AGED_BRASS");
        assertNotNull(ThemeConstants.BRASS_GLOW,     "BRASS_GLOW");
        assertNotNull(ThemeConstants.PARCHMENT,      "PARCHMENT");
        assertNotNull(ThemeConstants.PARCHMENT_TEXT, "PARCHMENT_TEXT");
        assertNotNull(ThemeConstants.CANDLE_TEXT,    "CANDLE_TEXT");
        assertNotNull(ThemeConstants.DIM_TEXT,       "DIM_TEXT");
        assertNotNull(ThemeConstants.SOLVED_OVERLAY, "SOLVED_OVERLAY");
        assertNotNull(ThemeConstants.EXIT_OVERLAY,   "EXIT_OVERLAY");
        assertNotNull(ThemeConstants.SELECTION_GLOW, "SELECTION_GLOW");
        assertNotNull(ThemeConstants.ERROR_TEXT,     "ERROR_TEXT");

        // verify they are Color instances (compile-time guarantee, but explicit intent)
        assertNotNull(ThemeConstants.NIGHT_BLACK.getClass().cast(ThemeConstants.NIGHT_BLACK));
    }

    @Test
    void allFontConstantsAreNonNull() {
        assertNotNull(ThemeConstants.FONT_TITLE,     "FONT_TITLE");
        assertNotNull(ThemeConstants.FONT_BODY,      "FONT_BODY");
        assertNotNull(ThemeConstants.FONT_LABEL,     "FONT_LABEL");
        assertNotNull(ThemeConstants.FONT_BUTTON,    "FONT_BUTTON");
        assertNotNull(ThemeConstants.FONT_INVENTORY, "FONT_INVENTORY");
        assertNotNull(ThemeConstants.FONT_SMALL,     "FONT_SMALL");
    }

    @Test
    void applyDarkButton_setsExpectedFontBackgroundForeground() {
        JButton btn = new JButton("Test");
        ThemeConstants.applyDarkButton(btn);

        assertEquals(ThemeConstants.FONT_BUTTON, btn.getFont());
        assertEquals(ThemeConstants.DARK_WOOD,   btn.getBackground());
        assertEquals(ThemeConstants.CANDLE_TEXT, btn.getForeground());
    }
}
