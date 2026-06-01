package com.abhishri.escape.ui;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assumptions;

import java.awt.GraphicsEnvironment;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CombinationPuzzleDialogInputTest {

    @Test
    void combinationDialog_setSpinnersTo3_8_4_returnsCode384() {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "Requires display");

        CombinationPuzzleDialog dialog = new CombinationPuzzleDialog(
                null, "puzzle_display_case", 3, "Enter the combination");
        dialog.getSpinners()[0].setValue(3);
        dialog.getSpinners()[1].setValue(8);
        dialog.getSpinners()[2].setValue(4);

        assertEquals(Map.of("code", "384"), dialog.getInputs());
        dialog.dispose();
    }
}
