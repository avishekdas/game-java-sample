package com.abhishri.escape.ui;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.awt.GraphicsEnvironment;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RiddleDialogFieldThemeTest {

    @Test
    void answerField_hasParchemntBackground() {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "Requires display");
        RiddlePuzzleDialog d = new RiddlePuzzleDialog(null, "p1", "Question?");
        assertEquals(ThemeConstants.PARCHMENT,      d.getAnswerField().getBackground());
        assertEquals(ThemeConstants.PARCHMENT_TEXT, d.getAnswerField().getForeground());
        d.dispose();
    }

    // C3 regression gate — existing input test must pass unchanged
    @Test
    void answerField_getInputs_stillWorks() {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "Requires display");
        RiddlePuzzleDialog d = new RiddlePuzzleDialog(null, "puzzle_clock", "Question?");
        d.getAnswerField().setText("11:47");
        assertEquals(Map.of("answer", "11:47"), d.getInputs());
        d.dispose();
    }
}
