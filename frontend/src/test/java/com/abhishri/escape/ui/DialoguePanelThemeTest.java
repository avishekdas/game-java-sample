package com.abhishri.escape.ui;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import javax.swing.SwingUtilities;
import java.awt.GraphicsEnvironment;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DialoguePanelThemeTest {

    @Test
    void textArea_hasParchemntBackground() throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "Requires display");

        DialoguePanel[] ref = {null};
        SwingUtilities.invokeAndWait(() -> ref[0] = new DialoguePanel());
        DialoguePanel panel = ref[0];

        assertEquals(ThemeConstants.PARCHMENT,      panel.getTextArea().getBackground());
        assertEquals(ThemeConstants.PARCHMENT_TEXT, panel.getTextArea().getForeground());
        assertEquals(ThemeConstants.FONT_BODY,      panel.getTextArea().getFont());
    }
}
