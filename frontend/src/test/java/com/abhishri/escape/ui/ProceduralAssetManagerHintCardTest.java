package com.abhishri.escape.ui;

import org.junit.jupiter.api.Test;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.*;

class ProceduralAssetManagerHintCardTest {

    private final ProceduralAssetManager manager = new ProceduralAssetManager();

    @Test
    void wallClockCard_hasCorrectDimensions() {
        BufferedImage img = (BufferedImage) manager.getHintCard("wall_clock");
        assertNotNull(img);
        assertEquals(320, img.getWidth());
        assertEquals(200, img.getHeight());
        assertEquals(BufferedImage.TYPE_INT_ARGB, img.getType());
    }

    @Test
    void receptionDeskCard_hasCorrectDimensions() {
        BufferedImage img = (BufferedImage) manager.getHintCard("reception_desk");
        assertNotNull(img);
        assertEquals(320, img.getWidth());
        assertEquals(200, img.getHeight());
        assertEquals(BufferedImage.TYPE_INT_ARGB, img.getType());
    }

    @Test
    void readingLampCard_hasCorrectDimensions() {
        BufferedImage img = (BufferedImage) manager.getHintCard("reading_lamp");
        assertNotNull(img);
        assertEquals(320, img.getWidth());
        assertEquals(200, img.getHeight());
        assertEquals(BufferedImage.TYPE_INT_ARGB, img.getType());
    }

    @Test
    void filingCabinetsCard_hasCorrectDimensions() {
        BufferedImage img = (BufferedImage) manager.getHintCard("filing_cabinets");
        assertNotNull(img);
        assertEquals(320, img.getWidth());
        assertEquals(200, img.getHeight());
        assertEquals(BufferedImage.TYPE_INT_ARGB, img.getType());
    }

    @Test
    void clockCard_and_nailsCard_centerPixelDiffers() {
        // Clock card has AGED_BRASS circle stroke at (90, 95) — west edge of the face
        // Nails card has DARK_WOOD base at (90, 95) — no shape drawn there
        BufferedImage clock = (BufferedImage) manager.getHintCard("wall_clock");
        BufferedImage nails = (BufferedImage) manager.getHintCard("reception_desk");
        assertNotEquals(clock.getRGB(90, 95), nails.getRGB(90, 95));
    }

    @Test
    void unknownKey_returnsGracefulFallback() {
        BufferedImage img = (BufferedImage) manager.getHintCard("unknown_object");
        assertNotNull(img);
        assertEquals(320, img.getWidth());
        assertEquals(200, img.getHeight());
    }
}
