package com.abhishri.escape.ui;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.awt.GraphicsEnvironment;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PuzzleDialogBaseThemeTest {

    @Test
    void riddleDialog_contentPane_isDarkWood() {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "Requires display");
        RiddlePuzzleDialog d = new RiddlePuzzleDialog(null, "p1", "Question?");
        assertEquals(ThemeConstants.DARK_WOOD, d.getContentPane().getBackground());
        d.dispose();
    }

    @Test
    void combinationDialog_contentPane_isDarkWood() {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "Requires display");
        CombinationPuzzleDialog d = new CombinationPuzzleDialog(null, "p2", 3, "Code");
        assertEquals(ThemeConstants.DARK_WOOD, d.getContentPane().getBackground());
        d.dispose();
    }

    @Test
    void sequenceDialog_contentPane_isDarkWood() {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "Requires display");
        SequencePuzzleDialog d = new SequencePuzzleDialog(null, "p3", List.of("A", "B"), "Order");
        assertEquals(ThemeConstants.DARK_WOOD, d.getContentPane().getBackground());
        d.dispose();
    }
}
