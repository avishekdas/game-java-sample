package com.abhishri.escape.ui;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assumptions;

import javax.swing.DefaultListModel;
import java.awt.GraphicsEnvironment;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SequencePuzzleDialogInputTest {

    @Test
    void sequenceDialog_reorderToCorrectSequence_returnsCommaSeparatedString() {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "Requires display");

        List<String> shuffled = List.of("810", "520", "720", "510", "610", "621");
        SequencePuzzleDialog dialog = new SequencePuzzleDialog(
                null, "puzzle_bookshelf", shuffled, "Arrange the books in Dewey Decimal order");

        DefaultListModel<String> model = dialog.getListModel();
        model.clear();
        List.of("510", "520", "610", "621", "720", "810").forEach(model::addElement);

        assertEquals(Map.of("sequence", "510,520,610,621,720,810"), dialog.getInputs());
        dialog.dispose();
    }
}
