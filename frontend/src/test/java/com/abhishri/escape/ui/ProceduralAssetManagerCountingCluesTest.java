package com.abhishri.escape.ui;

import org.junit.jupiter.api.Test;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.*;

class ProceduralAssetManagerCountingCluesTest {

    private final ProceduralAssetManager manager = new ProceduralAssetManager();

    @Test
    void foyer_nail_207_140_isPaintedBrass() {
        BufferedImage img = (BufferedImage) manager.getBackground("room_foyer");
        assertEquals(ThemeConstants.AGED_BRASS.getRGB(), img.getRGB(207, 140));
    }

    @Test
    void foyer_nail_192_145_isPaintedBrass() {
        BufferedImage img = (BufferedImage) manager.getBackground("room_foyer");
        assertEquals(ThemeConstants.AGED_BRASS.getRGB(), img.getRGB(192, 145));
    }

    @Test
    void foyer_nail_222_148_isPaintedBrass() {
        BufferedImage img = (BufferedImage) manager.getBackground("room_foyer");
        assertEquals(ThemeConstants.AGED_BRASS.getRGB(), img.getRGB(222, 148));
    }

    @Test
    void readingHall_eighthShelf_areaIsPainted() {
        BufferedImage img = (BufferedImage) manager.getBackground("room_reading_hall");
        assertNotEquals(ThemeConstants.NIGHT_BLACK.getRGB(), img.getRGB(750, 200));
    }
}
