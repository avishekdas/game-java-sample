package com.abhishri.escape.ui;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.awt.GraphicsEnvironment;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class SequenceDialogFieldThemeTest {

    @Test
    void list_hasDarkWoodBackgroundAndBrassSelectionBackground() {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "Requires display");
        SequencePuzzleDialog d = new SequencePuzzleDialog(null, "p1", List.of("A", "B"), "Order");
        assertNotNull(d.getList(), "getList() must return the JList field");
        assertEquals(ThemeConstants.DARK_WOOD,  d.getList().getBackground());
        assertEquals(ThemeConstants.BRASS_GLOW, d.getList().getSelectionBackground());
        d.dispose();
    }

    // C3 regression gate — existing input test must pass unchanged
    @Test
    void list_getInputs_stillWorks() {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "Requires display");
        SequencePuzzleDialog d = new SequencePuzzleDialog(
                null, "puzzle_bookshelf",
                List.of("810", "520", "720", "510", "610", "621"),
                "Order");
        d.getListModel().clear();
        List.of("510", "520", "610", "621", "720", "810").forEach(d.getListModel()::addElement);
        assertEquals(Map.of("sequence", "510,520,610,621,720,810"), d.getInputs());
        d.dispose();
    }
}
