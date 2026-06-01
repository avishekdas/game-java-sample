package com.abhishri.escape.ui;

import org.junit.jupiter.api.Test;
import javax.swing.SwingUtilities;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class MainFrameHintCardPreRenderTest {

    @Test
    void hintCardImages_hasFourKeys() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            MainFrame frame = new MainFrame(new ProceduralAssetManager());
            assertEquals(4, frame.getHintCardImages().size());
            assertTrue(frame.getHintCardImages().keySet().containsAll(
                    Set.of("wall_clock", "reception_desk", "reading_lamp", "filing_cabinets")));
            frame.dispose();
        });
    }

    @Test
    void shownHintCards_isEmptyAtConstruction() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            MainFrame frame = new MainFrame(new ProceduralAssetManager());
            assertTrue(frame.getShownHintCards().isEmpty());
            frame.dispose();
        });
    }
}
