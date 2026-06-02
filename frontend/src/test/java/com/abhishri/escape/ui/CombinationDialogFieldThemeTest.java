package com.abhishri.escape.ui;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import javax.swing.JSpinner;
import java.awt.GraphicsEnvironment;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CombinationDialogFieldThemeTest {

    @Test
    void spinners_haveParchmentBackground() {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "Requires display");
        CombinationPuzzleDialog d = new CombinationPuzzleDialog(null, "p1", 3, "Code");
        for (JSpinner spinner : d.getSpinners()) {
            assertEquals(ThemeConstants.PARCHMENT,      spinner.getBackground(),
                    "Spinner background must be PARCHMENT");
            assertEquals(ThemeConstants.PARCHMENT_TEXT, spinner.getForeground(),
                    "Spinner foreground must be PARCHMENT_TEXT");
        }
        d.dispose();
    }

    // C3 regression gate — existing input test must pass unchanged
    @Test
    void spinners_getInputs_stillWorks() {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "Requires display");
        CombinationPuzzleDialog d = new CombinationPuzzleDialog(null, "puzzle_display_case", 3, "Code");
        d.getSpinners()[0].setValue(3);
        d.getSpinners()[1].setValue(8);
        d.getSpinners()[2].setValue(4);
        assertEquals(Map.of("code", "384"), d.getInputs());
        d.dispose();
    }
}
