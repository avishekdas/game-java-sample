package com.abhishri.escape.ui;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assumptions;

import javax.swing.SwingUtilities;
import java.awt.GraphicsEnvironment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MainFrameSmokeTest {

    @Test
    void mainFrame_isVisible_andHasFourPanels() throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(),
                "Skipping Swing test in headless environment");

        MainFrame[] frameRef = new MainFrame[1];
        SwingUtilities.invokeAndWait(() -> {
            frameRef[0] = new MainFrame(new PlaceholderAssetManager());
            frameRef[0].setVisible(true);
        });

        MainFrame frame = frameRef[0];
        assertTrue(frame.isVisible());
        assertEquals(4, frame.getContentPane().getComponentCount());

        SwingUtilities.invokeAndWait(frame::dispose);
    }
}
