package com.abhishri.escape.ui;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProceduralAssetManagerTest {

    private ProceduralAssetManager manager;

    @BeforeEach
    void setUp() {
        manager = new ProceduralAssetManager();
    }

    // --- Room backgrounds ---

    @Test
    void getBackground_foyer_returns800x500RgbImage() {
        BufferedImage img = (BufferedImage) manager.getBackground("room_foyer");
        assertNotNull(img);
        assertEquals(800, img.getWidth());
        assertEquals(500, img.getHeight());
        assertEquals(BufferedImage.TYPE_INT_RGB, img.getType());
    }

    @Test
    void getBackground_readingHall_returns800x500RgbImage() {
        BufferedImage img = (BufferedImage) manager.getBackground("room_reading_hall");
        assertNotNull(img);
        assertEquals(800, img.getWidth());
        assertEquals(500, img.getHeight());
        assertEquals(BufferedImage.TYPE_INT_RGB, img.getType());
    }

    @Test
    void getBackground_archives_returns800x500RgbImage() {
        BufferedImage img = (BufferedImage) manager.getBackground("room_archives");
        assertNotNull(img);
        assertEquals(800, img.getWidth());
        assertEquals(500, img.getHeight());
        assertEquals(BufferedImage.TYPE_INT_RGB, img.getType());
    }

    @Test
    void getBackground_foyerAndArchives_areNotPixelIdentical() {
        BufferedImage foyer   = (BufferedImage) manager.getBackground("room_foyer");
        BufferedImage archives = (BufferedImage) manager.getBackground("room_archives");

        // At least one pixel in the center column must differ (different structural silhouettes)
        int centerX = 400;
        boolean foundDifference = false;
        for (int y = 0; y < 500; y++) {
            if (foyer.getRGB(centerX, y) != archives.getRGB(centerX, y)) {
                foundDifference = true;
                break;
            }
        }
        assertTrue(foundDifference, "room_foyer and room_archives must differ in center column pixels");
    }

    @Test
    void getBackground_unknownRoom_returnsNonNull800x500Image() {
        BufferedImage img = (BufferedImage) manager.getBackground("unknown_room");
        assertNotNull(img);
        assertEquals(800, img.getWidth());
        assertEquals(500, img.getHeight());
    }

    // --- Item icons ---

    @Test
    void getItemIcon_key_returns64x64ArgbImage() {
        BufferedImage img = (BufferedImage) manager.getItemIcon("item_key");
        assertNotNull(img);
        assertEquals(64, img.getWidth());
        assertEquals(64, img.getHeight());
        assertEquals(BufferedImage.TYPE_INT_ARGB, img.getType());
    }

    @Test
    void getItemIcon_allFiveKeys_return64x64ArgbImages() {
        for (String key : new String[]{"item_key", "item_lens", "item_scrap", "item_manuscript", "item_token"}) {
            BufferedImage img = (BufferedImage) manager.getItemIcon(key);
            assertNotNull(img, "icon for " + key + " must not be null");
            assertEquals(64, img.getWidth(),  key + " width");
            assertEquals(64, img.getHeight(), key + " height");
            assertEquals(BufferedImage.TYPE_INT_ARGB, img.getType(), key + " type");
        }
    }

    @Test
    void getItemIcon_keyAndLens_differAtCenterPixel() {
        BufferedImage key  = (BufferedImage) manager.getItemIcon("item_key");
        BufferedImage lens = (BufferedImage) manager.getItemIcon("item_lens");

        assertNotEquals(key.getRGB(32, 32), lens.getRGB(32, 32),
                "item_key and item_lens must differ at center pixel (32,32)");
    }

    @Test
    void getItemIcon_unknownKey_returnsNonNull64x64Image() {
        BufferedImage img = (BufferedImage) manager.getItemIcon("unknown_key");
        assertNotNull(img);
        assertEquals(64, img.getWidth());
        assertEquals(64, img.getHeight());
    }
}
