package com.abhishri.escape.ui;

import org.junit.jupiter.api.Test;
import javax.swing.SwingUtilities;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.*;

class ScenePanelHintCardTest {

    @Test
    void showHintCard_isVisibleAtFullAlpha() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            ScenePanel panel = new ScenePanel(new PlaceholderAssetManager());
            panel.showHintCard(new BufferedImage(320, 200, BufferedImage.TYPE_INT_ARGB));
            assertTrue(panel.isHintCardVisible());
            assertEquals(1.0f, panel.getHintCardAlpha(), 0.001f);
        });
    }

    @Test
    void showHintCard_timerIsRunning() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            ScenePanel panel = new ScenePanel(new PlaceholderAssetManager());
            panel.showHintCard(new BufferedImage(320, 200, BufferedImage.TYPE_INT_ARGB));
            assertTrue(panel.hintCardTimer.isRunning());
        });
    }

    @Test
    void showHintCard_calledTwice_resetsCleanly() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            ScenePanel panel = new ScenePanel(new PlaceholderAssetManager());
            panel.showHintCard(new BufferedImage(320, 200, BufferedImage.TYPE_INT_ARGB));
            panel.showHintCard(new BufferedImage(320, 200, BufferedImage.TYPE_INT_ARGB));
            assertTrue(panel.hintCardTimer.isRunning());
            assertTrue(panel.isHintCardVisible());
        });
    }
}
