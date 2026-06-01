package com.abhishri.escape.ui;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assumptions;

import java.awt.GraphicsEnvironment;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RiddlePuzzleDialogInputTest {

    @Test
    void riddleDialog_setAnswer_returnsAnswerMap() {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "Requires display");

        RiddlePuzzleDialog dialog = new RiddlePuzzleDialog(
                null, "puzzle_clock", "When does the library's silence begin?");
        dialog.getAnswerField().setText("11:47");

        assertEquals(Map.of("answer", "11:47"), dialog.getInputs());
        dialog.dispose();
    }
}
